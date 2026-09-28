package com.dms.report.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.crm.entity.FollowTask;
import com.dms.crm.mapper.FollowTaskMapper;
import com.dms.network.entity.Dealer;
import com.dms.network.entity.VehicleSalesOrder;
import com.dms.network.mapper.DealerMapper;
import com.dms.network.mapper.VehicleSalesOrderMapper;
import com.dms.parts.entity.StockMovement;
import com.dms.parts.mapper.StockMovementMapper;
import com.dms.parts.service.PartStockService;
import com.dms.procure.entity.PurchaseOrder;
import com.dms.procure.mapper.PurchaseOrderMapper;
import com.dms.report.entity.DailyReport;
import com.dms.report.mapper.DailyReportMapper;
import com.dms.survey.entity.Complaint;
import com.dms.survey.entity.Survey;
import com.dms.survey.mapper.ComplaintMapper;
import com.dms.survey.mapper.SurveyMapper;
import com.dms.warranty.entity.WarrantyClaim;
import com.dms.warranty.mapper.WarrantyClaimMapper;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.mapper.WorkOrderMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 经营日报：按经销商按天汇总，UNIQUE(report_date,dealer_code) 保证幂等。 */
@Service
@RequiredArgsConstructor
public class DailyReportService {
    private static final Logger log = LoggerFactory.getLogger(DailyReportService.class);

    private final DailyReportMapper mapper;
    private final DealerMapper dealerMapper;
    private final WorkOrderMapper orderMapper;
    private final VehicleSalesOrderMapper salesMapper;
    private final StockMovementMapper movementMapper;
    private final PartStockService stockService;
    private final SurveyMapper surveyMapper;
    private final ComplaintMapper complaintMapper;
    private final WarrantyClaimMapper claimMapper;
    private final PurchaseOrderMapper poMapper;
    private final FollowTaskMapper taskMapper;

    /** 每晚 23:30 生成当日全部经销商日报。 */
    @Scheduled(cron = "${dms.report.daily-cron:0 30 23 * * ?}")
    public void daily() {
        int n = generate(LocalDate.now(), null).size();
        log.info("经营日报生成 {} 条", n);
    }

    /** 生成某日日报；dealerCode 空 = 全部经销商（受 DataScope 限制）。返回生成/更新的行。 */
    @Transactional
    public List<DailyReport> generate(LocalDate date, String dealerCode) {
        String scoped = DataScope.effectiveDealer(dealerCode);
        LocalDate d = date == null ? LocalDate.now() : date;
        List<String> dealers = new ArrayList<>();
        if (scoped != null) {
            dealers.add(scoped);
        } else {
            for (Dealer x : dealerMapper.selectList(null)) {
                dealers.add(x.getCode());
            }
        }
        List<DailyReport> out = new ArrayList<>();
        for (String dc : dealers) {
            out.add(generateOne(d, dc));
        }
        return out;
    }

    private DailyReport generateOne(LocalDate d, String dealerCode) {
        LocalDateTime from = d.atStartOfDay();
        LocalDateTime to = d.plusDays(1).atStartOfDay();
        DailyReport r = new DailyReport();
        r.setReportDate(d);
        r.setDealerCode(dealerCode);
        Dealer dealer = dealerMapper.selectOne(new QueryWrapper<Dealer>().eq("code", dealerCode));
        r.setDealerName(dealer == null ? dealerCode : dealer.getName());

        QueryWrapper<WorkOrder> oq = new QueryWrapper<WorkOrder>().eq("dealer_code", dealerCode);
        int checkIns = 0, delivered = 0, settled = 0;
        BigDecimal revenue = BigDecimal.ZERO, labor = BigDecimal.ZERO, parts = BigDecimal.ZERO;
        for (WorkOrder o : orderMapper.selectList(oq)) {
            if (inRange(o.getCheckInTime(), from, to)) checkIns++;
            if (inRange(o.getDeliverTime(), from, to)) delivered++;
            if (inRange(o.getSettleTime(), from, to)) {
                settled++;
                revenue = revenue.add(nz(o.getTotalAmount()));
                labor = labor.add(nz(o.getLaborAmount()));
                parts = parts.add(nz(o.getPartsAmount()));
            }
        }
        r.setCheckIns(checkIns);
        r.setDelivered(delivered);
        r.setSettledOrders(settled);
        r.setRevenue(revenue);
        r.setLaborAmount(labor);
        r.setPartsAmount(parts);

        int salesOrders = 0, salesDelivered = 0;
        BigDecimal salesAmount = BigDecimal.ZERO;
        QueryWrapper<VehicleSalesOrder> sq =
                new QueryWrapper<VehicleSalesOrder>().eq("dealer_code", dealerCode);
        for (VehicleSalesOrder o : salesMapper.selectList(sq)) {
            if (inRange(o.getCreatedAt(), from, to)) salesOrders++;
            if ("DELIVERED".equals(o.getStatus()) && inRange(o.getDeliveredAt(), from, to)) {
                salesDelivered++;
                salesAmount = salesAmount.add(nz(o.getPrice()));
            }
        }
        r.setSalesOrders(salesOrders);
        r.setSalesDelivered(salesDelivered);
        r.setSalesAmount(salesAmount);

        int in = 0, outQty = 0;
        QueryWrapper<StockMovement> mq =
                new QueryWrapper<StockMovement>()
                        .eq("dealer_code", dealerCode)
                        .ge("created_at", from)
                        .lt("created_at", to);
        for (StockMovement m : movementMapper.selectList(mq)) {
            if ("IN".equals(m.getType())) in += m.getQty() == null ? 0 : m.getQty();
            if ("OUT".equals(m.getType())) outQty += m.getQty() == null ? 0 : m.getQty();
        }
        r.setPartsIn(in);
        r.setPartsOut(outQty);
        r.setShortageCount(
                (int) stockService.shortage().stream()
                        .filter(x -> dealerCode.equals(x.get("dealerCode")))
                        .count());

        int surveys = 0, promoters = 0, detractors = 0;
        QueryWrapper<Survey> svq =
                new QueryWrapper<Survey>().eq("dealer_code", dealerCode).eq("status", "ANSWERED");
        for (Survey sv : surveyMapper.selectList(svq)) {
            if (inRange(sv.getAnsweredTime(), from, to)) {
                surveys++;
                if (sv.getNpsScore() != null) {
                    if (sv.getNpsScore().intValue() >= 9) promoters++;
                    if (sv.getNpsScore().intValue() <= 6) detractors++;
                }
            }
        }
        r.setSurveys(surveys);
        r.setNps(
                surveys == 0
                        ? BigDecimal.ZERO
                        : new BigDecimal((promoters - detractors) * 100.0 / surveys)
                                .setScale(1, RoundingMode.HALF_UP));

        QueryWrapper<Complaint> cq =
                new QueryWrapper<Complaint>().eq("dealer_code", dealerCode);
        int complaints = 0;
        for (Complaint c : complaintMapper.selectList(cq)) {
            if (inRange(c.getCreatedAt(), from, to)) complaints++;
        }
        r.setComplaints(complaints);

        int claims = 0;
        BigDecimal claimAmount = BigDecimal.ZERO;
        QueryWrapper<WarrantyClaim> wq =
                new QueryWrapper<WarrantyClaim>().eq("dealer_code", dealerCode);
        for (WarrantyClaim c : claimMapper.selectList(wq)) {
            if (inRange(c.getSubmittedAt(), from, to)) {
                claims++;
                claimAmount = claimAmount.add(nz(c.getAmount()));
            }
        }
        r.setClaims(claims);
        r.setClaimAmount(claimAmount);

        int poCount = 0;
        BigDecimal poAmount = BigDecimal.ZERO;
        QueryWrapper<PurchaseOrder> pq =
                new QueryWrapper<PurchaseOrder>().eq("dealer_code", dealerCode);
        for (PurchaseOrder o : poMapper.selectList(pq)) {
            if (inRange(o.getCreatedAt(), from, to)) {
                poCount++;
                poAmount = poAmount.add(nz(o.getTotalAmount()));
            }
        }
        r.setPoCount(poCount);
        r.setPoAmount(poAmount);

        r.setPendingTasks(
                taskMapper
                        .selectCount(
                                new QueryWrapper<FollowTask>()
                                        .eq("dealer_code", dealerCode)
                                        .eq("status", "PENDING"))
                        .intValue());
        r.setGeneratedAt(LocalDateTime.now());

        DailyReport existing =
                mapper.selectOne(
                        new QueryWrapper<DailyReport>()
                                .eq("report_date", d)
                                .eq("dealer_code", dealerCode));
        if (existing != null) {
            r.setId(existing.getId());
            mapper.updateById(r);
        } else {
            mapper.insert(r);
        }
        return r;
    }

    /** 日报查询：单天或区间（受数据范围限制）。 */
    public List<DailyReport> list(LocalDate from, LocalDate to, String dealerCode) {
        String scoped = DataScope.effectiveDealer(dealerCode);
        QueryWrapper<DailyReport> q = new QueryWrapper<>();
        if (scoped != null) q.eq("dealer_code", scoped);
        if (from != null) q.ge("report_date", from);
        if (to != null) q.le("report_date", to);
        return mapper.selectList(q.orderByDesc("report_date").orderByAsc("dealer_code"));
    }

    private static boolean inRange(LocalDateTime t, LocalDateTime from, LocalDateTime to) {
        return t != null && !t.isBefore(from) && t.isBefore(to);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
