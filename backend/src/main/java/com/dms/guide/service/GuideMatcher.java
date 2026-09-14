package com.dms.guide.service;

import com.dms.guide.entity.RepairGuide;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 纯逻辑：维修指导匹配打分。DTC 每个 +5，车型匹配 +3，症状关键词每个 +2。 */
public final class GuideMatcher {
    private GuideMatcher() {}

    public static int score(
            RepairGuide g, String modelCode, List<String> dtcCodes, String symptom) {
        int score = 0;
        if (dtcCodes != null && g.getDtcCodes() != null) {
            Set<String> guideDtc = split(g.getDtcCodes());
            for (String d : dtcCodes) {
                if (d != null && guideDtc.contains(d.trim().toUpperCase())) {
                    score += 5;
                }
            }
        }
        if (modelCode != null && g.getModelCodes() != null) {
            Set<String> models = split(g.getModelCodes());
            if (models.contains("ALL") || models.contains(modelCode.trim().toUpperCase())) {
                score += 3;
            }
        }
        if (symptom != null && g.getSymptoms() != null) {
            for (String kw : g.getSymptoms().split("[,，、]")) {
                String k = kw.trim();
                if (!k.isEmpty() && symptom.contains(k)) {
                    score += 2;
                }
            }
        }
        return score;
    }

    public static Set<String> split(String csv) {
        Set<String> s = new HashSet<>();
        if (csv != null) {
            for (String x : csv.split("[,，]")) {
                if (!x.trim().isEmpty()) {
                    s.add(x.trim().toUpperCase());
                }
            }
        }
        return s;
    }

    public static List<String> splitList(String csv) {
        return Arrays.asList(csv == null || csv.isEmpty() ? new String[0] : csv.split("[,，]"));
    }
}
