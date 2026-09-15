package com.dms.workshop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.customer.entity.Vehicle;
import com.dms.customer.entity.VehicleModel;
import com.dms.customer.mapper.VehicleMapper;
import com.dms.customer.mapper.VehicleModelMapper;
import com.dms.guide.entity.LaborItem;
import com.dms.guide.entity.RepairGuide;
import com.dms.guide.mapper.LaborItemMapper;
import com.dms.guide.mapper.RepairGuideMapper;
import com.dms.guide.service.GuideMatcher;
import com.dms.invoice.service.InvoiceService;
import com.dms.network.entity.Bay;
import com.dms.network.entity.Dealer;
import com.dms.network.entity.Technician;
import com.dms.network.mapper.BayMapper;
import com.dms.network.mapper.DealerMapper;
import com.dms.network.mapper.TechnicianMapper;
import com.dms.parts.entity.Part;
import com.dms.parts.mapper.PartMapper;
import com.dms.parts.service.PartStockService;
import com.dms.survey.service.SurveyService;
import com.dms.workshop.entity.*;
import com.dms.workshop.mapper.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private final WorkOrderMapper orderMapper;
    private final WorkOrderLaborMapper laborMapper;
    private final WorkOrderPartMapper partLineMapper;
    private final WorkOrderLogMapper logMapper;
    private final AppointmentMapper appointmentMapper;
    private final WarrantyClaimMapper claimMapper;
    private final TechnicianMapper technicianMapper;
    private final BayMapper bayMapper;
    private final DealerMapper dealerMapper;
    private final LaborItemMapper laborItemMapper;
    private final PartMapper partMapper;
    private final RepairGuideMapper guideMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleModelMapper modelMapper;
    private final PartStockService stockService;
    private final CodeGenerator codeGenerator;
    private final InvoiceService invoiceService;
    private final SurveyService surveyService;

    public WorkOrder mustGet(Long id) {
        WorkOrder o = orderMapper.selectById(id);
        if (o == null) {
            throw new BizException("工单不存在: " + id);
        }
        DataScope.check(o.getDealerCode());
        return o;
    }

    public Map<String, Object> detail(Long id) {
        WorkOrder o = mustGet(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", o);
        m.put("labors", labors(id));
        m.put("parts", parts(id));
        m.put(
                "logs",
                logMapper.selectList(
                        new QueryWrapper<WorkOrderLog>().eq("order_id", id).orderByAsc("id")));
        return m;
    }

    private List<WorkOrderLabor> labors(Long orderId) {
        return laborMapper.selectList(
                new QueryWrapper<WorkOrderLabor>().eq("order_id", orderId).orderByAsc("id"));
    }

    private List<WorkOrderPart> parts(Long orderId) {
        return partLineMapper.selectList(
                new QueryWrapper<WorkOrderPart>().eq("order_id", orderId).orderByAsc("id"));
    }

    private void transit(WorkOrder o, String to, String operator, String remark) {
        if (!WorkOrderCalculator.canTransit(o.getStatus(), to)) {
            throw new BizException("非法状态流转: " + o.getStatus() + " -> " + to);
        }
        WorkOrderLog l = new WorkOrderLog();
        l.setOrderId(o.getId());
        l.setFromStatus(o.getStatus());
        l.setToStatus(to);
        l.setOperator(operator);
        l.setLogTime(LocalDateTime.now());
        l.setRemark(remark);
        logMapper.insert(l);
        o.setStatus(to);
    }

    private Dealer dealer(String code) {
        return dealerMapper.selectOne(new QueryWrapper<Dealer>().eq("code", code));
    }

    /** 创建工单：可关联预约或直接进店，直接置为已接车 CHECKED_IN。 */
    @Transactional
    public WorkOrder create(WorkOrder o) {
        o.setId(null);
        o.setOrderNo(codeGenerator.next("WO"));
        if (o.getAppointmentId() != null) {
            Appointment a = appointmentMapper.selectById(o.getAppointmentId());
            if (a == null) {
                throw new BizException("预约不存在");
            }
            o.setDealerCode(a.getDealerCode());
            o.setCustomerId(a.getCustomerId());
            o.setVehicleId(a.getVehicleId());
            if (o.getServiceType() == null) {
                o.setServiceType(a.getServiceType());
            }
            a.setStatus("ARRIVED");
            appointmentMapper.updateById(a);
        }
        if (o.getVehicleId() != null) {
            Vehicle v = vehicleMapper.selectById(o.getVehicleId());
            if (v != null) {
                o.setVin(v.getVin());
                o.setPlateNo(v.getPlateNo());
                if (o.getCustomerId() == null) {
                    o.setCustomerId(v.getCustomerId());
                }
                if (o.getDealerCode() == null) {
                    o.setDealerCode(v.getDealerCode());
                }
            }
        }
        if (o.getDealerCode() == null) {
            throw new BizException("dealerCode 必填");
        }
        DataScope.check(o.getDealerCode());
        if (o.getOrderType() == null) {
            o.setOrderType("REGULAR");
        }
        o.setStatus(WorkOrderCalculator.DRAFT);
        orderMapper.insert(o);
        transit(o, WorkOrderCalculator.CHECKED_IN, o.getAdvisorName(), "接车登记");
        o.setCheckInTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    private void ensureEditable(WorkOrder o) {
        String s = o.getStatus();
        if (!(WorkOrderCalculator.DRAFT.equals(s)
                || WorkOrderCalculator.CHECKED_IN.equals(s)
                || WorkOrderCalculator.DIAGNOSED.equals(s)
                || WorkOrderCalculator.QUOTED.equals(s))) {
            throw new BizException("当前状态 " + s + " 不允许增删工时/备件");
        }
    }

    @Transactional
    public WorkOrderLabor addLabor(Long orderId, WorkOrderLabor line) {
        WorkOrder o = mustGet(orderId);
        ensureEditable(o);
        LaborItem li =
                laborItemMapper.selectOne(
                        new QueryWrapper<LaborItem>().eq("code", line.getLaborCode()));
        if (li == null) {
            throw new BizException("工时项目不存在: " + line.getLaborCode());
        }
        Dealer d = dealer(o.getDealerCode());
        line.setId(null);
        line.setOrderId(orderId);
        line.setName(li.getName());
        line.setHours(line.getHours() != null ? line.getHours() : li.getStandardHours());
        BigDecimal rate =
                d != null && d.getLaborRate() != null ? d.getLaborRate() : new BigDecimal("150");
        line.setRate(rate);
        line.setAmount(line.getHours().multiply(rate).setScale(2, RoundingMode.HALF_UP));
        if (line.getIsWarranty() == null) {
            line.setIsWarranty(false);
        }
        laborMapper.insert(line);
        return line;
    }

    @Transactional
    public WorkOrderPart addPart(Long orderId, WorkOrderPart line) {
        WorkOrder o = mustGet(orderId);
        ensureEditable(o);
        Part p =
                partMapper.selectOne(new QueryWrapper<Part>().eq("part_no", line.getPartNo()));
        if (p == null) {
            throw new BizException("备件不存在: " + line.getPartNo());
        }
        line.setId(null);
        line.setOrderId(orderId);
        line.setName(p.getName());
        line.setUnitPrice(line.getUnitPrice() != null ? line.getUnitPrice() : p.getSalePrice());
        if (line.getQty() == null || line.getQty() <= 0) {
            line.setQty(1);
        }
        line.setAmount(
                line.getUnitPrice()
                        .multiply(new BigDecimal(line.getQty()))
                        .setScale(2, RoundingMode.HALF_UP));
        if (line.getIsWarranty() == null) {
            line.setIsWarranty(false);
        }
        line.setReservedFlag(false);
        partLineMapper.insert(line);
        return line;
    }

    @Transactional
    public void removeLabor(Long orderId, Long lineId) {
        ensureEditable(mustGet(orderId));
        WorkOrderLabor line = laborMapper.selectById(lineId);
        if (line != null && !orderId.equals(line.getOrderId())) {
            throw new BizException("明细不属于该工单");
        }
        laborMapper.deleteById(lineId);
    }

    @Transactional
    public void removePart(Long orderId, Long lineId) {
        WorkOrder o = mustGet(orderId);
        ensureEditable(o);
        WorkOrderPart p = partLineMapper.selectById(lineId);
        if (p != null && !orderId.equals(p.getOrderId())) {
            throw new BizException("明细不属于该工单");
        }
        if (p != null && Boolean.TRUE.equals(p.getReservedFlag())) {
            stockService.release(o.getDealerCode(), p.getPartNo(), p.getQty(), "WO", o.getOrderNo());
        }
        partLineMapper.deleteById(lineId);
    }

    /** 一键带入维修指导：工时行 + 备件行。 */
    @Transactional
    public WorkOrder applyGuide(Long orderId, String guideCode) {
        WorkOrder o = mustGet(orderId);
        ensureEditable(o);
        RepairGuide g =
                guideMapper.selectOne(new QueryWrapper<RepairGuide>().eq("code", guideCode));
        if (g == null) {
            throw new BizException("维修指导不存在: " + guideCode);
        }
        for (String code : GuideMatcher.splitList(g.getLaborItemCodes())) {
            WorkOrderLabor l = new WorkOrderLabor();
            l.setLaborCode(code.trim());
            l.setIsWarranty("WARRANTY".equals(o.getOrderType()));
            addLabor(orderId, l);
        }
        for (String no : GuideMatcher.splitList(g.getPartNos())) {
            WorkOrderPart p = new WorkOrderPart();
            p.setPartNo(no.trim());
            p.setQty(1);
            p.setIsWarranty("WARRANTY".equals(o.getOrderType()));
            addPart(orderId, p);
        }
        return mustGet(orderId);
    }

    @Transactional
    public WorkOrder diagnose(Long id, String diagnosis, String operator) {
        WorkOrder o = mustGet(id);
        o.setDiagnosis(diagnosis);
        transit(o, WorkOrderCalculator.DIAGNOSED, operator, "诊断完成");
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public WorkOrder quote(Long id, String operator) {
        WorkOrder o = mustGet(id);
        WorkOrderCalculator.compute(o, labors(id), parts(id));
        transit(o, WorkOrderCalculator.QUOTED, operator, "报价完成");
        o.setQuoteTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    /** 客户确认：预留全部备件，不足则抛错并列出缺口。 */
    @Transactional
    public WorkOrder approve(Long id, String operator) {
        WorkOrder o = mustGet(id);
        List<WorkOrderPart> lines = parts(id);
        // 先按备件号聚合需求量再整体校验，避免同备件多行各自通过但总量不足
        Map<String, Integer> required = new LinkedHashMap<>();
        Map<String, String> names = new HashMap<>();
        for (WorkOrderPart p : lines) {
            if (Boolean.TRUE.equals(p.getReservedFlag())) {
                continue; // 已预留的不再重复校验
            }
            required.merge(p.getPartNo(), p.getQty(), Integer::sum);
            names.put(p.getPartNo(), p.getName());
        }
        StringBuilder shortage = new StringBuilder();
        for (Map.Entry<String, Integer> e : required.entrySet()) {
            int avail = stockService.available(o.getDealerCode(), e.getKey());
            if (avail < e.getValue()) {
                shortage.append(e.getKey())
                        .append("(")
                        .append(names.get(e.getKey()))
                        .append(")缺")
                        .append(e.getValue() - avail)
                        .append(" ");
            }
        }
        if (shortage.length() > 0) {
            throw new BizException("备件库存不足: " + shortage);
        }
        for (WorkOrderPart p : lines) {
            if (!Boolean.TRUE.equals(p.getReservedFlag())) {
                stockService.reserve(
                        o.getDealerCode(), p.getPartNo(), p.getQty(), "WO", o.getOrderNo());
                p.setReservedFlag(true);
                partLineMapper.updateById(p);
            }
        }
        transit(o, WorkOrderCalculator.APPROVED, operator, "客户确认报价");
        o.setApproveTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public WorkOrder dispatch(Long id, String technicianCode, String bayCode, String operator) {
        WorkOrder o = mustGet(id);
        Technician t =
                technicianMapper.selectOne(
                        new QueryWrapper<Technician>().eq("code", technicianCode));
        if (t == null || !"IDLE".equals(t.getStatus())) {
            throw new BizException("技师不存在或非空闲状态: " + technicianCode);
        }
        Bay b =
                bayMapper.selectOne(
                        new QueryWrapper<Bay>()
                                .eq("dealer_code", o.getDealerCode())
                                .eq("code", bayCode));
        if (b == null || !"IDLE".equals(b.getStatus())) {
            throw new BizException("工位不存在或非空闲状态: " + bayCode);
        }
        t.setStatus("BUSY");
        technicianMapper.updateById(t);
        b.setStatus("BUSY");
        bayMapper.updateById(b);
        o.setTechnicianCode(technicianCode);
        o.setBayCode(bayCode);
        transit(o, WorkOrderCalculator.DISPATCHED, operator, "派工 " + technicianCode + "/" + bayCode);
        o.setDispatchTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public WorkOrder start(Long id, String operator) {
        WorkOrder o = mustGet(id);
        transit(o, WorkOrderCalculator.IN_REPAIR, operator, "开工");
        o.setRepairStartTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    /** 完工提交质检：消耗预留备件，释放技师与工位。 */
    @Transactional
    public WorkOrder finish(Long id, String operator) {
        WorkOrder o = mustGet(id);
        for (WorkOrderPart p : parts(id)) {
            if (Boolean.TRUE.equals(p.getReservedFlag())
                    && !Boolean.TRUE.equals(p.getConsumedFlag())) {
                stockService.consume(
                        o.getDealerCode(), p.getPartNo(), p.getQty(), "WO", o.getOrderNo());
                p.setConsumedFlag(true);
                p.setReservedFlag(false);
                partLineMapper.updateById(p);
            }
        }
        releaseResources(o);
        transit(o, WorkOrderCalculator.QC_PENDING, operator, "完工待质检");
        o.setRepairEndTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    private void releaseResources(WorkOrder o) {
        if (o.getTechnicianCode() != null) {
            Technician t =
                    technicianMapper.selectOne(
                            new QueryWrapper<Technician>().eq("code", o.getTechnicianCode()));
            if (t != null) {
                t.setStatus("IDLE");
                technicianMapper.updateById(t);
            }
        }
        if (o.getBayCode() != null) {
            Bay b =
                    bayMapper.selectOne(
                            new QueryWrapper<Bay>()
                                    .eq("dealer_code", o.getDealerCode())
                                    .eq("code", o.getBayCode()));
            if (b != null) {
                b.setStatus("IDLE");
                bayMapper.updateById(b);
            }
        }
    }

    @Transactional
    public WorkOrder qc(Long id, boolean pass, String qcRemark, String operator) {
        WorkOrder o = mustGet(id);
        if (pass) {
            transit(o, WorkOrderCalculator.QC_PASSED, operator, "质检合格");
            o.setQcResult("PASSED");
        } else {
            transit(o, WorkOrderCalculator.QC_FAILED, operator, "质检不合格: " + qcRemark);
            o.setQcResult("FAILED");
            // 返工时重新占用原技师与工位（finish 时已释放）；已被占用则在日志备注
            String reworkRemark = reoccupyResources(o);
            transit(o, WorkOrderCalculator.IN_REPAIR, operator,
                    "返工" + (reworkRemark == null ? "" : "（" + reworkRemark + "）"));
        }
        o.setQcRemark(qcRemark);
        o.setQcTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    /** 返工时重新占用原技师与工位；返回 null 表示成功，否则返回需重新派工的说明。 */
    private String reoccupyResources(WorkOrder o) {
        StringBuilder note = new StringBuilder();
        if (o.getTechnicianCode() != null) {
            Technician t =
                    technicianMapper.selectOne(
                            new QueryWrapper<Technician>().eq("code", o.getTechnicianCode()));
            if (t != null && "IDLE".equals(t.getStatus())) {
                t.setStatus("BUSY");
                technicianMapper.updateById(t);
            } else if (t != null) {
                note.append("技师 ").append(o.getTechnicianCode()).append(" 已被占用");
            }
        }
        if (o.getBayCode() != null) {
            Bay b =
                    bayMapper.selectOne(
                            new QueryWrapper<Bay>()
                                    .eq("dealer_code", o.getDealerCode())
                                    .eq("code", o.getBayCode()));
            if (b != null && "IDLE".equals(b.getStatus())) {
                b.setStatus("BUSY");
                bayMapper.updateById(b);
            } else if (b != null) {
                if (note.length() > 0) {
                    note.append("；");
                }
                note.append("工位 ").append(o.getBayCode()).append(" 已被占用");
            }
        }
        return note.length() == 0 ? null : note.append("，需重新派工").toString();
    }

    /**
     * 结算：重算金额、折扣、税额；保修金额生成厂家索赔；需要开票则自动创建 DRAFT 发票。
     */
    @Transactional
    public WorkOrder settle(Long id, Map<String, Object> body, String operator) {
        WorkOrder o = mustGet(id);
        if (body.get("discountAmount") != null) {
            o.setDiscountAmount(new BigDecimal(body.get("discountAmount").toString()));
        }
        WorkOrderCalculator.compute(o, labors(id), parts(id));
        transit(o, WorkOrderCalculator.SETTLED, operator, "结算");
        o.setSettleTime(LocalDateTime.now());
        if (o.getWarrantyAmount() != null
                && o.getWarrantyAmount().compareTo(BigDecimal.ZERO) > 0) {
            WarrantyClaim c = new WarrantyClaim();
            c.setClaimNo(codeGenerator.next("WC"));
            c.setOrderId(o.getId());
            c.setDealerCode(o.getDealerCode());
            c.setAmount(o.getWarrantyAmount());
            c.setStatus("SUBMITTED");
            claimMapper.insert(c);
        }
        if (Boolean.TRUE.equals(body.get("needInvoice"))) {
            Long invoiceId =
                    invoiceService.createFromWorkOrder(
                            o,
                            (String) body.get("invoiceType"),
                            (String) body.get("buyerName"),
                            (String) body.get("buyerTaxNo"));
            o.setInvoiceId(invoiceId);
        }
        orderMapper.updateById(o);
        return o;
    }

    /** 交车：更新车辆里程/上次保养/下次保养里程，自动创建满意度调研。 */
    @Transactional
    public WorkOrder deliver(Long id, String operator) {
        WorkOrder o = mustGet(id);
        transit(o, WorkOrderCalculator.DELIVERED, operator, "交车");
        o.setDeliverTime(LocalDateTime.now());
        if (o.getVehicleId() != null) {
            Vehicle v = vehicleMapper.selectById(o.getVehicleId());
            if (v != null) {
                if (o.getMileageIn() != null) {
                    v.setMileage(o.getMileageIn());
                }
                v.setLastServiceDate(LocalDate.now());
                VehicleModel m =
                        v.getModelCode() == null
                                ? null
                                : modelMapper.selectOne(
                                        new QueryWrapper<VehicleModel>()
                                                .eq("code", v.getModelCode()));
                if (m != null && m.getMaintenanceIntervalKm() != null && v.getMileage() != null) {
                    v.setNextServiceMileage(v.getMileage() + m.getMaintenanceIntervalKm());
                }
                vehicleMapper.updateById(v);
            }
        }
        Long surveyId = surveyService.createForOrder(o);
        o.setSurveyId(surveyId);
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public WorkOrder close(Long id, String operator) {
        WorkOrder o = mustGet(id);
        transit(o, WorkOrderCalculator.CLOSED, operator, "关单");
        orderMapper.updateById(o);
        return o;
    }

    /** 取消：DISPATCHED 之前均可取消，释放备件预留。 */
    @Transactional
    public WorkOrder cancel(Long id, String operator) {
        WorkOrder o = mustGet(id);
        String s = o.getStatus();
        if (!(WorkOrderCalculator.DRAFT.equals(s)
                || WorkOrderCalculator.CHECKED_IN.equals(s)
                || WorkOrderCalculator.DIAGNOSED.equals(s)
                || WorkOrderCalculator.QUOTED.equals(s)
                || WorkOrderCalculator.APPROVED.equals(s))) {
            throw new BizException("当前状态 " + s + " 不允许取消");
        }
        for (WorkOrderPart p : parts(id)) {
            if (Boolean.TRUE.equals(p.getReservedFlag())) {
                stockService.release(
                        o.getDealerCode(), p.getPartNo(), p.getQty(), "WO", o.getOrderNo());
                p.setReservedFlag(false);
                partLineMapper.updateById(p);
            }
        }
        transit(o, WorkOrderCalculator.CANCELLED, operator, "取消工单");
        orderMapper.updateById(o);
        return o;
    }
}
