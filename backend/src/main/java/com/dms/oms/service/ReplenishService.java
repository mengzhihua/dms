package com.dms.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

/**
 * 备件补货:DMS 作为 OMS 的渠道(shopCode=dms.oms.shop-code,channelOrderNo=replenishNo)。
 * 下单幂等由 OMS 按 shopCode+channelOrderNo 保证;状态同步既支持 OMS 主动回推,也支持 DMS 轮询拉取;
 * 签收(COMPLETED)时按 OMS 实发数量入库到经销商备件库,入库只允许发生一次(SHIPPED->RECEIVED 条件更新)。
 */
@Slf4j
@Service
public class ReplenishService {
    public static final String DRAFT = "DRAFT";
    public static final String PUSHED = "PUSHED";
    public static final String SHIPPED = "SHIPPED";
    public static final String RECEIVED = "RECEIVED";
    public static final String CANCELLED = "CANCELLED";
    private static final List<String> OPEN = Arrays.asList(PUSHED, SHIPPED);

    private final ReplenishOrderMapper mapper;
    private final DealerMapper dealerMapper;
    private final PartMapper partMapper;
    private final PartStockService stockService;
    private final OmsClient oms;
    private final CodeGenerator codes;
    private final ObjectMapper om;
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
            @Value("${dms.oms.shop-code:SHOP-DMS01}") String shopCode,
            @Value("${dms.oms.receive-location:RCV-01}") String defaultLocation) {
        this.mapper = mapper;
        this.dealerMapper = dealerMapper;
        this.partMapper = partMapper;
        this.stockService = stockService;
        this.oms = oms;
        this.codes = codes;
        this.om = om;
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
        List<Map<String, Object>> lines = new ArrayList<>();
        for (Map<String, Object> it : items) {
            String partNo = String.valueOf(it.get("partNo"));
            int qty = it.get("qty") == null ? 0 : ((Number) it.get("qty")).intValue();
            if (qty <= 0) {
                throw new BizException("备件 " + partNo + " 补货数量必须大于0");
            }
            Part p = partMapper.selectOne(new QueryWrapper<Part>().eq("part_no", partNo));
            if (p == null) {
                throw new BizException("备件不存在: " + partNo);
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("partNo", partNo);
            line.put("name", p.getName());
            line.put("qty", qty);
            line.put("price", p.getCostPrice());
            lines.add(line);
        }
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

    @Transactional
    public ReplenishOrder push(Long id) {
        ReplenishOrder o = get(id);
        if (!DRAFT.equals(o.getStatus())) {
            throw new BizException("仅草稿状态可下单 OMS(当前 " + o.getStatus() + ")");
        }
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
        try {
            Map<String, Object> created = oms.createOrder(req);
            o.setOmsOrderNo(str(created.get("orderNo")));
            o.setOmsStatus(str(created.get("status")));
            o.setWarehouseCode(str(created.get("warehouseCode")));
            o.setLastError(null);
            o.setPushedAt(LocalDateTime.now());
            o.setSyncedAt(LocalDateTime.now());
            o.setStatus(PUSHED);
            mapper.updateById(o);
            applyOmsState(o, created);
        } catch (OmsException e) {
            o.setLastError(trim(e.getMessage()));
            mapper.updateById(o);
            throw new BizException("下单 OMS 失败: " + e.getMessage());
        }
        return mapper.selectById(id);
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

    /** 主动向 OMS 拉取单据状态 */
    @Transactional
    public ReplenishOrder sync(Long id) {
        ReplenishOrder o = get(id);
        if (!OPEN.contains(o.getStatus())) {
            return o;
        }
        Map<String, Object> remote;
        try {
            remote = oms.getOrder(o.getShopCode(), o.getReplenishNo());
        } catch (OmsException e) {
            o.setLastError(trim(e.getMessage()));
            mapper.updateById(o);
            throw new BizException("查询 OMS 失败: " + e.getMessage());
        }
        if (remote == null) {
            o.setLastError("OMS 无此渠道单号 " + o.getReplenishNo());
            mapper.updateById(o);
            return o;
        }
        applyOmsState(o, remote);
        return mapper.selectById(id);
    }

    /** 同步所有在途补货单,返回同步条数 */
    @Transactional
    public int syncAll() {
        int n = 0;
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
     */
    private void applyOmsState(ReplenishOrder o, Map<String, Object> remote) {
        String omsStatus = str(remote.get("status"));
        if (remote.get("orderNo") != null) {
            o.setOmsOrderNo(str(remote.get("orderNo")));
        }
        if (remote.get("warehouseCode") != null) {
            o.setWarehouseCode(str(remote.get("warehouseCode")));
        }
        if (remote.get("carrierCode") != null) {
            o.setCarrierCode(str(remote.get("carrierCode")));
        }
        if (remote.get("trackingNo") != null) {
            o.setTrackingNo(str(remote.get("trackingNo")));
        }
        o.setOmsStatus(omsStatus);
        o.setSyncedAt(LocalDateTime.now());
        o.setLastError(null);
        mapper.updateById(o);

        if ("SHIPPED".equals(omsStatus) && PUSHED.equals(o.getStatus())) {
            if (mapper.transit(o.getId(), PUSHED, SHIPPED) == 1) {
                ReplenishOrder u = new ReplenishOrder();
                u.setId(o.getId());
                u.setShippedAt(LocalDateTime.now());
                mapper.updateById(u);
                o.setStatus(SHIPPED);
            }
        } else if ("COMPLETED".equals(omsStatus)) {
            if (PUSHED.equals(o.getStatus()) && mapper.transit(o.getId(), PUSHED, SHIPPED) == 1) {
                o.setStatus(SHIPPED);
            }
            if (SHIPPED.equals(o.getStatus()) && mapper.transit(o.getId(), SHIPPED, RECEIVED) == 1) {
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
        String batch = o.getOmsOrderNo() == null ? o.getReplenishNo() : o.getOmsOrderNo();
        String location = o.getLocation() == null ? defaultLocation : o.getLocation();
        for (Map<String, Object> l : lines(o)) {
            String partNo = str(l.get("partNo"));
            int qty = shipped.containsKey(partNo) ? shipped.get(partNo) : ((Number) l.get("qty")).intValue();
            if (qty > 0) {
                stockService.inbound(o.getDealerCode(), partNo, location, batch, qty);
            }
        }
        ReplenishOrder u = new ReplenishOrder();
        u.setId(o.getId());
        u.setReceivedAt(LocalDateTime.now());
        mapper.updateById(u);
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
