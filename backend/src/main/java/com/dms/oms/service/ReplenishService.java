package com.dms.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.network.entity.Dealer;
import com.dms.network.mapper.DealerMapper;
import com.dms.oms.client.OmsClient;
import com.dms.oms.client.OmsException;
import com.dms.oms.entity.ReplenishOrder;
import com.dms.oms.mapper.ReplenishOrderMapper;
import com.dms.parts.entity.Part;
import com.dms.parts.mapper.PartMapper;
import com.dms.parts.service.PartStockService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.time.Duration;

/**
 * 备件补货:DMS 作为 OMS 的渠道(shopCode=dms.oms.shop-code,channelOrderNo=replenishNo)。
 * 下单幂等由 OMS 按 shopCode+channelOrderNo 保证;状态同步既支持 OMS 主动回推,也支持 DMS 轮询拉取;
 * 签收(COMPLETED)时按 OMS 实发数量入库到经销商备件库,入库只允许发生一次(SHIPPED->RECEIVED 条件更新)。
 */
@Slf4j
@Service
public class ReplenishService {
    public static final String DRAFT = "DRAFT";
    public static final String PUSHING = "PUSHING";
    public static final String PUSHED = "PUSHED";
    public static final String SHIPPED = "SHIPPED";
    public static final String RECEIVED = "RECEIVED";
    public static final String CANCELLED = "CANCELLED";
    private static final List<String> OPEN = Arrays.asList(PUSHED, SHIPPED);
    /** 超过此时长仍在 PUSHING 视为下单中断(启动/syncAll 时恢复);应大于 OMS 调用超时 dms.oms.timeout-ms */
    public static final Duration PUSHING_TIMEOUT = Duration.ofMinutes(2);
    /** OMS 状态先后次序,旧快照不能覆盖新快照的元数据 */
    private static final List<String> OMS_ORDER = Arrays.asList(
            "CREATED", "AUDITED", "ALLOCATED", "PUSHED", "SPLIT", "SHIPPED", "COMPLETED", "CANCELLED");

    private final ReplenishOrderMapper mapper;
    private final DealerMapper dealerMapper;
    private final PartMapper partMapper;
    private final PartStockService stockService;
    private final OmsClient oms;
    private final CodeGenerator codes;
    private final ObjectMapper om;
    private final TransactionTemplate tx;
    private final String shopCode;
    private final String defaultLocation;

    public ReplenishService(
            ReplenishOrderMapper mapper,
            DealerMapper dealerMapper,
            PartMapper partMapper,
            PartStockService stockService,
            OmsClient oms,
            CodeGenerator codes,
            ObjectMapper om,
            TransactionTemplate tx,
            @Value("${dms.oms.shop-code:SHOP-DMS01}") String shopCode,
            @Value("${dms.oms.receive-location:RCV-01}") String defaultLocation) {
        this.mapper = mapper;
        this.dealerMapper = dealerMapper;
        this.partMapper = partMapper;
        this.stockService = stockService;
        this.oms = oms;
        this.codes = codes;
        this.om = om;
        this.tx = tx;
        this.shopCode = shopCode;
        this.defaultLocation = defaultLocation;
    }

    public String shopCode() {
        return shopCode;
    }

    // ------------------------------------------------------------ 创建

    /** 手工创建补货单;items: [{partNo, qty}] */
    @Transactional
    public ReplenishOrder create(String dealerCode, List<Map<String, Object>> items, String source, String remark) {
        Dealer dealer = dealer(dealerCode);
        if (items == null || items.isEmpty()) {
            throw new BizException("补货明细不能为空");
        }
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Map<String, Object> it : items) {
            String partNo = String.valueOf(it.get("partNo"));
            int qty = it.get("qty") == null ? 0 : ((Number) it.get("qty")).intValue();
            if (qty <= 0) {
                throw new BizException("备件 " + partNo + " 补货数量必须大于0");
            }
            Map<String, Object> line = merged.get(partNo);
            if (line != null) {
                try {
                    line.put("qty", Math.addExact(((Number) line.get("qty")).intValue(), qty));
                } catch (ArithmeticException e) {
                    throw new BizException("备件 " + partNo + " 合并后补货数量超出范围");
                }
                continue;
            }
            Part p = partMapper.selectOne(new QueryWrapper<Part>().eq("part_no", partNo));
            if (p == null) {
                throw new BizException("备件不存在: " + partNo);
            }
            line = new LinkedHashMap<>();
            line.put("partNo", partNo);
            line.put("name", p.getName());
            line.put("qty", qty);
            line.put("price", p.getCostPrice());
            merged.put(partNo, line);
        }
        List<Map<String, Object>> lines = new ArrayList<>(merged.values());
        ReplenishOrder o = new ReplenishOrder();
        o.setReplenishNo(codes.next("RPL"));
        o.setDealerCode(dealer.getCode());
        o.setShopCode(shopCode);
        o.setStatus(DRAFT);
        o.setSource(source == null ? "MANUAL" : source);
        o.setItems(json(lines));
        o.setLocation(defaultLocation);
        o.setRemark(remark);
        mapper.insert(o);
        return o;
    }

    /** 按缺货预警为指定经销商生成补货单(补到 minStock 的 2 倍),无缺口返回 null */
    @Transactional
    public ReplenishOrder createFromShortage(String dealerCode) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : stockService.shortage()) {
            if (!dealerCode.equals(row.get("dealerCode"))) {
                continue;
            }
            int min = ((Number) row.get("minStock")).intValue();
            int avail = ((Number) row.get("available")).intValue();
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("partNo", row.get("partNo"));
            it.put("qty", Math.max(1, min * 2 - avail));
            items.add(it);
        }
        if (items.isEmpty()) {
            return null;
        }
        return create(dealerCode, items, "SHORTAGE", "缺货预警自动生成");
    }

    // ------------------------------------------------------------ 下单 OMS

    /**
     * 下单 OMS。不在事务内调用远端:先 DRAFT->PUSHING 抢占(建单中,不可取消/同步),
     * 建单成功 PUSHING->PUSHED,失败回退 DRAFT 并记录 lastError(独立提交,不随异常回滚)。
     */
    public ReplenishOrder push(Long id) {
        ReplenishOrder o = get(id);
        if (!DRAFT.equals(o.getStatus()) || mapper.transit(id, DRAFT, PUSHING) != 1) {
            throw new BizException("仅草稿状态可下单 OMS(当前 " + o.getStatus() + ")");
        }
        Map<String, Object> req;
        try {
            req = buildOrderRequest(o);
        } catch (RuntimeException e) {
            markError(id, e.getMessage());
            mapper.transit(id, PUSHING, DRAFT);
            throw e;
        }
        Map<String, Object> created;
        try {
            created = oms.createOrder(req);
        } catch (OmsException e) {
            markError(id, e.getMessage());
            mapper.transit(id, PUSHING, DRAFT);
            throw new BizException("下单 OMS 失败: " + e.getMessage());
        }
        return confirmPushed(o, created);
    }

    private Map<String, Object> buildOrderRequest(ReplenishOrder o) {
        Dealer dealer = dealer(o.getDealerCode());
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("shopCode", o.getShopCode());
        req.put("channelOrderNo", o.getReplenishNo());
        req.put("customerCode", dealer.getCode());
        req.put("receiverName", dealer.getName());
        req.put("receiverPhone", dealer.getPhone());
        req.put("province", dealer.getProvince());
        req.put("city", dealer.getCity());
        req.put("address", dealer.getAddress());
        req.put("payStatus", "PAID");
        req.put("buyerRemark", "DMS 备件补货 " + o.getReplenishNo() + " " + dealer.getName());
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> l : lines(o)) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("sku", l.get("partNo"));
            it.put("qty", l.get("qty"));
            it.put("price", l.get("price"));
            items.add(it);
        }
        req.put("items", items);
        return req;
    }

    /**
     * OMS 已建单:PUSHING->PUSHED 并应用 OMS 快照。
     * 若其他实例的恢复任务已将本单回退 DRAFT(OMS 建单前没查到),则改为 DRAFT->PUSHED,保证本地与远端一致;
     * 仍无法迁移则以当前状态报错,不静默返回。
     */
    private ReplenishOrder confirmPushed(ReplenishOrder o, Map<String, Object> remote) {
        if (mapper.transit(o.getId(), PUSHING, PUSHED) != 1
                && mapper.transit(o.getId(), DRAFT, PUSHED) != 1) {
            ReplenishOrder cur = mapper.selectById(o.getId());
            if (cur == null || !OPEN.contains(cur.getStatus())) {
                String st = cur == null ? "不存在" : cur.getStatus();
                markError(o.getId(), "OMS 已建单 " + str(remote.get("orderNo")) + " 但本地状态为 " + st + ",请人工核对");
                throw new BizException("OMS 已建单,但补货单本地状态为 " + st + ",无法确认为已下单");
            }
        }
        mapper.update(null, new LambdaUpdateWrapper<ReplenishOrder>()
                .eq(ReplenishOrder::getId, o.getId())
                .isNull(ReplenishOrder::getPushedAt)
                .set(ReplenishOrder::getPushedAt, LocalDateTime.now()));
        o.setStatus(PUSHED);
        tx.executeWithoutResult(s -> applyOmsState(o, remote));
        return mapper.selectById(o.getId());
    }

    /**
     * 恢复因进程中断停在 PUSHING 的补货单:按 shopCode+replenishNo 查 OMS,已建单则转 PUSHED,未建单则回退 DRAFT。
     * 只处理超过 PUSHING_TIMEOUT 的单据:启动恢复也遵守超时,因为滚动发布时其他实例可能仍在下单。
     */
    public int recoverPushing() {
        QueryWrapper<ReplenishOrder> q = new QueryWrapper<ReplenishOrder>()
                .eq("status", PUSHING)
                .lt("updated_at", LocalDateTime.now().minus(PUSHING_TIMEOUT));
        int n = 0;
        for (ReplenishOrder o : mapper.selectList(q)) {
            try {
                Map<String, Object> remote = oms.getOrder(o.getShopCode(), o.getReplenishNo());
                if (remote == null) {
                    markError(o.getId(), "下单中断,OMS 无此单,已回退草稿");
                    mapper.transit(o.getId(), PUSHING, DRAFT);
                } else {
                    confirmPushed(o, remote);
                }
                n++;
            } catch (OmsException e) {
                log.warn("补货单 {} PUSHING 恢复失败: {}", o.getReplenishNo(), e.getMessage());
            }
        }
        return n;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverPushingOnStartup() {
        int n = recoverPushing();
        if (n > 0) {
            log.info("启动恢复 PUSHING 补货单 {} 张", n);
        }
    }

    @Transactional
    public ReplenishOrder cancel(Long id, String reason) {
        ReplenishOrder o = get(id);
        if (DRAFT.equals(o.getStatus())) {
            o.setStatus(CANCELLED);
            o.setLastError(null);
            mapper.updateById(o);
            return o;
        }
        if (PUSHING.equals(o.getStatus())) {
            throw new BizException("正在下单 OMS,请稍后重试取消");
        }
        if (!PUSHED.equals(o.getStatus())) {
            throw new BizException("已发货/已入库的补货单不能取消(当前 " + o.getStatus() + ")");
        }
        try {
            Map<String, Object> r = oms.cancelOrder(o.getShopCode(), o.getReplenishNo(),
                    reason == null ? "DMS 取消补货" : reason);
            o.setOmsStatus(str(r.get("status")));
        } catch (OmsException e) {
            throw new BizException("OMS 取消失败: " + e.getMessage());
        }
        o.setStatus(CANCELLED);
        o.setSyncedAt(LocalDateTime.now());
        mapper.updateById(o);
        return o;
    }

    // ------------------------------------------------------------ 状态同步

    /** 主动向 OMS 拉取单据状态(远端调用在事务外,失败原因独立落库) */
    public ReplenishOrder sync(Long id) {
        ReplenishOrder o = get(id);
        if (!OPEN.contains(o.getStatus())) {
            return o;
        }
        Map<String, Object> remote;
        try {
            remote = oms.getOrder(o.getShopCode(), o.getReplenishNo());
        } catch (OmsException e) {
            markError(id, e.getMessage());
            throw new BizException("查询 OMS 失败: " + e.getMessage());
        }
        if (remote == null) {
            markError(id, "OMS 无此渠道单号 " + o.getReplenishNo());
            return mapper.selectById(id);
        }
        tx.executeWithoutResult(s -> applyOmsState(o, remote));
        return mapper.selectById(id);
    }

    private void markError(Long id, String msg) {
        mapper.update(null, new LambdaUpdateWrapper<ReplenishOrder>()
                .eq(ReplenishOrder::getId, id)
                .set(ReplenishOrder::getLastError, trim(msg)));
    }

    /** 同步所有在途补货单,返回同步条数 */
    public int syncAll() {
        int n = recoverPushing();
        for (ReplenishOrder o : mapper.selectList(new QueryWrapper<ReplenishOrder>().in("status", OPEN))) {
            try {
                sync(o.getId());
                n++;
            } catch (BizException e) {
                log.warn("补货单 {} 同步失败: {}", o.getReplenishNo(), e.getMessage());
            }
        }
        return n;
    }

    /** OMS 回推(SHIPPED/SIGNED/CANCELLED 事件),按 channelOrderNo 定位;未知单号返回 null */
    @Transactional
    public ReplenishOrder onOmsEvent(Map<String, Object> payload) {
        String channelOrderNo = str(payload.get("channelOrderNo"));
        if (channelOrderNo == null) {
            throw new BizException("channelOrderNo 必填");
        }
        ReplenishOrder o = mapper.selectOne(
                new QueryWrapper<ReplenishOrder>().eq("replenish_no", channelOrderNo));
        if (o == null) {
            return null;
        }
        if (!OPEN.contains(o.getStatus())) {
            return o;
        }
        applyOmsState(o, payload);
        return mapper.selectById(o.getId());
    }

    /**
     * 把 OMS 订单快照应用到补货单。OMS 状态:CREATED/AUDITED/ALLOCATED/PUSHED/SPLIT -> 在途;
     * SHIPPED -> SHIPPED;COMPLETED -> RECEIVED(入库);CANCELLED -> CANCELLED。
     * 元数据更新不携带 status,状态只经 transit 条件迁移,并发回推/轮询不会把已 RECEIVED 的单覆盖回 SHIPPED;
     * 元数据仅在本地单仍在途且快照不早于已记录的 OMS 状态时写入,旧快照不会覆盖新物流信息。
     */
    private void applyOmsState(ReplenishOrder o, Map<String, Object> remote) {
        String omsStatus = str(remote.get("status"));
        int rank = OMS_ORDER.indexOf(omsStatus);
        List<String> notNewer = rank < 0 ? OMS_ORDER : OMS_ORDER.subList(0, rank + 1);
        LambdaUpdateWrapper<ReplenishOrder> meta = new LambdaUpdateWrapper<ReplenishOrder>()
                .eq(ReplenishOrder::getId, o.getId())
                .in(ReplenishOrder::getStatus, OPEN)
                .and(w -> w.isNull(ReplenishOrder::getOmsStatus).or().in(ReplenishOrder::getOmsStatus, notNewer))
                .set(ReplenishOrder::getOmsStatus, omsStatus)
                .set(ReplenishOrder::getSyncedAt, LocalDateTime.now())
                .set(ReplenishOrder::getLastError, null);
        if (remote.get("orderNo") != null) {
            o.setOmsOrderNo(str(remote.get("orderNo")));
            meta.set(ReplenishOrder::getOmsOrderNo, o.getOmsOrderNo());
        }
        if (remote.get("warehouseCode") != null) {
            meta.set(ReplenishOrder::getWarehouseCode, str(remote.get("warehouseCode")));
        }
        if (remote.get("carrierCode") != null) {
            meta.set(ReplenishOrder::getCarrierCode, str(remote.get("carrierCode")));
        }
        if (remote.get("trackingNo") != null) {
            meta.set(ReplenishOrder::getTrackingNo, str(remote.get("trackingNo")));
        }
        mapper.update(null, meta);

        if ("SHIPPED".equals(omsStatus) && PUSHED.equals(o.getStatus())) {
            if (mapper.transit(o.getId(), PUSHED, SHIPPED) == 1) {
                mapper.update(null, new LambdaUpdateWrapper<ReplenishOrder>()
                        .eq(ReplenishOrder::getId, o.getId())
                        .set(ReplenishOrder::getShippedAt, LocalDateTime.now()));
                o.setStatus(SHIPPED);
            }
        } else if ("COMPLETED".equals(omsStatus)) {
            // 不依赖入口处读到的 o.status:并发线程可能已完成 PUSHED->SHIPPED,这里继续以数据库当前状态认领入库
            mapper.transit(o.getId(), PUSHED, SHIPPED);
            if (mapper.transit(o.getId(), SHIPPED, RECEIVED) == 1) {
                o.setStatus(RECEIVED);
                receive(o, remote);
            }
        } else if ("CANCELLED".equals(omsStatus) && PUSHED.equals(o.getStatus())) {
            mapper.transit(o.getId(), PUSHED, CANCELLED);
        }
    }

    /** 按 OMS 实发数量(缺省为订购数量)入库,批次号=OMS 单号,保证同一补货单只入库一次 */
    @SuppressWarnings("unchecked")
    private void receive(ReplenishOrder o, Map<String, Object> remote) {
        Map<String, Integer> shipped = new LinkedHashMap<>();
        Object items = remote.get("items");
        if (items instanceof List) {
            for (Object x : (List<Object>) items) {
                if (x instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) x;
                    Object q = m.get("shippedQty") != null ? m.get("shippedQty") : m.get("qty");
                    if (m.get("sku") != null && q != null) {
                        shipped.merge(str(m.get("sku")), ((Number) q).intValue(), Integer::sum);
                    }
                }
            }
        }
        String omsNo = remote.get("orderNo") != null ? str(remote.get("orderNo")) : o.getOmsOrderNo();
        String batch = omsNo == null ? o.getReplenishNo() : omsNo;
        String location = o.getLocation() == null ? defaultLocation : o.getLocation();
        for (Map<String, Object> l : lines(o)) {
            String partNo = str(l.get("partNo"));
            int qty = shipped.containsKey(partNo) ? shipped.get(partNo) : ((Number) l.get("qty")).intValue();
            if (qty > 0) {
                stockService.inbound(o.getDealerCode(), partNo, location, batch, qty);
            }
        }
        mapper.update(null, new LambdaUpdateWrapper<ReplenishOrder>()
                .eq(ReplenishOrder::getId, o.getId())
                .set(ReplenishOrder::getReceivedAt, LocalDateTime.now()));
    }

    // ------------------------------------------------------------ 查询

    public List<Map<String, Object>> omsInventory(List<String> partNos) {
        if (partNos == null || partNos.isEmpty()) {
            throw new BizException("partNos 必填");
        }
        try {
            return oms.inventory(shopCode, partNos);
        } catch (OmsException e) {
            throw new BizException("查询 OMS 库存失败: " + e.getMessage());
        }
    }

    public ReplenishOrder get(Long id) {
        ReplenishOrder o = mapper.selectById(id);
        if (o == null) {
            throw new BizException("补货单不存在: " + id);
        }
        return o;
    }

    public List<Map<String, Object>> lines(ReplenishOrder o) {
        try {
            return o.getItems() == null
                    ? new ArrayList<>()
                    : om.readValue(o.getItems(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new BizException("补货明细解析失败: " + e.getMessage());
        }
    }

    private Dealer dealer(String code) {
        Dealer d = dealerMapper.selectOne(new QueryWrapper<Dealer>().eq("code", code));
        if (d == null) {
            throw new BizException("经销商不存在: " + code);
        }
        return d;
    }

    private String json(Object v) {
        try {
            return om.writeValueAsString(v);
        } catch (Exception e) {
            throw new BizException("序列化失败: " + e.getMessage());
        }
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String trim(String s) {
        return s == null || s.length() <= 500 ? s : s.substring(0, 500);
    }
}
