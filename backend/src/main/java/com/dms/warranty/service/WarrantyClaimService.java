package com.dms.warranty.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.warranty.entity.WarrantyClaim;
import com.dms.warranty.entity.WarrantyClaimLine;
import com.dms.warranty.mapper.WarrantyClaimLineMapper;
import com.dms.warranty.mapper.WarrantyClaimMapper;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.entity.WorkOrderLabor;
import com.dms.workshop.entity.WorkOrderPart;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 保修索赔：经销商提交(DRAFT/RETURNED→SUBMITTED) → OEM 审核(APPROVED)/退回(RETURNED)/拒绝(REJECTED)，
 * 需回收配件时 PARTS_RETURNING → PARTS_SHIPPED → APPROVED，之后进入结算批次(SETTLED/PAID)。
 */
@Service
@RequiredArgsConstructor
public class WarrantyClaimService {
    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String RETURNED = "RETURNED";
    public static final String REJECTED = "REJECTED";
    public static final String PARTS_RETURNING = "PARTS_RETURNING";
    public static final String PARTS_SHIPPED = "PARTS_SHIPPED";
    public static final String APPROVED = "APPROVED";
    public static final String SETTLED = "SETTLED";
    public static final String PAID = "PAID";

    private final WarrantyClaimMapper mapper;
    private final WarrantyClaimLineMapper lineMapper;
    private final CodeGenerator codeGenerator;

    /** 工单结算时生成索赔草稿：复制车辆/故障信息，取 isWarranty 工时与配件明细为索赔行。 */
    @Transactional
    public WarrantyClaim createFromWorkOrder(
            WorkOrder o, List<WorkOrderLabor> labors, List<WorkOrderPart> parts) {
        WarrantyClaim c = new WarrantyClaim();
        c.setClaimNo(codeGenerator.next("WC"));
        c.setOrderId(o.getId());
        c.setDealerCode(o.getDealerCode());
        c.setVin(o.getVin());
        c.setPlateNo(o.getPlateNo());
        c.setMileage(o.getMileageIn());
        c.setRepairDate(LocalDate.now());
        c.setFaultDesc(
                o.getDiagnosis() != null && !o.getDiagnosis().trim().isEmpty()
                        ? o.getDiagnosis()
                        : o.getComplaint());
        c.setStatus(DRAFT);
        BigDecimal laborSum = BigDecimal.ZERO;
        BigDecimal partSum = BigDecimal.ZERO;
        mapper.insert(c);
        if (labors != null) {
            for (WorkOrderLabor l : labors) {
                if (!Boolean.TRUE.equals(l.getIsWarranty())) {
                    continue;
                }
                WarrantyClaimLine line = newLine(c.getId(), "LABOR", l.getLaborCode(),
                        l.getName(), l.getHours(), l.getRate(), l.getAmount());
                lineMapper.insert(line);
                laborSum = laborSum.add(nvl(line.getAmount()));
            }
        }
        if (parts != null) {
            for (WorkOrderPart p : parts) {
                if (!Boolean.TRUE.equals(p.getIsWarranty())) {
                    continue;
                }
                WarrantyClaimLine line = newLine(c.getId(), "PART", p.getPartNo(),
                        p.getName(),
                        p.getQty() == null ? null : new BigDecimal(p.getQty()),
                        p.getUnitPrice(), p.getAmount());
                lineMapper.insert(line);
                partSum = partSum.add(nvl(line.getAmount()));
            }
        }
        c.setLaborAmount(laborSum);
        c.setPartAmount(partSum);
        c.setAmount(laborSum.add(partSum));
        mapper.updateById(c);
        return c;
    }

    private WarrantyClaimLine newLine(
            Long claimId, String type, String code, String name,
            BigDecimal qty, BigDecimal unitPrice, BigDecimal amount) {
        WarrantyClaimLine line = new WarrantyClaimLine();
        line.setClaimId(claimId);
        line.setLineType(type);
        line.setCode(code);
        line.setName(name);
        line.setQty(qty);
        line.setUnitPrice(unitPrice);
        line.setAmount(amount);
        return line;
    }

    private WarrantyClaim mustGet(Long id) {
        WarrantyClaim c = mapper.selectById(id);
        if (c == null) {
            throw new BizException("索赔单不存在");
        }
        return c;
    }

    public List<WarrantyClaimLine> lines(Long id) {
        WarrantyClaim c = mustGet(id);
        DataScope.check(c.getDealerCode());
        return lineMapper.selectList(
                new QueryWrapper<WarrantyClaimLine>().eq("claim_id", id).orderByAsc("id"));
    }

    /** 经销商提交：DRAFT/RETURNED → SUBMITTED；需至少一条明细且故障描述非空。 */
    @Transactional
    public WarrantyClaim submit(Long id) {
        WarrantyClaim c = mustGet(id);
        DataScope.check(c.getDealerCode());
        if (!DRAFT.equals(c.getStatus()) && !RETURNED.equals(c.getStatus())) {
            throw new BizException("仅草稿或已退回的索赔单可提交");
        }
        if (lines(id).isEmpty()) {
            throw new BizException("索赔单无明细行，不能提交");
        }
        if (c.getFaultDesc() == null || c.getFaultDesc().trim().isEmpty()) {
            throw new BizException("请填写故障描述后再提交");
        }
        c.setStatus(SUBMITTED);
        c.setSubmittedAt(LocalDateTime.now());
        mapper.updateById(c);
        return c;
    }

    /** 经销商编辑：仅 DRAFT/RETURNED 可改故障码/故障描述/备注。 */
    @Transactional
    public WarrantyClaim update(Long id, Map<String, Object> body) {
        WarrantyClaim c = mustGet(id);
        DataScope.check(c.getDealerCode());
        if (!DRAFT.equals(c.getStatus()) && !RETURNED.equals(c.getStatus())) {
            throw new BizException("仅草稿或已退回的索赔单可修改");
        }
        if (body.get("faultCode") != null) {
            c.setFaultCode(String.valueOf(body.get("faultCode")));
        }
        if (body.get("faultDesc") != null) {
            c.setFaultDesc(String.valueOf(body.get("faultDesc")));
        }
        if (body.get("remark") != null) {
            c.setRemark(String.valueOf(body.get("remark")));
        }
        mapper.updateById(c);
        return c;
    }

    /** OEM 审核通过：可指定核价金额与需回收的配件行；有回收行则进入 PARTS_RETURNING。 */
    @Transactional
    public WarrantyClaim approve(Long id, Map<String, Object> body) {
        WarrantyClaim c = mustGet(id);
        if (!SUBMITTED.equals(c.getStatus())) {
            throw new BizException("仅已提交的索赔单可审核");
        }
        BigDecimal approved = c.getAmount();
        if (body != null && body.get("approvedAmount") != null) {
            approved = new BigDecimal(String.valueOf(body.get("approvedAmount")));
        }
        if (approved == null
                || approved.compareTo(BigDecimal.ZERO) <= 0
                || approved.compareTo(nvl(c.getAmount())) > 0) {
            throw new BizException("核准金额须大于 0 且不超过索赔金额");
        }
        boolean needReturn = false;
        if (body != null && body.get("returnLineIds") instanceof List) {
            for (Object lid : (List<?>) body.get("returnLineIds")) {
                WarrantyClaimLine line = lineMapper.selectById(Long.valueOf(String.valueOf(lid)));
                if (line == null || !c.getId().equals(line.getClaimId())) {
                    throw new BizException("回收行不属于该索赔单");
                }
                if (!"PART".equals(line.getLineType())) {
                    throw new BizException("仅配件行可要求回收");
                }
                line.setReturnRequired(true);
                lineMapper.updateById(line);
                needReturn = true;
            }
        }
        c.setApprovedAmount(approved);
        if (body != null && body.get("oemRemark") != null) {
            c.setOemRemark(String.valueOf(body.get("oemRemark")));
        }
        c.setPartsReturnRequired(needReturn);
        c.setApprovedAt(LocalDateTime.now());
        c.setStatus(needReturn ? PARTS_RETURNING : APPROVED);
        mapper.updateById(c);
        return c;
    }

    /** OEM 拒绝：需填写原因。 */
    @Transactional
    public WarrantyClaim reject(Long id, Map<String, Object> body) {
        WarrantyClaim c = mustGet(id);
        if (!SUBMITTED.equals(c.getStatus())) {
            throw new BizException("仅已提交的索赔单可拒绝");
        }
        c.setOemRemark(requiredRemark(body, "拒绝原因必填"));
        c.setStatus(REJECTED);
        mapper.updateById(c);
        return c;
    }

    /** OEM 退回经销商补充资料：需填写原因。 */
    @Transactional
    public WarrantyClaim returnBack(Long id, Map<String, Object> body) {
        WarrantyClaim c = mustGet(id);
        if (!SUBMITTED.equals(c.getStatus())) {
            throw new BizException("仅已提交的索赔单可退回");
        }
        c.setOemRemark(requiredRemark(body, "退回原因必填"));
        c.setStatus(RETURNED);
        mapper.updateById(c);
        return c;
    }

    private String requiredRemark(Map<String, Object> body, String message) {
        String remark = body == null || body.get("oemRemark") == null
                ? null
                : String.valueOf(body.get("oemRemark")).trim();
        if (remark == null || remark.isEmpty()) {
            throw new BizException(message);
        }
        return remark;
    }

    /** 经销商寄回旧件。 */
    @Transactional
    public WarrantyClaim ship(Long id, Map<String, Object> body) {
        WarrantyClaim c = mustGet(id);
        DataScope.check(c.getDealerCode());
        if (!PARTS_RETURNING.equals(c.getStatus())) {
            throw new BizException("仅待回收配件的索赔单可发货");
        }
        String shipNo = body == null || body.get("returnShipNo") == null
                ? null
                : String.valueOf(body.get("returnShipNo")).trim();
        if (shipNo == null || shipNo.isEmpty()) {
            throw new BizException("请填写旧件快递单号");
        }
        c.setReturnShipNo(shipNo);
        c.setReturnShippedAt(LocalDateTime.now());
        c.setStatus(PARTS_SHIPPED);
        mapper.updateById(c);
        return c;
    }

    /** OEM 签收旧件：进入 APPROVED。 */
    @Transactional
    public WarrantyClaim receive(Long id) {
        WarrantyClaim c = mustGet(id);
        if (!PARTS_SHIPPED.equals(c.getStatus())) {
            throw new BizException("仅旧件在途的索赔单可签收");
        }
        c.setReturnReceivedAt(LocalDateTime.now());
        c.setStatus(APPROVED);
        mapper.updateById(c);
        return c;
    }

    /** 经销商删除索赔单：已挂结算或已结算/已付款的禁止删除；同时删除明细行。 */
    @Transactional
    public void delete(Long id) {
        WarrantyClaim c = mustGet(id);
        DataScope.check(c.getDealerCode());
        if (c.getSettlementId() != null
                || SETTLED.equals(c.getStatus())
                || PAID.equals(c.getStatus())) {
            throw new BizException("已进入结算或已付款的索赔单不可删除");
        }
        lineMapper.delete(new QueryWrapper<WarrantyClaimLine>().eq("claim_id", id));
        mapper.deleteById(id);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
