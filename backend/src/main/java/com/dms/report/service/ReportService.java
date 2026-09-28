package com.dms.report.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.crm.entity.FollowTask;
import com.dms.crm.mapper.FollowTaskMapper;
import com.dms.network.entity.Dealer;
import com.dms.network.entity.VehicleSalesOrder;
import com.dms.network.mapper.DealerMapper;
import com.dms.network.mapper.VehicleSalesOrderMapper;
import com.dms.parts.entity.PartStock;
import com.dms.parts.entity.StockMovement;
import com.dms.parts.mapper.PartMapper;
import com.dms.parts.mapper.PartStockMapper;
import com.dms.parts.mapper.StockMovementMapper;
import com.dms.parts.service.PartStockService;
import com.dms.procure.entity.PurchaseOrder;
import com.dms.procure.entity.PurchaseStatement;
import com.dms.procure.mapper.PurchaseOrderMapper;
import com.dms.procure.mapper.PurchaseStatementMapper;
import com.dms.report.entity.ReportTable;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 多维经营报表：按 日/月/经销商 分组聚合，输出通用 ReportTable。 */
@Service
@RequiredArgsConstructor
public class ReportService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final WorkOrderMapper orderMapper;
    private final VehicleSalesOrderMapper salesMapper;
    private final StockMovementMapper movementMapper;
    private final PartStockMapper stockMapper;
    private final PartMapper partMapper;
    private final PartStockService stockService;
    private final SurveyMapper surveyMapper;
    private final ComplaintMapper complaintMapper;
    private final WarrantyClaimMapper claimMapper;
    private final PurchaseOrderMapper poMapper;
    private final PurchaseStatementMapper psMapper;
    private final FollowTaskMapper taskMapper;
    private final DealerMapper dealerMapper;

    public ReportTable report(String type, LocalDate from, LocalDate to, String dealerCode, String groupBy) {
        String dealer = DataScope.effectiveDealer(dealerCode);
        String g = groupBy == null ? "day" : groupBy;
        if (!Arrays.asList("day", "month", "dealer").contains(g)) {
            throw new BizException("groupBy 仅支持 day|month|dealer");
        }
        switch (type) {
            case "workshop": return workshop(from, to, dealer, g);
            case "sales": return sales(from, to, dealer, g);
            case "parts": return parts(from, to, dealer, g);
            case "satisfaction": return satisfaction(from, to, dealer, g);
            case "warranty": return warranty(from, to, dealer, g);
            case "procure": return procure(from, to, dealer, g);
            default: throw new BizException("未知报表类型: " + type);
        }
    }

    private String key(String groupBy, String dealerCode, LocalDateTime t, String prefix) {
        if (t == null) return null;
        switch (groupBy) {
            case "day": return t.format(DAY);
            case "month": return t.format(MONTH);
            default: return dealerCode + " " + dealerName(dealerCode);
        }
    }

    private String dealerName(String code) {
        if (code == null) return "";
        Dealer d = dealerMapper.selectOne(new QueryWrapper<Dealer>().eq("code", code));
        return d == null ? code : d.getName();
    }

    private static BigDecimal bd(Object v) {
        return v == null ? BigDecimal.ZERO : (BigDecimal) v;
    }

    private static class Agg {
        int c1; int c2; int c3;
        BigDecimal a1 = BigDecimal.ZERO, a2 = BigDecimal.ZERO, a3 = BigDecimal.ZERO,
                a4 = BigDecimal.ZERO, a5 = BigDecimal.ZERO, a6 = BigDecimal.ZERO;
        double d1; long n1;
    }

    private ReportTable workshop(LocalDate from, LocalDate to, String dealer, String g) {
        ReportTable t = new ReportTable();
        t.setTitle("售后经营报表");
        ReportTable.Section s = t.addSection("售后经营",
                Arrays.asList("分组", "工单数", "交车数", "结算单数", "结算金额", "工时费", "材料费",
                        "保修金额", "客户实付", "平均维修小时", "单均产值"));
        Map<String, Agg> map = new TreeMap<>();
        QueryWrapper<WorkOrder> q = new QueryWrapper<>();
        if (dealer != null) q.eq("dealer_code", dealer);
        for (WorkOrder o : orderMapper.selectList(q)) {
            if (in(o.getCheckInTime(), from, to)) {
                agg(map, g, o.getDealerCode(), o.getCheckInTime()).c1++;
            }
            if (in(o.getDeliverTime(), from, to)) {
                agg(map, g, o.getDealerCode(), o.getDeliverTime()).c2++;
            }
            if (in(o.getSettleTime(), from, to)) {
                Agg a = agg(map, g, o.getDealerCode(), o.getSettleTime());
                a.c3++;
                a.a1 = a.a1.add(bd(o.getTotalAmount()));
                a.a2 = a.a2.add(bd(o.getLaborAmount()));
                a.a3 = a.a3.add(bd(o.getPartsAmount()));
                a.a4 = a.a4.add(bd(o.getWarrantyAmount()));
                a.a5 = a.a5.add(bd(o.getCustomerPayable()));
            }
            if (in(o.getRepairStartTime(), from, to) && o.getRepairEndTime() != null) {
                Agg a = agg(map, g, o.getDealerCode(), o.getRepairStartTime());
                a.d1 += java.time.Duration.between(o.getRepairStartTime(), o.getRepairEndTime()).toMinutes() / 60.0;
                a.n1++;
            }
        }
        Agg total = new Agg();
        for (Map.Entry<String, Agg> e : map.entrySet()) {
            Agg a = e.getValue();
            s.addRow(Arrays.asList(e.getKey(), a.c1, a.c2, a.c3, a.a1, a.a2, a.a3, a.a4, a.a5,
                    a.n1 == 0 ? 0 : round(a.d1 / a.n1, 1),
                    a.c3 == 0 ? BigDecimal.ZERO : a.a1.divide(new BigDecimal(a.c3), 2, RoundingMode.HALF_UP)));
            merge(total, a);
        }
        t.getSummary().put("结算金额合计", total.a1);
        t.getSummary().put("工单数合计", total.c1);
        t.getSummary().put("交车数合计", total.c2);
        return t;
    }

    private ReportTable sales(LocalDate from, LocalDate to, String dealer, String g) {
        ReportTable t = new ReportTable();
        t.setTitle("整车销售报表");
        ReportTable.Section s = t.addSection("整车销售",
                Arrays.asList("分组", "订单数", "交车数", "销售额", "定金", "贷款金额", "金融渗透率",
                        "保险金额", "保险渗透率", "已收金额"));
        Map<String, Agg> map = new TreeMap<>();
        QueryWrapper<VehicleSalesOrder> q = new QueryWrapper<>();
        if (dealer != null) q.eq("dealer_code", dealer);
        for (VehicleSalesOrder o : salesMapper.selectList(q)) {
            if (in(o.getCreatedAt(), from, to)) {
                agg(map, g, o.getDealerCode(), o.getCreatedAt()).c1++;
            }
            boolean delivered = "DELIVERED".equals(o.getStatus()) && in(o.getDeliveredAt(), from, to);
            if (delivered) {
                Agg a = agg(map, g, o.getDealerCode(), o.getDeliveredAt());
                a.c2++;
                a.a1 = a.a1.add(bd(o.getPrice()));
                a.a2 = a.a2.add(bd(o.getDeposit()));
                a.a5 = a.a5.add(bd(o.getPaidAmount()));
                if ("LOAN".equals(o.getPaymentType())) {
                    a.c3++;
                    a.a3 = a.a3.add(bd(o.getLoanAmount()));
                }
                if ("ISSUED".equals(o.getInsuranceStatus())) {
                    a.n1++;
                    a.a4 = a.a4.add(bd(o.getInsuranceAmount()));
                }
            }
        }
        Agg total = new Agg();
        for (Map.Entry<String, Agg> e : map.entrySet()) {
            Agg a = e.getValue();
            s.addRow(Arrays.asList(e.getKey(), a.c1, a.c2, a.a1, a.a2, a.a3,
                    pct(a.c3, a.c2), a.a4, pct(a.n1, a.c2), a.a5));
            merge(total, a);
        }
        t.getSummary().put("销售额合计", total.a1);
        t.getSummary().put("交车数合计", total.c2);
        return t;
    }

    private ReportTable parts(LocalDate from, LocalDate to, String dealer, String g) {
        ReportTable t = new ReportTable();
        t.setTitle("备件报表");
        ReportTable.Section s = t.addSection("出入库流水",
                Arrays.asList("分组", "入库数量", "出库数量", "调整数量", "预留数", "释放数"));
        Map<String, Agg> map = new TreeMap<>();
        Map<String, BigDecimal> outQty = new TreeMap<>();
        Map<String, String> outName = new java.util.HashMap<>();
        QueryWrapper<StockMovement> q = new QueryWrapper<>();
        if (dealer != null) q.eq("dealer_code", dealer);
        q.ge("created_at", from.atStartOfDay()).lt("created_at", to.plusDays(1).atStartOfDay());
        for (StockMovement m : movementMapper.selectList(q)) {
            Agg a = agg(map, g, m.getDealerCode(), m.getCreatedAt());
            switch (m.getType() == null ? "" : m.getType()) {
                case "IN": a.c1 += m.getQty(); break;
                case "OUT": a.c2 += m.getQty();
                    outQty.merge(m.getPartNo(), new BigDecimal(m.getQty()), BigDecimal::add);
                    if (!outName.containsKey(m.getPartNo())) {
                        com.dms.parts.entity.Part p = partMapper.selectOne(
                                new QueryWrapper<com.dms.parts.entity.Part>().eq("part_no", m.getPartNo()));
                        outName.put(m.getPartNo(), p == null ? m.getPartNo() : p.getName());
                    }
                    break;
                case "ADJUST": a.c3 += m.getQty(); break;
                case "RESERVE": a.n1 += m.getQty(); break;
                case "RELEASE": a.d1 += m.getQty(); break;
                default: break;
            }
        }
        for (Map.Entry<String, Agg> e : map.entrySet()) {
            Agg a = e.getValue();
            s.addRow(Arrays.asList(e.getKey(), a.c1, a.c2, a.c3, a.n1, (long) a.d1));
        }
        // top-10 出库备件
        ReportTable.Section top = t.addSection("出库 TOP10", Arrays.asList("件号", "名称", "出库数量"));
        outQty.entrySet().stream()
                .sorted((x, y) -> y.getValue().compareTo(x.getValue()))
                .limit(10)
                .forEach(e -> top.addRow(Arrays.asList(e.getKey(), outName.get(e.getKey()), e.getValue())));
        // 汇总：当前库存金额 + 缺货项
        BigDecimal stockValue = BigDecimal.ZERO;
        QueryWrapper<PartStock> sq = new QueryWrapper<>();
        if (dealer != null) sq.eq("dealer_code", dealer);
        List<PartStock> stocks = stockMapper.selectList(sq);
        for (PartStock ps : stocks) {
            com.dms.parts.entity.Part p = partMapper.selectOne(
                    new QueryWrapper<com.dms.parts.entity.Part>().eq("part_no", ps.getPartNo()));
            if (p != null && p.getCostPrice() != null) {
                stockValue = stockValue.add(p.getCostPrice().multiply(new BigDecimal(ps.getQty())));
            }
        }
        t.getSummary().put("当前库存金额", stockValue);
        long shortage = dealer == null ? stockService.shortage().size()
                : stockService.shortage().stream().filter(m -> dealer.equals(m.get("dealerCode"))).count();
        t.getSummary().put("缺货项数", shortage);
        return t;
    }

    private ReportTable satisfaction(LocalDate from, LocalDate to, String dealer, String g) {
        ReportTable t = new ReportTable();
        t.setTitle("满意度报表");
        ReportTable.Section s = t.addSection("满意度",
                Arrays.asList("分组", "答卷数", "NPS", "平均CSI", "投诉数", "投诉关闭数", "平均关闭时长h"));
        Map<String, Agg> map = new TreeMap<>();
        QueryWrapper<Survey> q = new QueryWrapper<Survey>().eq("status", "ANSWERED");
        if (dealer != null) q.eq("dealer_code", dealer);
        for (Survey sv : surveyMapper.selectList(q)) {
            if (!in(sv.getAnsweredTime(), from, to)) continue;
            Agg a = agg(map, g, sv.getDealerCode(), sv.getAnsweredTime());
            a.c1++;
            if (sv.getNpsScore() != null) {
                if (sv.getNpsScore().intValue() >= 9) a.c2++;
                if (sv.getNpsScore().intValue() <= 6) a.c3++;
            }
            if (sv.getTotalScore() != null) {
                a.d1 += sv.getTotalScore().doubleValue();
                a.n1++;
            }
        }
        QueryWrapper<Complaint> cq = new QueryWrapper<>();
        if (dealer != null) cq.eq("dealer_code", dealer);
        Map<String, Agg> cmap = new TreeMap<>();
        for (Complaint c : complaintMapper.selectList(cq)) {
            if (in(c.getCreatedAt(), from, to)) {
                Agg a = agg(cmap, g, c.getDealerCode(), c.getCreatedAt());
                a.c1++;
                if ("CLOSED".equals(c.getStatus())) {
                    a.c2++;
                    a.d1 += java.time.Duration.between(c.getCreatedAt(), c.getUpdatedAt()).toHours();
                    a.n1++;
                }
            }
        }
        List<String> keys = new ArrayList<>(map.keySet());
        for (String k : cmap.keySet()) if (!map.containsKey(k)) keys.add(k);
        java.util.Collections.sort(keys);
        Agg total = new Agg();
        for (String k : keys) {
            Agg a = map.getOrDefault(k, new Agg());
            Agg c = cmap.getOrDefault(k, new Agg());
            s.addRow(Arrays.asList(k, a.c1,
                    a.c1 == 0 ? BigDecimal.ZERO : round((a.c2 - a.c3) * 100.0 / a.c1, 1),
                    a.n1 == 0 ? BigDecimal.ZERO : round(a.d1 / a.n1, 1),
                    c.c1, c.c2, c.n1 == 0 ? BigDecimal.ZERO : round(c.d1 / c.n1, 1)));
            merge(total, a);
        }
        t.getSummary().put("答卷数合计", total.c1);
        return t;
    }

    private ReportTable warranty(LocalDate from, LocalDate to, String dealer, String g) {
        ReportTable t = new ReportTable();
        t.setTitle("保修索赔报表");
        ReportTable.Section s = t.addSection("保修索赔",
                Arrays.asList("分组", "提交数", "审核通过数", "申请金额", "核准金额", "核准率", "已付金额"));
        Map<String, Agg> map = new TreeMap<>();
        QueryWrapper<WarrantyClaim> q = new QueryWrapper<>();
        if (dealer != null) q.eq("dealer_code", dealer);
        for (WarrantyClaim c : claimMapper.selectList(q)) {
            if (!in(c.getSubmittedAt(), from, to)) continue;
            Agg a = agg(map, g, c.getDealerCode(), c.getSubmittedAt());
            a.c1++;
            a.a1 = a.a1.add(bd(c.getAmount()));
            if (Arrays.asList("APPROVED", "SETTLED", "PAID", "PARTS_RETURNING", "PARTS_SHIPPED")
                    .contains(c.getStatus())) {
                a.c2++;
                BigDecimal appr = c.getApprovedAmount() != null ? c.getApprovedAmount() : bd(c.getAmount());
                a.a2 = a.a2.add(appr);
            }
            if ("PAID".equals(c.getStatus())) {
                a.a3 = a.a3.add(c.getApprovedAmount() != null ? c.getApprovedAmount() : bd(c.getAmount()));
            }
        }
        Agg total = new Agg();
        for (Map.Entry<String, Agg> e : map.entrySet()) {
            Agg a = e.getValue();
            s.addRow(Arrays.asList(e.getKey(), a.c1, a.c2, a.a1, a.a2,
                    pct(a.c2, a.c1), a.a3));
            merge(total, a);
        }
        t.getSummary().put("申请金额合计", total.a1);
        t.getSummary().put("核准金额合计", total.a2);
        return t;
    }

    private ReportTable procure(LocalDate from, LocalDate to, String dealer, String g) {
        ReportTable t = new ReportTable();
        t.setTitle("备件采购报表");
        ReportTable.Section s = t.addSection("采购订单",
                Arrays.asList("分组", "订单数", "订单金额", "到货金额", "完成数", "对账付款金额"));
        Map<String, Agg> map = new TreeMap<>();
        QueryWrapper<PurchaseOrder> q = new QueryWrapper<>();
        if (dealer != null) q.eq("dealer_code", dealer);
        for (PurchaseOrder o : poMapper.selectList(q)) {
            if (!in(o.getCreatedAt(), from, to)) continue;
            Agg a = agg(map, g, o.getDealerCode(), o.getCreatedAt());
            a.c1++;
            a.a1 = a.a1.add(bd(o.getTotalAmount()));
            a.a2 = a.a2.add(bd(o.getReceivedAmount()));
            if (Arrays.asList("RECEIVED", "CLOSED").contains(o.getStatus())) a.c2++;
        }
        QueryWrapper<PurchaseStatement> pq = new QueryWrapper<PurchaseStatement>().eq("status", "PAID");
        if (dealer != null) pq.eq("dealer_code", dealer);
        for (PurchaseStatement p : psMapper.selectList(pq)) {
            if (in(p.getUpdatedAt(), from, to)) {
                Agg a = agg(map, g, p.getDealerCode(), p.getUpdatedAt());
                a.a3 = a.a3.add(bd(p.getTotalAmount()));
            }
        }
        Agg total = new Agg();
        for (Map.Entry<String, Agg> e : map.entrySet()) {
            Agg a = e.getValue();
            s.addRow(Arrays.asList(e.getKey(), a.c1, a.a1, a.a2, a.c2, a.a3));
            merge(total, a);
        }
        t.getSummary().put("订单金额合计", total.a1);
        t.getSummary().put("到货金额合计", total.a2);
        return t;
    }

    private Agg agg(Map<String, Agg> map, String g, String dealer, LocalDateTime t) {
        String k = key(g, dealer, t, null);
        return map.computeIfAbsent(k, x -> new Agg());
    }

    private static void merge(Agg into, Agg a) {
        into.c1 += a.c1; into.c2 += a.c2; into.c3 += a.c3;
        into.a1 = into.a1.add(a.a1); into.a2 = into.a2.add(a.a2); into.a3 = into.a3.add(a.a3);
    }

    private static boolean in(LocalDateTime t, LocalDate from, LocalDate to) {
        if (t == null) return false;
        LocalDate d = t.toLocalDate();
        return !d.isBefore(from) && !d.isAfter(to);
    }

    private static BigDecimal pct(long x, long total) {
        if (total == 0) return BigDecimal.ZERO;
        return new BigDecimal(x * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal round(double v, int scale) {
        return new BigDecimal(v).setScale(scale, RoundingMode.HALF_UP);
    }
}
