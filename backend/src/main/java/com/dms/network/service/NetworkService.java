package com.dms.network.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.customer.entity.Vehicle;
import com.dms.customer.entity.VehicleModel;
import com.dms.customer.mapper.VehicleMapper;
import com.dms.customer.mapper.VehicleModelMapper;
import com.dms.network.entity.*;
import com.dms.network.mapper.*;
import com.dms.survey.entity.Complaint;
import com.dms.survey.entity.Survey;
import com.dms.survey.mapper.ComplaintMapper;
import com.dms.survey.mapper.SurveyMapper;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.mapper.WorkOrderMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NetworkService {
    private final DealerMapper dealerMapper;
    private final DealerTargetMapper targetMapper;
    private final DealerAssessmentMapper assessmentMapper;
    private final VehicleSalesOrderMapper salesOrderMapper;
    private final VehicleStockMapper stockMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleModelMapper modelMapper;
    private final WorkOrderMapper workOrderMapper;
    private final SurveyMapper surveyMapper;
    private final ComplaintMapper complaintMapper;
    private final CodeGenerator codeGenerator;

    /** 目标达成：按 dealerCode+yearMonth 统计整车交付数、工单数、已结算金额 vs 目标。 */
    public Map<String, Object> achievement(String dealerCode, String yearMonth) {
        String from = yearMonth + "-01";
        String to = YearMonth.parse(yearMonth).atEndOfMonth().toString();
        long salesDone =
                salesOrderMapper.selectCount(
                        new QueryWrapper<VehicleSalesOrder>()
                                .eq("dealer_code", dealerCode)
                                .eq("status", "DELIVERED")
                                .ge("updated_at", from)
                                .le("updated_at", to + " 23:59:59"));
        long serviceDone =
                workOrderMapper.selectCount(
                        new QueryWrapper<WorkOrder>()
                                .eq("dealer_code", dealerCode)
                                .in("status", "SETTLED", "DELIVERED", "CLOSED")
                                .ge("settle_time", from)
                                .le("settle_time", to + " 23:59:59"));
        BigDecimal revenue = BigDecimal.ZERO;
        List<WorkOrder> settled =
                workOrderMapper.selectList(
                        new QueryWrapper<WorkOrder>()
                                .eq("dealer_code", dealerCode)
                                .in("status", "SETTLED", "DELIVERED", "CLOSED")
                                .ge("settle_time", from)
                                .le("settle_time", to + " 23:59:59"));
        for (WorkOrder o : settled) {
            if (o.getTotalAmount() != null) {
                revenue = revenue.add(o.getTotalAmount());
            }
        }
        DealerTarget t =
                targetMapper.selectOne(
                        new QueryWrapper<DealerTarget>()
                                .eq("dealer_code", dealerCode)
                                .eq("year_month", yearMonth));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dealerCode", dealerCode);
        m.put("yearMonth", yearMonth);
        m.put("target", t);
        m.put("salesActual", salesDone);
        m.put("serviceActual", serviceDone);
        m.put("revenueActual", revenue);
        m.put(
                "salesRate",
                pct(salesDone, t == null ? null : t.getSalesTarget()));
        m.put(
                "serviceRate",
                pct(serviceDone, t == null ? null : t.getServiceTarget()));
        m.put(
                "revenueRate",
                t != null && t.getRevenueTarget() != null
                                && t.getRevenueTarget().compareTo(BigDecimal.ZERO) > 0
                        ? revenue.divide(t.getRevenueTarget(), 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                                .setScale(2, RoundingMode.HALF_UP)
                        : null);
        return m;
    }

    private BigDecimal pct(long actual, Integer target) {
        if (target == null || target == 0) {
            return null;
        }
        return new BigDecimal(actual)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(target), 2, RoundingMode.HALF_UP);
    }

    /**
     * 经销商考核公式（权重 30/30/30/10，每项 0-100）：
     * salesScore = 整车达成率（封顶100）；serviceScore = 工单达成率（封顶100）；
     * csiScore = 当月已答卷平均分（0-100 直读），无答卷记 80 基准分；
     * complianceScore = 100 - 未关闭投诉*10（最低 0）。
     * grade: total>=90 A, >=80 B, >=70 C, >=60 D, 否则 E。
     */
    @Transactional
    public DealerAssessment generateAssessment(String dealerCode, String yearMonth) {
        Map<String, Object> ach = achievement(dealerCode, yearMonth);
        BigDecimal sales = cap100((BigDecimal) ach.get("salesRate"));
        BigDecimal service = cap100((BigDecimal) ach.get("serviceRate"));

        List<Survey> surveys =
                surveyMapper.selectList(
                        new QueryWrapper<Survey>()
                                .eq("dealer_code", dealerCode)
                                .eq("status", "ANSWERED")
                                .ge("answered_time", yearMonth + "-01")
                                .le("answered_time", YearMonth.parse(yearMonth).atEndOfMonth() + " 23:59:59"));
        BigDecimal csi = new BigDecimal("80");
        if (!surveys.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Survey s : surveys) {
                sum = sum.add(s.getTotalScore() == null ? BigDecimal.ZERO : s.getTotalScore());
            }
            csi = sum.divide(new BigDecimal(surveys.size()), 2, RoundingMode.HALF_UP);
        }

        long complaints =
                complaintMapper.selectCount(
                        new QueryWrapper<Complaint>()
                                .eq("dealer_code", dealerCode)
                                .in("status", "OPEN", "PROCESSING")
                                .ge("created_at", yearMonth + "-01")
                                .le("created_at", YearMonth.parse(yearMonth).atEndOfMonth() + " 23:59:59"));
        BigDecimal compliance =
                BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(complaints * 10)).max(BigDecimal.ZERO);

        BigDecimal total =
                sales.multiply(new BigDecimal("0.3"))
                        .add(service.multiply(new BigDecimal("0.3")))
                        .add(csi.multiply(new BigDecimal("0.3")))
                        .add(compliance.multiply(new BigDecimal("0.1")))
                        .setScale(2, RoundingMode.HALF_UP);

        DealerAssessment a =
                assessmentMapper.selectOne(
                        new QueryWrapper<DealerAssessment>()
                                .eq("dealer_code", dealerCode)
                                .eq("year_month", yearMonth));
        if (a == null) {
            a = new DealerAssessment();
            a.setDealerCode(dealerCode);
            a.setYearMonth(yearMonth);
        }
        a.setSalesScore(sales);
        a.setServiceScore(service);
        a.setCsiScore(csi);
        a.setComplianceScore(compliance);
        a.setTotal(total);
        a.setGrade(
                total.compareTo(new BigDecimal("90")) >= 0
                        ? "A"
                        : total.compareTo(new BigDecimal("80")) >= 0
                                ? "B"
                                : total.compareTo(new BigDecimal("70")) >= 0
                                        ? "C"
                                        : total.compareTo(new BigDecimal("60")) >= 0 ? "D" : "E");
        if (a.getId() == null) {
            assessmentMapper.insert(a);
        } else {
            assessmentMapper.updateById(a);
        }
        return a;
    }

    private BigDecimal cap100(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        return rate.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ======== 整车销售 ========

    @Transactional
    public VehicleSalesOrder createSalesOrder(VehicleSalesOrder o) {
        o.setId(null);
        o.setOrderNo(codeGenerator.next("SO"));
        o.setStatus("NEW");
        salesOrderMapper.insert(o);
        return o;
    }

    /** 分配：从该经销商整车库存中挑一台匹配车型/颜色的在库车。 */
    @Transactional
    public VehicleSalesOrder allocate(Long id) {
        VehicleSalesOrder o = mustGet(id);
        if (!"NEW".equals(o.getStatus())) {
            throw new BizException("只有新建订单可以分配车辆");
        }
        QueryWrapper<VehicleStock> q =
                new QueryWrapper<VehicleStock>()
                        .eq("dealer_code", o.getDealerCode())
                        .eq("model_code", o.getModelCode())
                        .eq("status", "IN_STOCK")
                        .orderByAsc("id");
        if (o.getColor() != null && !o.getColor().isEmpty()) {
            q.eq("color", o.getColor());
        }
        List<VehicleStock> stocks = stockMapper.selectList(q);
        if (stocks.isEmpty()) {
            throw new BizException("无匹配的在库整车");
        }
        VehicleStock s = stocks.get(0);
        s.setStatus("ALLOCATED");
        stockMapper.updateById(s);
        o.setVin(s.getVin());
        o.setStatus("ALLOCATED");
        salesOrderMapper.updateById(o);
        return o;
    }

    @Transactional
    public VehicleSalesOrder invoiceSalesOrder(Long id) {
        VehicleSalesOrder o = mustGet(id);
        if (!"ALLOCATED".equals(o.getStatus())) {
            throw new BizException("只有已分配订单可以开票");
        }
        o.setStatus("INVOICED");
        salesOrderMapper.updateById(o);
        return o;
    }

    /** 交付：生成售后 Vehicle 档案，保修期按车型（月数/里程）起算。 */
    @Transactional
    public VehicleSalesOrder deliver(Long id) {
        VehicleSalesOrder o = mustGet(id);
        if (!"ALLOCATED".equals(o.getStatus()) && !"INVOICED".equals(o.getStatus())) {
            throw new BizException("只有已分配/已开票订单可以交付");
        }
        VehicleStock s =
                stockMapper.selectOne(new QueryWrapper<VehicleStock>().eq("vin", o.getVin()));
        if (s != null) {
            s.setStatus("SOLD");
            stockMapper.updateById(s);
        }
        Vehicle v = new Vehicle();
        v.setVin(o.getVin());
        v.setModelCode(o.getModelCode());
        v.setCustomerId(o.getCustomerId());
        v.setDealerCode(o.getDealerCode());
        v.setMileage(0);
        v.setPurchaseDate(LocalDate.now());
        v.setWarrantyStart(LocalDate.now());
        VehicleModel m =
                modelMapper.selectOne(
                        new QueryWrapper<VehicleModel>().eq("code", o.getModelCode()));
        int months = m != null && m.getWarrantyMonths() != null ? m.getWarrantyMonths() : 36;
        v.setWarrantyEnd(LocalDate.now().plusMonths(months));
        if (m != null && m.getMaintenanceIntervalKm() != null) {
            v.setNextServiceMileage(m.getMaintenanceIntervalKm());
        }
        vehicleMapper.insert(v);
        o.setStatus("DELIVERED");
        salesOrderMapper.updateById(o);
        return o;
    }

    @Transactional
    public VehicleSalesOrder cancelSalesOrder(Long id) {
        VehicleSalesOrder o = mustGet(id);
        if ("DELIVERED".equals(o.getStatus()) || "CANCELLED".equals(o.getStatus())) {
            throw new BizException("订单已交付或已取消");
        }
        if ("ALLOCATED".equals(o.getStatus()) && o.getVin() != null) {
            VehicleStock s =
                    stockMapper.selectOne(new QueryWrapper<VehicleStock>().eq("vin", o.getVin()));
            if (s != null) {
                s.setStatus("IN_STOCK");
                stockMapper.updateById(s);
            }
        }
        o.setStatus("CANCELLED");
        salesOrderMapper.updateById(o);
        return o;
    }

    private VehicleSalesOrder mustGet(Long id) {
        VehicleSalesOrder o = salesOrderMapper.selectById(id);
        if (o == null) {
            throw new BizException("销售订单不存在");
        }
        return o;
    }
}
