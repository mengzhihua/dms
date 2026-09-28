package com.dms.procure.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.parts.entity.Part;
import com.dms.parts.mapper.PartMapper;
import com.dms.procure.entity.PurchaseInquiry;
import com.dms.procure.entity.PurchaseInquiryLine;
import com.dms.procure.mapper.PurchaseInquiryLineMapper;
import com.dms.procure.mapper.PurchaseInquiryMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 采购询价：经销商询价(DRAFT→SENT) → OEM 报价(QUOTED) → 转采购订单(ORDERED)。 */
@Service
@RequiredArgsConstructor
public class PurchaseInquiryService {
    private final PurchaseInquiryMapper mapper;
    private final PurchaseInquiryLineMapper lineMapper;
    private final PartMapper partMapper;
    private final CodeGenerator codes;
    private final PurchaseOrderService orderService;

    private PurchaseInquiry mustGet(Long id) {
        PurchaseInquiry q = mapper.selectById(id);
        if (q == null) {
            throw new BizException("询价单不存在");
        }
        return q;
    }

    public List<PurchaseInquiryLine> lines(Long id) {
        PurchaseInquiry q = mustGet(id);
        DataScope.check(q.getDealerCode());
        return lineMapper.selectList(
                new QueryWrapper<PurchaseInquiryLine>().eq("inquiry_id", id).orderByAsc("id"));
    }

    /** 经销商创建询价单：校验备件存在、qty>0、同件号合并；名称取备件主数据。 */
    @Transactional
    public PurchaseInquiry create(Map<String, Object> body) {
        String dealerCode =
                DataScope.effectiveDealer(
                        body.get("dealerCode") == null ? null : String.valueOf(body.get("dealerCode")));
        if (dealerCode == null || dealerCode.trim().isEmpty()) {
            throw new BizException("请选择经销商");
        }
        PurchaseInquiry q = new PurchaseInquiry();
        q.setInquiryNo(codes.next("PI"));
        q.setDealerCode(dealerCode);
        q.setTitle(str(body.get("title")));
        if (body.get("expectDate") != null && !String.valueOf(body.get("expectDate")).isEmpty()) {
            q.setExpectDate(LocalDate.parse(String.valueOf(body.get("expectDate"))));
        }
        q.setRemark(str(body.get("remark")));
        q.setStatus("DRAFT");
        mapper.insert(q);
        insertLines(q.getId(), body.get("lines"), true);
        return q;
    }

    /** 校验并落明细行；merge=true 时同件号数量合并。 */
    private void insertLines(Long inquiryId, Object linesObj, boolean merge) {
        if (!(linesObj instanceof List) || ((List<?>) linesObj).isEmpty()) {
            throw new BizException("询价明细不能为空");
        }
        Map<String, PurchaseInquiryLine> merged = new LinkedHashMap<>();
        for (Object o : (List<?>) linesObj) {
            Map<String, Object> it = (Map<String, Object>) o;
            String partNo = String.valueOf(it.get("partNo"));
            int qty = it.get("qty") == null ? 0 : ((Number) it.get("qty")).intValue();
            if (qty <= 0) {
                throw new BizException("备件 " + partNo + " 数量必须大于0");
            }
            PurchaseInquiryLine line = merged.get(partNo);
            if (line != null && merge) {
                line.setQty(line.getQty() + qty);
                continue;
            }
            Part p =
                    partMapper.selectOne(new QueryWrapper<Part>().eq("part_no", partNo));
            if (p == null) {
                throw new BizException("备件不存在: " + partNo);
            }
            line = new PurchaseInquiryLine();
            line.setInquiryId(inquiryId);
            line.setPartNo(partNo);
            line.setName(p.getName());
            line.setQty(qty);
            if (it.get("targetPrice") != null) {
                line.setTargetPrice(new BigDecimal(String.valueOf(it.get("targetPrice"))));
            }
            merged.put(partNo, line);
        }
        for (PurchaseInquiryLine line : merged.values()) {
            lineMapper.insert(line);
        }
    }

    /** 仅 DRAFT 可整体替换明细行。 */
    @Transactional
    public PurchaseInquiry replaceLines(Long id, Map<String, Object> body) {
        PurchaseInquiry q = mustGet(id);
        DataScope.check(q.getDealerCode());
        if (!"DRAFT".equals(q.getStatus())) {
            throw new BizException("仅草稿询价单可修改明细");
        }
        lineMapper.delete(new QueryWrapper<PurchaseInquiryLine>().eq("inquiry_id", id));
        insertLines(id, body.get("lines"), true);
        return q;
    }

    @Transactional
    public PurchaseInquiry send(Long id) {
        PurchaseInquiry q = mustGet(id);
        DataScope.check(q.getDealerCode());
        if (!"DRAFT".equals(q.getStatus())) {
            throw new BizException("仅草稿询价单可发出");
        }
        if (lines(id).isEmpty()) {
            throw new BizException("询价单无明细行，不能发出");
        }
        q.setStatus("SENT");
        mapper.updateById(q);
        return q;
    }

    /** OEM 报价：每行必须给出 quotedPrice>0，可附 leadDays。 */
    @Transactional
    public PurchaseInquiry quote(Long id, Map<String, Object> body) {
        PurchaseInquiry q = mustGet(id);
        if (!"SENT".equals(q.getStatus())) {
            throw new BizException("仅已发出的询价单可报价");
        }
        Map<Long, Map<String, Object>> byLine = new LinkedHashMap<>();
        if (body != null && body.get("lines") instanceof List) {
            for (Object o : (List<?>) body.get("lines")) {
                Map<String, Object> it = (Map<String, Object>) o;
                if (it.get("lineId") != null) {
                    byLine.put(Long.valueOf(String.valueOf(it.get("lineId"))), it);
                }
            }
        }
        for (PurchaseInquiryLine line : lines(id)) {
            Map<String, Object> it = byLine.get(line.getId());
            if (it == null || it.get("quotedPrice") == null
                    || new BigDecimal(String.valueOf(it.get("quotedPrice"))).compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("明细行 " + line.getPartNo() + " 缺少有效报价");
            }
            line.setQuotedPrice(new BigDecimal(String.valueOf(it.get("quotedPrice"))));
            if (it.get("leadDays") != null) {
                line.setLeadDays(((Number) it.get("leadDays")).intValue());
            }
            lineMapper.updateById(line);
        }
        if (body != null) {
            q.setOemRemark(str(body.get("oemRemark")));
        }
        q.setQuotedAt(LocalDateTime.now());
        q.setStatus("QUOTED");
        mapper.updateById(q);
        return q;
    }

    @Transactional
    public PurchaseInquiry close(Long id) {
        PurchaseInquiry q = mustGet(id);
        DataScope.check(q.getDealerCode());
        if ("ORDERED".equals(q.getStatus()) || "CLOSED".equals(q.getStatus())) {
            throw new BizException("已转订单或已关闭的询价单不可关闭");
        }
        q.setStatus("CLOSED");
        mapper.updateById(q);
        return q;
    }

    /** 询价转采购订单：按报价生成 DRAFT 采购单。 */
    @Transactional
    public com.dms.procure.entity.PurchaseOrder toOrder(Long id) {
        PurchaseInquiry q = mustGet(id);
        DataScope.check(q.getDealerCode());
        if (!"QUOTED".equals(q.getStatus())) {
            throw new BizException("仅已报价的询价单可转采购订单");
        }
        com.dms.procure.entity.PurchaseOrder po = orderService.createFromInquiry(q, lines(id));
        q.setStatus("ORDERED");
        mapper.updateById(q);
        return po;
    }

    /** 仅 DRAFT/CLOSED 可删除，连同明细。 */
    @Transactional
    public void delete(Long id) {
        PurchaseInquiry q = mustGet(id);
        DataScope.check(q.getDealerCode());
        if (!"DRAFT".equals(q.getStatus()) && !"CLOSED".equals(q.getStatus())) {
            throw new BizException("仅草稿或已关闭的询价单可删除");
        }
        lineMapper.delete(new QueryWrapper<PurchaseInquiryLine>().eq("inquiry_id", id));
        mapper.deleteById(id);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
