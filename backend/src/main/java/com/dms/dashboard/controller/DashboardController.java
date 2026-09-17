package com.dms.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.R;
import com.dms.invoice.entity.Invoice;
import com.dms.invoice.mapper.InvoiceMapper;
import com.dms.network.entity.Dealer;
import com.dms.network.entity.Technician;
import com.dms.network.mapper.DealerMapper;
import com.dms.network.mapper.TechnicianMapper;
import com.dms.parts.service.PartStockService;
import com.dms.survey.entity.Complaint;
import com.dms.survey.entity.Survey;
import com.dms.survey.mapper.ComplaintMapper;
import com.dms.survey.mapper.SurveyMapper;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.mapper.WorkOrderMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final WorkOrderMapper orderMapper;
    private final TechnicianMapper technicianMapper;
    private final SurveyMapper surveyMapper;
    private final ComplaintMapper complaintMapper;
    private final InvoiceMapper invoiceMapper;
    private final DealerMapper dealerMapper;
    private final PartStockService stockService;

    @GetMapping
    public R<Map<String, Object>> summary(@RequestParam(required = false) String dealerCode) {
        dealerCode = DataScope.effectiveDealer(dealerCode);
        Map<String, Object> m = new LinkedHashMap<>();
        String monthStart = LocalDate.now().withDayOfMonth(1).toString();

        List<WorkOrder> all = orderMapper.selectList(dealerQ(dealerCode));
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        long todayCheckIns = 0;
        long inRepair = 0;
        BigDecimal monthRevenue = BigDecimal.ZERO;
        long monthSettled = 0;
        long repairHoursSum = 0;
        long repairCount = 0;
        for (WorkOrder o : all) {
            statusCounts.merge(o.getStatus(), 1L, Long::sum);
            if (o.getCheckInTime() != null
                    && o.getCheckInTime().toLocalDate().equals(LocalDate.now())) {
                todayCheckIns++;
            }
            if ("IN_REPAIR".equals(o.getStatus())) {
                inRepair++;
            }
            if (o.getSettleTime() != null
                    && !o.getSettleTime().toLocalDate().isBefore(LocalDate.now().withDayOfMonth(1))) {
                monthSettled++;
                if (o.getTotalAmount() != null) {
                    monthRevenue = monthRevenue.add(o.getTotalAmount());
                }
            }
            if (o.getRepairStartTime() != null && o.getRepairEndTime() != null) {
                repairHoursSum +=
                        Duration.between(o.getRepairStartTime(), o.getRepairEndTime()).toMinutes();
                repairCount++;
            }
        }
        m.put("workOrderStatusCounts", statusCounts);
        m.put("todayCheckIns", todayCheckIns);
        m.put("inRepairCount", inRepair);
        m.put("monthRevenue", monthRevenue);
        m.put("monthSettledOrders", monthSettled);
        m.put(
                "avgRepairHours",
                repairCount == 0
                        ? 0
                        : new BigDecimal(repairHoursSum)
                                .divide(new BigDecimal(repairCount * 60), 2, RoundingMode.HALF_UP));

        List<Technician> techs = technicianMapper.selectList(dealerQ2(dealerCode));
        long busy = techs.stream().filter(t -> "BUSY".equals(t.getStatus())).count();
        m.put(
                "technicianUtilization",
                techs.isEmpty()
                        ? null
                        : new BigDecimal(busy * 100)
                                .divide(new BigDecimal(techs.size()), 1, RoundingMode.HALF_UP));

        long shortage = 0;
        for (Map<String, Object> row : stockService.shortage()) {
            if (dealerCode == null || dealerCode.equals(row.get("dealerCode"))) {
                shortage++;
            }
        }
        m.put("partsShortageCount", shortage);

        List<Survey> monthSurveys =
                surveyMapper.selectList(
                        dealerQ3(dealerCode)
                                .eq("status", "ANSWERED")
                                .ge("answered_time", monthStart));
        long promoters = 0;
        long detractors = 0;
        long npsCount = 0;
        for (Survey s : monthSurveys) {
            if (s.getNpsScore() == null) {
                continue;
            }
            npsCount++;
            if (s.getNpsScore().compareTo(new BigDecimal("9")) >= 0) {
                promoters++;
            } else if (s.getNpsScore().compareTo(new BigDecimal("6")) <= 0) {
                detractors++;
            }
        }
        m.put(
                "npsThisMonth",
                npsCount == 0
                        ? null
                        : new BigDecimal(promoters - detractors)
                                .multiply(new BigDecimal("100"))
                                .divide(new BigDecimal(npsCount), 1, RoundingMode.HALF_UP));
        m.put(
                "openComplaints",
                complaintMapper.selectCount(
                        dealerQ4(dealerCode).in("status", "OPEN", "PROCESSING")));
        m.put(
                "invoiceIssuedThisMonth",
                invoiceMapper.selectCount(
                        dealerQ5(dealerCode).eq("status", "ISSUED").ge("issued_time", monthStart)));
        m.put(
                "pendingSurveys",
                surveyMapper.selectCount(dealerQ3(dealerCode).eq("status", "PENDING")));
        m.put("dealerRanking", dealerRanking(monthStart, dealerCode));
        return R.ok(m);
    }

    /** 各经销商当月结算金额排名。 */
    private List<Map<String, Object>> dealerRanking(String monthStart, String dealerCode) {
        Map<String, BigDecimal> revenue = new HashMap<>();
        QueryWrapper<WorkOrder> rankQ =
                new QueryWrapper<WorkOrder>()
                        .in("status", "SETTLED", "DELIVERED", "CLOSED")
                        .ge("settle_time", monthStart);
        if (dealerCode != null && !dealerCode.isEmpty()) {
            rankQ.eq("dealer_code", dealerCode);
        }
        for (WorkOrder o : orderMapper.selectList(rankQ)) {
            if (o.getTotalAmount() != null) {
                revenue.merge(o.getDealerCode(), o.getTotalAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : revenue.entrySet()) {
            Dealer d =
                    dealerMapper.selectOne(
                            new QueryWrapper<Dealer>().eq("code", e.getKey()).last("LIMIT 1"));
            Map<String, Object> row = new HashMap<>();
            row.put("dealerCode", e.getKey());
            row.put("dealerName", d == null ? e.getKey() : d.getName());
            row.put("monthRevenue", e.getValue());
            list.add(row);
        }
        list.sort(
                Comparator.comparing(
                        (Map<String, Object> r) -> (BigDecimal) r.get("monthRevenue")).reversed());
        return list;
    }

    private QueryWrapper<WorkOrder> dealerQ(String d) {
        QueryWrapper<WorkOrder> q = new QueryWrapper<>();
        if (d != null && !d.isEmpty()) {
            q.eq("dealer_code", d);
        }
        return q;
    }

    private QueryWrapper<Technician> dealerQ2(String d) {
        QueryWrapper<Technician> q = new QueryWrapper<>();
        if (d != null && !d.isEmpty()) {
            q.eq("dealer_code", d);
        }
        return q;
    }

    private QueryWrapper<Survey> dealerQ3(String d) {
        QueryWrapper<Survey> q = new QueryWrapper<>();
        if (d != null && !d.isEmpty()) {
            q.eq("dealer_code", d);
        }
        return q;
    }

    private QueryWrapper<Complaint> dealerQ4(String d) {
        QueryWrapper<Complaint> q = new QueryWrapper<>();
        if (d != null && !d.isEmpty()) {
            q.eq("dealer_code", d);
        }
        return q;
    }

    private QueryWrapper<Invoice> dealerQ5(String d) {
        QueryWrapper<Invoice> q = new QueryWrapper<>();
        if (d != null && !d.isEmpty()) {
            q.eq("dealer_code", d);
        }
        return q;
    }
}
