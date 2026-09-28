package com.dms.report.controller;

import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.report.entity.DailyReport;
import com.dms.report.entity.ReportTable;
import com.dms.report.service.DailyReportService;
import com.dms.report.service.ReportExporter;
import com.dms.report.service.ReportService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 经营报表：多维统计 JSON + Excel/PDF 导出 + 经营日报。 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private static final Set<String> TYPES =
            new HashSet<>(
                    Arrays.asList("workshop", "sales", "parts", "satisfaction", "warranty", "procure"));

    private final ReportService reportService;
    private final DailyReportService dailyService;
    private final ReportExporter exporter;

    @GetMapping("/{type}")
    public R<ReportTable> report(
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String dealerCode,
            @RequestParam(defaultValue = "day") String groupBy) {
        check(type, from, to);
        return R.ok(reportService.report(type, from, to, dealerCode, groupBy));
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String dealerCode,
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(defaultValue = "xlsx") String format) {
        check(type, from, to);
        ReportTable table = reportService.report(type, from, to, dealerCode, groupBy);
        String title = table.getTitle() == null ? type : table.getTitle();
        String filename = "报表_" + type + "_" + from + "_" + to;
        byte[] body;
        MediaType ct;
        if ("pdf".equalsIgnoreCase(format)) {
            body = exporter.toPdf(table, title);
            ct = MediaType.APPLICATION_PDF;
            filename += ".pdf";
        } else {
            body = exporter.toExcel(table, title);
            ct = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            filename += ".xlsx";
        }
        return ResponseEntity.ok()
                .contentType(ct)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; " + rfc5987(filename))
                .body(body);
    }

    private void check(String type, LocalDate from, LocalDate to) {
        if (!TYPES.contains(type)) {
            throw new BizException("未知报表类型: " + type);
        }
        if (from == null || to == null || to.isBefore(from) || to.toEpochDay() - from.toEpochDay() > 366) {
            throw new BizException("时间范围非法（最长366天）");
        }
    }

    private static String rfc5987(String filename) {
        return "filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // ============ 经营日报 ============

    @GetMapping("/daily")
    public R<List<DailyReport>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String dealerCode) {
        LocalDate d = date == null ? LocalDate.now() : date;
        return R.ok(dailyService.list(d, d, dealerCode));
    }

    @GetMapping("/daily/page")
    public R<List<DailyReport>> dailyPage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String dealerCode) {
        return R.ok(dailyService.list(from, to, dealerCode));
    }

    @PostMapping("/daily/generate")
    public R<List<DailyReport>> dailyGenerate(@RequestBody(required = false) Map<String, Object> body) {
        LocalDate d = body == null || body.get("date") == null
                ? LocalDate.now()
                : LocalDate.parse(String.valueOf(body.get("date")));
        String dealer = body == null ? null : (String) body.get("dealerCode");
        return R.ok(dailyService.generate(d, dealer));
    }

    @GetMapping("/daily/export")
    public ResponseEntity<byte[]> dailyExport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String dealerCode) {
        LocalDate d = date == null ? LocalDate.now() : date;
        List<DailyReport> rows = dailyService.list(d, d, dealerCode);
        ReportTable t = new ReportTable();
        t.setTitle("经营日报 " + d);
        ReportTable.Section s = t.addSection("经营日报",
                Arrays.asList("经销商", "进厂", "交车", "结算单", "产值", "工时费", "材料费",
                        "销售订单", "销售交车", "销售额", "入库", "出库", "缺货",
                        "答卷", "NPS", "投诉", "索赔", "索赔金额", "采购单", "采购额", "待办任务"));
        for (DailyReport r : rows) {
            s.addRow(Arrays.asList(
                    r.getDealerName() == null ? r.getDealerCode() : r.getDealerName(),
                    r.getCheckIns(), r.getDelivered(), r.getSettledOrders(), r.getRevenue(),
                    r.getLaborAmount(), r.getPartsAmount(), r.getSalesOrders(), r.getSalesDelivered(),
                    r.getSalesAmount(), r.getPartsIn(), r.getPartsOut(), r.getShortageCount(),
                    r.getSurveys(), r.getNps(), r.getComplaints(), r.getClaims(), r.getClaimAmount(),
                    r.getPoCount(), r.getPoAmount(), r.getPendingTasks()));
        }
        byte[] body;
        MediaType ct;
        String filename = "经营日报_" + d;
        if ("pdf".equalsIgnoreCase(format)) {
            body = exporter.toPdf(t, t.getTitle());
            ct = MediaType.APPLICATION_PDF;
            filename += ".pdf";
        } else {
            body = exporter.toExcel(t, t.getTitle());
            ct = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            filename += ".xlsx";
        }
        return ResponseEntity.ok()
                .contentType(ct)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; " + rfc5987(filename))
                .body(body);
    }
}
