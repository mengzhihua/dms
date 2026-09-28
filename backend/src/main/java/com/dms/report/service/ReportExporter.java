package com.dms.report.service;

import com.dms.report.entity.ReportTable;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 报表导出：Excel（POI XSSF）与 PDF（OpenPDF），中文字体按配置/常见路径回退。 */
@Component
public class ReportExporter {
    private static final Logger log = LoggerFactory.getLogger(ReportExporter.class);
    private final String pdfFontPath;

    public ReportExporter(@Value("${dms.report.pdf-font:}") String pdfFontPath) {
        this.pdfFontPath = pdfFontPath;
    }

    public byte[] toExcel(ReportTable report, String title) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font hf = wb.createFont();
            hf.setBold(true);
            headerStyle.setFont(hf);
            int idx = 0;
            for (ReportTable.Section sec : report.getSections()) {
                Sheet sheet =
                        wb.createSheet(
                                sec.getTitle() == null || sec.getTitle().isEmpty()
                                        ? "Sheet" + (++idx)
                                        : sec.getTitle());
                int rowIdx = 0;
                org.apache.poi.ss.usermodel.Row header = sheet.createRow(rowIdx++);
                for (int i = 0; i < sec.getHeaders().size(); i++) {
                    header.createCell(i).setCellValue(sec.getHeaders().get(i));
                    header.getCell(i).setCellStyle(headerStyle);
                }
                for (List<Object> row : sec.getRows()) {
                    org.apache.poi.ss.usermodel.Row r = sheet.createRow(rowIdx++);
                    for (int i = 0; i < row.size(); i++) {
                        Object v = row.get(i);
                        if (v instanceof Number) {
                            r.createCell(i).setCellValue(((Number) v).doubleValue());
                        } else {
                            r.createCell(i).setCellValue(v == null ? "" : String.valueOf(v));
                        }
                    }
                }
                for (int i = 0; i < sec.getHeaders().size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel 导出失败: " + e.getMessage(), e);
        }
    }

    public byte[] toPdf(ReportTable report, String title) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font font = pdfFont();
            Font bold = new Font(font.getBaseFont(), 12, Font.BOLD);
            doc.add(new Paragraph(title, bold));
            doc.add(new Paragraph(
                    "生成时间 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    font));
            for (ReportTable.Section sec : report.getSections()) {
                if (sec.getTitle() != null) {
                    doc.add(new Paragraph(sec.getTitle(), bold));
                }
                PdfPTable table = new PdfPTable(sec.getHeaders().size());
                table.setWidthPercentage(100);
                for (String h : sec.getHeaders()) {
                    table.addCell(new PdfPCell(new Paragraph(h, font)));
                }
                for (List<Object> row : sec.getRows()) {
                    for (Object v : row) {
                        table.addCell(new PdfPCell(new Paragraph(v == null ? "" : String.valueOf(v), font)));
                    }
                }
                doc.add(table);
            }
            if (report.getSummary() != null && !report.getSummary().isEmpty()) {
                StringBuilder sb = new StringBuilder("汇总：");
                report.getSummary().forEach((k, v) -> sb.append(k).append("=").append(v).append("  "));
                doc.add(new Paragraph(sb.toString(), font));
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 导出失败: " + e.getMessage(), e);
        }
    }

    /** 中文字体解析：配置路径 → 常见系统路径 → STSong 内置 → Helvetica。 */
    private Font pdfFont() {
        String[] candidates = {
            pdfFontPath,
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
            "C:/Windows/Fonts/simsun.ttc,0",
            "/System/Library/Fonts/PingFang.ttc,0"
        };
        for (String c : candidates) {
            if (c == null || c.isEmpty()) continue;
            try {
                BaseFont bf = BaseFont.createFont(c, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(bf, 9);
            } catch (Exception ignored) {
            }
        }
        try {
            return new Font(BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED), 9);
        } catch (Exception e) {
            log.warn("PDF 中文字体不可用，中文可能显示为空白: {}", e.getMessage());
            return new Font(Font.HELVETICA, 9);
        }
    }
}
