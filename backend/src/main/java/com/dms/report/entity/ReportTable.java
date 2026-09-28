package com.dms.report.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 通用报表结构：多个小节 + 汇总指标，供 Excel/PDF 导出复用。 */
public class ReportTable {
    private String title;
    private List<Section> sections = new ArrayList<>();
    private Map<String, Object> summary = new LinkedHashMap<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Section> getSections() {
        return sections;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public Section addSection(String title, List<String> headers) {
        Section s = new Section();
        s.title = title;
        s.headers = headers;
        sections.add(s);
        return s;
    }

    public static class Section {
        private String title;
        private List<String> headers;
        private List<List<Object>> rows = new ArrayList<>();

        public String getTitle() {
            return title;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<List<Object>> getRows() {
            return rows;
        }

        public void addRow(List<Object> row) {
            rows.add(row);
        }
    }
}
