package com.dms.survey.service;

import com.dms.survey.entity.SurveyQuestion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/** 纯逻辑：满意度打分。totalScore = Σ(score/10*weight)/Σweight*100 (0-100)；npsScore 取 NPS 题得分。 */
public final class SurveyScorer {
    private SurveyScorer() {}

    public static BigDecimal totalScore(
            List<SurveyQuestion> questions, Map<Long, BigDecimal> answers) {
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        for (SurveyQuestion q : questions) {
            if (!"SCORE".equals(q.getType())) {
                continue;
            }
            BigDecimal w = q.getWeight() == null ? BigDecimal.ONE : q.getWeight();
            BigDecimal s = answers.get(q.getId());
            if (s == null) {
                continue;
            }
            weighted = weighted.add(s.multiply(w));
            weightSum = weightSum.add(w);
        }
        if (weightSum.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return weighted
                .divide(weightSum, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("10"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal npsScore(
            List<SurveyQuestion> questions, Map<Long, BigDecimal> answers) {
        for (SurveyQuestion q : questions) {
            if ("NPS".equals(q.getType())) {
                return answers.get(q.getId());
            }
        }
        return null;
    }

    /** NPS = %推荐者(9-10) - %贬损者(0-6)，返回 -100~100。 */
    public static BigDecimal nps(List<BigDecimal> npsAnswers) {
        if (npsAnswers == null || npsAnswers.isEmpty()) {
            return null;
        }
        long promoters = 0;
        long detractors = 0;
        for (BigDecimal s : npsAnswers) {
            if (s == null) {
                continue;
            }
            if (s.compareTo(new BigDecimal("9")) >= 0) {
                promoters++;
            } else if (s.compareTo(new BigDecimal("6")) <= 0) {
                detractors++;
            }
        }
        return new BigDecimal(promoters - detractors)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(npsAnswers.size()), 2, RoundingMode.HALF_UP);
    }
}
