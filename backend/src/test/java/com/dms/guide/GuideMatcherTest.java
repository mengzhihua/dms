package com.dms.guide;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.guide.entity.RepairGuide;
import com.dms.guide.service.GuideMatcher;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class GuideMatcherTest {
    private RepairGuide g(String models, String dtcs, String symptoms) {
        RepairGuide g = new RepairGuide();
        g.setModelCodes(models);
        g.setDtcCodes(dtcs);
        g.setSymptoms(symptoms);
        return g;
    }

    @Test
    void scoring() {
        RepairGuide g = g("M001,M002", "P0300,P0301", "抖动,故障灯");
        // dtc P0300 hit(+5), model M001(+3), symptom "抖动"(+2) = 10
        assertEquals(
                10,
                GuideMatcher.score(g, "M001", Arrays.asList("P0300"), "发动机抖动严重"));
        // 2 dtc hits = 10 + model 3 + symptom 2 = 15
        assertEquals(
                15,
                GuideMatcher.score(g, "M001", Arrays.asList("P0300", "P0301"), "抖动"));
        // no match
        assertEquals(0, GuideMatcher.score(g, "M009", Collections.singletonList("P9999"), "正常"));
        // ALL model match
        RepairGuide all = g("ALL", null, "保养");
        assertEquals(5, GuideMatcher.score(all, "M005", null, "保养到期"));
    }
}
