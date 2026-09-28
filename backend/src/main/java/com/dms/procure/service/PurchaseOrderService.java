package com.dms.procure.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.parts.entity.Part;
import com.dms.parts.mapper.PartMapper;
import com.dms.parts.service.PartStockService;
import com.dms.procure.entity.PurchaseInquiry;
import com.dms.procure.entity.PurchaseInquiryLine;
import com.dms.procure.entity.PurchaseOrder;
import com.dms.procure.entity.PurchaseOrderLine;
import com.dms.procure.entity.PurchaseReceipt;
import com.dms.procure.entity.PurchaseReceiptLine;
import com.dms.procure.mapper.PurchaseOrderLineMapper;
import com.dms.procure.mapper.PurchaseOrderMapper;
import com.dms.procure.mapper.PurchaseReceiptLineMapper;
import com.dms.procure.mapper.PurchaseReceiptMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 采购订单：经销商下单 → OEM 确认/退回 → 到货入库 → 关闭 → 对账。 */
@Service
public class PurchaseOrderService {
    private final PurchaseOrderMapper mapper;
    private final PurchaseOrderLineMapper lineMapper;
    private final PurchaseReceiptMapper receiptMapper;
    private final PurchaseReceiptLineMapper receiptLineMapper;
    private final PartMapper partMapper;
    private final PartStockService stockService;
    private final CodeGenerator codes;
    private final String defaultLocation;

    public PurchaseOrderService(
            PurchaseOrderMapper mapper,
            PurchaseOrderLineMapper lineMapper,
            PurchaseReceiptMapper receiptMapper,
            PurchaseReceiptLineMapper receiptLineMapper,
            PartMapper partMapper,
            PartStockService stockService,
            CodeGenerator codes,
            @Value("${dms.oms.receive-location:RCV-01}") String defaultLocation) {
        this.mapper = mapper;
        this.lineMapper = lineMapper;
        this.receiptMapper = receiptMapper;
        this.receiptLineMapper = receiptLineMapper;
        this.partMapper = partMapper;
        this.stockService = stockService;
        this.codes = codes;
        this.defaultLocation = defaultLocation;
    }

    private PurchaseOrder mustGet(Long id) {
        PurchaseOrder o = mapper.selectById(id);
        if (o == null) {
            throw new BizException("采购订单不存在");
        }
        return o;
    }

    public List<PurchaseOrderLine> lines(Long id) {
        PurchaseOrder o = mustGet(id);
        DataScope.check(o.getDealerCode());
        return lineMapper.selectList(
                new QueryWrapper<PurchaseOrderLine>().eq("order_id", id).orderByAsc("id"));
    }

    /** 到货单列表（每单含 lines 嵌套）。 */
    public List<Map<String, Object>> receipts(Long id) {
        PurchaseOrder o = mustGet(id);
        DataScope.check(o.getDealerCode());
        List<Map<String, Object>> result = new ArrayList<>();
        for (PurchaseReceipt r :
                receiptMapper.selectList(
                        new QueryWrapper<PurchaseReceipt>().eq("order_id", id).orderByAsc("id"))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("receiptNo", r.getReceiptNo());
            row.put("location", r.getLocation());
            row.put("batchNo", r.getBatchNo());
            row.put("createdAt", r.getCreatedAt());
            row.put(
                    "lines",
                    receiptLineMapper.selectList(
                            new QueryWrapper<PurchaseReceiptLine>()
                                    .eq("receipt_id", r.getId())
                                    .orderByAsc("id")));
            result.add(row);
        }
        return result;
    }

    private Part part(String partNo) {
        Part p = partMapper.selectOne(new QueryWrapper<Part>().eq("part_no", partNo));
        if (p == null) {
            throw new BizException("备件不存在: " + partNo);
        }
        return p;
    }

    /** 手工创建采购订单；单价默认备件成本价。 */
    @Transactional
    public PurchaseOrder create(Map<String, Object> body) {
        String dealerCode =
                DataScope.effectiveDealer(
                        body.get("dealerCode") == null ? null : String.valueOf(body.get("dealerCode")));
        if (dealerCode == null || dealerCode.trim().isEmpty()) {
            throw new BizException("请选择经销商");
        }
        PurchaseOrder o = new PurchaseOrder();
        o.setPoNo(codes.next("PO"));
        o.setDealerCode(dealerCode);
        o.setSource("MANUAL");
        o.setStatus("DRAFT");
        o.setReceivedAmount(BigDecimal.ZERO);
        if (body.get("expectDate") != null && !String.valueOf(body.get("expectDate")).isEmpty()) {
            o.setExpectDate(LocalDate.parse(String.valueOf(body.get("expectDate"))));
        }
        o.setRemark(body.get("remark") == null ? null : String.valueOf(body.get("remark")));
        mapper.insert(o);
        createLines(o, body.get("lines"));
        return o;
    }

    private void createLines(PurchaseOrder o, Object linesObj) {
        if (!(linesObj instanceof List) || ((List<?>) linesObj).isEmpty()) {
            throw new BizException("采购明细不能为空");
        }
        Map<String, PurchaseOrderLine> merged = new LinkedHashMap<>();
        for (Object it : (List<?>) linesObj) {
            Map<String, Object> m = (Map<String, Object>) it;
            String partNo = String.valueOf(m.get("partNo"));
            int qty = m.get("qty") == null ? 0 : ((Number) m.get("qty")).intValue();
            if (qty <= 0) {
                throw new BizException("备件 " + partNo + " 数量必须大于0");
            }
            PurchaseOrderLine line = merged.get(partNo);
            if (line != null) {
                line.setQty(line.getQty() + qty);
                line.setAmount(
                        line.getUnitPrice().multiply(new BigDecimal(line.getQty())));
                continue;
            }
            Part p = part(partNo);
            BigDecimal price =
                    m.get("unitPrice") != null
                            ? new BigDecimal(String.valueOf(m.get("unitPrice")))
                            : p.getCostPrice();
            line = new PurchaseOrderLine();
            line.setOrderId(o.getId());
            line.setPartNo(partNo);
            line.setName(p.getName());
            line.setQty(qty);
            line.setReceivedQty(0);
            line.setUnitPrice(price);
            line.setAmount(price.multiply(new BigDecimal(qty)));
            merged.put(partNo, line);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderLine line : merged.values()) {
            lineMapper.insert(line);
            total = total.add(line.getAmount());
        }
        o.setTotalAmount(total);
        mapper.updateById(o);
    }

    /** 询价单转采购订单：单价取报价。 */
    @Transactional
    public PurchaseOrder createFromInquiry(PurchaseInquiry q, List<PurchaseInquiryLine> qlines) {
        PurchaseOrder o = new PurchaseOrder();
        o.setPoNo(codes.next("PO"));
        o.setDealerCode(q.getDealerCode());
        o.setInquiryId(q.getId());
        o.setSource("INQUIRY");
        o.setStatus("DRAFT");
        o.setReceivedAmount(BigDecimal.ZERO);
        o.setExpectDate(q.getExpectDate());
        o.setRemark("询价单 " + q.getInquiryNo() + " 转入");
        mapper.insert(o);
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseInquiryLine ql : qlines) {
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setOrderId(o.getId());
            line.setPartNo(ql.getPartNo());
            line.setName(ql.getName());
            line.setQty(ql.getQty());
            line.setReceivedQty(0);
            line.setUnitPrice(ql.getQuotedPrice());
            line.setAmount(
                    ql.getQuotedPrice() == null
                            ? null
                            : ql.getQuotedPrice().multiply(new BigDecimal(ql.getQty())));
            lineMapper.insert(line);
            if (line.getAmount() != null) {
                total = total.add(line.getAmount());
            }
        }
        o.setTotalAmount(total);
        mapper.updateById(o);
        return o;
    }

    /** 缺货预警一键生成采购单（补到 minStock 的 2 倍），来源 SHORTAGE。 */
    @Transactional
    public PurchaseOrder createFromShortage(String dealerCodeParam) {
        String dealerCode = DataScope.effectiveDealer(dealerCodeParam);
        if (dealerCode == null || dealerCode.trim().isEmpty()) {
            throw new BizException("请选择经销商");
        }
        List<Map<String, Object>> lines = new ArrayList<>();
        for (Map<String, Object> row : stockService.shortage()) {
            if (!dealerCode.equals(row.get("dealerCode"))) {
                continue;
            }
            int min = ((Number) row.get("minStock")).intValue();
            int avail = ((Number) row.get("available")).intValue();
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("partNo", row.get("partNo"));
            it.put("qty", Math.max(1, min * 2 - avail));
            lines.add(it);
        }
        if (lines.isEmpty()) {
            throw new BizException("无缺货备件");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dealerCode", dealerCode);
        body.put("lines", lines);
        body.put("remark", "缺货预警自动生成");
        PurchaseOrder o = create(body);
        o.setSource("SHORTAGE");
        mapper.updateById(o);
        return o;
    }

    /** 提交给 OEM。 */
    @Transactional
    public PurchaseOrder submit(Long id) {
        PurchaseOrder o = mustGet(id);
        DataScope.check(o.getDealerCode());
        if (!"DRAFT".equals(o.getStatus())) {
            throw new BizException("仅草稿订单可提交");
        }
        if (lines(id).isEmpty()) {
            throw new BizException("采购订单无明细行，不能提交");
        }
        o.setStatus("SUBMITTED");
        o.setSubmittedAt(LocalDateTime.now());
        mapper.updateById(o);
        return o;
    }

    /** OEM 确认。 */
    @Transactional
    public PurchaseOrder confirm(Long id, Map<String, Object> body) {
        PurchaseOrder o = mustGet(id);
        if (!"SUBMITTED".equals(o.getStatus())) {
            throw new BizException("仅已提交的订单可确认");
        }
        if (body != null) {
            if (body.get("oemOrderNo") != null) {
                o.setOemOrderNo(String.valueOf(body.get("oemOrderNo")));
            }
            if (body.get("oemRemark") != null) {
                o.setOemRemark(String.valueOf(body.get("oemRemark")));
            }
            if (body.get("expectDate") != null && !String.valueOf(body.get("expectDate")).isEmpty()) {
                o.setExpectDate(LocalDate.parse(String.valueOf(body.get("expectDate"))));
            }
        }
        o.setStatus("CONFIRMED");
        o.setConfirmedAt(LocalDateTime.now());
        mapper.updateById(o);
        return o;
    }

    /** OEM 退回：回到草稿。 */
    @Transactional
    public PurchaseOrder reject(Long id, Map<String, Object> body) {
        PurchaseOrder o = mustGet(id);
        if (!"SUBMITTED".equals(o.getStatus())) {
            throw new BizException("仅已提交的订单可退回");
        }
        String remark = body == null || body.get("oemRemark") == null
                ? null
                : String.valueOf(body.get("oemRemark")).trim();
        if (remark == null || remark.isEmpty()) {
            throw new BizException("退回原因必填");
        }
        o.setOemRemark(remark);
        o.setStatus("DRAFT");
        mapper.updateById(o);
        return o;
    }

    @Transactional
    public PurchaseOrder cancel(Long id, Map<String, Object> body) {
        PurchaseOrder o = mustGet(id);
        DataScope.check(o.getDealerCode());
        if (!"DRAFT".equals(o.getStatus()) && !"SUBMITTED".equals(o.getStatus())) {
            throw new BizException("仅草稿或已提交的订单可取消");
        }
        o.setStatus("CANCELLED");
        if (body != null && body.get("reason") != null) {
            o.setRemark(String.valueOf(body.get("reason")));
        }
        mapper.updateById(o);
        return o;
    }

    /**
     * 到货入库：CONFIRMED/PARTIAL_RECEIVED 可收货；超量拒绝；
     * 生成到货单 + 明细，逐行入库并累计 received_qty/received_amount。
     */
    @Transactional
    public PurchaseOrder receive(Long id, Map<String, Object> body) {
        PurchaseOrder o = mustGet(id);
        DataScope.check(o.getDealerCode());
        if (!"CONFIRMED".equals(o.getStatus()) && !"PARTIAL_RECEIVED".equals(o.getStatus())) {
            throw new BizException("仅厂家已确认的订单可收货");
        }
        if (body == null || !(body.get("lines") instanceof List) || ((List<?>) body.get("lines")).isEmpty()) {
            throw new BizException("到货明细不能为空");
        }
        String location =
                body.get("location") == null || String.valueOf(body.get("location")).trim().isEmpty()
                        ? defaultLocation
                        : String.valueOf(body.get("location")).trim();
        String batchNo = body.get("batchNo") == null ? null : String.valueOf(body.get("batchNo"));

        Map<Long, PurchaseOrderLine> byId = new LinkedHashMap<>();
        for (PurchaseOrderLine l : lines(id)) {
            byId.put(l.getId(), l);
        }
        Map<PurchaseOrderLine, Integer> toReceive = new LinkedHashMap<>();
        for (Object it : (List<?>) body.get("lines")) {
            Map<String, Object> m = (Map<String, Object>) it;
            Long lineId = m.get("orderLineId") == null ? null : Long.valueOf(String.valueOf(m.get("orderLineId")));
            PurchaseOrderLine line = lineId == null ? null : byId.get(lineId);
            if (line == null) {
                throw new BizException("到货行不属于该订单");
            }
            int qty = m.get("qty") == null ? 0 : ((Number) m.get("qty")).intValue();
            if (qty <= 0) {
                throw new BizException("到货数量必须大于0");
            }
            int already = line.getReceivedQty() == null ? 0 : line.getReceivedQty();
            int pending = toReceive.getOrDefault(line, 0);
            if (already + pending + qty > line.getQty()) {
                throw new BizException("到货数量超出订单数量: " + line.getPartNo());
            }
            toReceive.put(line, pending + qty);
        }

        PurchaseReceipt r = new PurchaseReceipt();
        r.setReceiptNo(codes.next("PR"));
        r.setOrderId(o.getId());
        r.setDealerCode(o.getDealerCode());
        r.setLocation(location);
        r.setBatchNo(batchNo);
        r.setRemark(body.get("remark") == null ? null : String.valueOf(body.get("remark")));
        receiptMapper.insert(r);

        BigDecimal receivedAdd = BigDecimal.ZERO;
        for (Map.Entry<PurchaseOrderLine, Integer> e : toReceive.entrySet()) {
            PurchaseOrderLine line = e.getKey();
            int qty = e.getValue();
            PurchaseReceiptLine rl = new PurchaseReceiptLine();
            rl.setReceiptId(r.getId());
            rl.setOrderLineId(line.getId());
            rl.setPartNo(line.getPartNo());
            rl.setQty(qty);
            rl.setUnitPrice(line.getUnitPrice());
            rl.setAmount(line.getUnitPrice().multiply(new BigDecimal(qty)));
            receiptLineMapper.insert(rl);
            stockService.inbound(o.getDealerCode(), line.getPartNo(), location, batchNo, qty);
            line.setReceivedQty((line.getReceivedQty() == null ? 0 : line.getReceivedQty()) + qty);
            lineMapper.updateById(line);
            receivedAdd = receivedAdd.add(rl.getAmount());
        }

        o.setReceivedAmount(
                (o.getReceivedAmount() == null ? BigDecimal.ZERO : o.getReceivedAmount())
                        .add(receivedAdd));
        boolean allDone = true;
        for (PurchaseOrderLine l : lines(id)) {
            if ((l.getReceivedQty() == null ? 0 : l.getReceivedQty()) < l.getQty()) {
                allDone = false;
                break;
            }
        }
        o.setStatus(allDone ? "RECEIVED" : "PARTIAL_RECEIVED");
        mapper.updateById(o);
        return o;
    }

    @Transactional
    public PurchaseOrder close(Long id) {
        PurchaseOrder o = mustGet(id);
        DataScope.check(o.getDealerCode());
        if (!"RECEIVED".equals(o.getStatus())) {
            throw new BizException("仅已全部到货的订单可关闭");
        }
        o.setStatus("CLOSED");
        o.setClosedAt(LocalDateTime.now());
        mapper.updateById(o);
        return o;
    }

    /** 仅 DRAFT/CANCELLED 可删除，连同明细。 */
    @Transactional
    public void delete(Long id) {
        PurchaseOrder o = mustGet(id);
        DataScope.check(o.getDealerCode());
        if (!"DRAFT".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus())) {
            throw new BizException("仅草稿或已取消的订单可删除");
        }
        lineMapper.delete(new QueryWrapper<PurchaseOrderLine>().eq("order_id", id));
        mapper.deleteById(id);
    }
}
