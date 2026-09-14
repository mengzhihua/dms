package com.dms.survey;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.survey.entity.SurveyQuestion;
import com.dms.survey.service.SurveyScorer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurveyScorerTest {
    private SurveyQuestion q(long id, String type, String weight) {
        SurveyQuestion q = new SurveyQuestion();
        q.setId(id);
        q.setType(type);
        q.setWeight(new BigDecimal(weight));
        return q;
    }

    @Test
    void weightedTotal() {
        List<SurveyQuestion> qs =
                Arrays.asList(q(1, "SCORE", "0.3"), q(2, "SCORE", "0.2"), q(3, "SCORE", "0.5"), q(4, "NPS", "1"));
        Map<Long, BigDecimal> ans = new HashMap<>();
        ans.put(1L, new BigDecimal("8"));
        ans.put(2L, new BigDecimal("10"));
        ans.put(3L, new BigDecimal("4"));
        ans.put(4L, new BigDecimal("9"));
        // (8*.3+10*.2+4*.5)=6.4 → 6.4*10=64
        assertEquals(new BigDecimal("64.00"), SurveyScorer.totalScore(qs, ans));
        assertEquals(new BigDecimal("9"), SurveyScorer.npsScore(qs, ans));
    }

    @Test
    void npsCalculation() {
        List<BigDecimal> scores =
                Arrays.asList(
                        new BigDecimal("10"), new BigDecimal("9"), new BigDecimal("7"),
                        new BigDecimal("5"), new BigDecimal("2"));
        // promoters 2/5=40%, detractors 2/5=40% → NPS 0
        assertEquals(new BigDecimal("0.00"), SurveyScorer.nps(scores));
    }

    @Test
    void lowScoreTriggersComplaintRule() {
        // totalScore<60 or any score<=3 → complaint; verify scorer emits <60 for low answers
        List<SurveyQuestion> qs = Arrays.asList(q(1, "SCORE", "1"), q(2, "NPS", "1"));
        Map<Long, BigDecimal> ans = new HashMap<>();
        ans.put(1L, new BigDecimal("2"));
        ans.put(2L, new BigDecimal("3"));
        assertTrue(SurveyScorer.totalScore(qs, ans).compareTo(new BigDecimal("60")) < 0);
    }
}
