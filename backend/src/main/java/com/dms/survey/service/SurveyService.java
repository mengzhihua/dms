package com.dms.survey.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BizException;
import com.dms.common.CodeGenerator;
import com.dms.survey.entity.*;
import com.dms.survey.mapper.*;
import com.dms.workshop.entity.WorkOrder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SurveyService {
    private final SurveyMapper surveyMapper;
    private final SurveyTemplateMapper templateMapper;
    private final SurveyQuestionMapper questionMapper;
    private final SurveyAnswerMapper answerMapper;
    private final ComplaintMapper complaintMapper;
    private final CodeGenerator codeGenerator;

    /** 交车后自动创建 SERVICE 类型调研。 */
    @Transactional
    public Long createForOrder(WorkOrder o) {
        SurveyTemplate t =
                templateMapper.selectOne(
                        new QueryWrapper<SurveyTemplate>().eq("type", "SERVICE").last("LIMIT 1"));
        Survey s = new Survey();
        s.setSurveyNo(codeGenerator.next("SV"));
        s.setTemplateCode(t == null ? null : t.getCode());
        s.setDealerCode(o.getDealerCode());
        s.setCustomerId(o.getCustomerId());
        s.setOrderId(o.getId());
        s.setChannel("SMS");
        s.setStatus("PENDING");
        s.setSentTime(LocalDateTime.now());
        surveyMapper.insert(s);
        return s.getId();
    }

    /** 整车交付后自动创建 SALES 类型调研（无 SALES 模板时回退 SERVICE）。 */
    @Transactional
    public Long createForSalesOrder(com.dms.network.entity.VehicleSalesOrder o) {
        SurveyTemplate t =
                templateMapper.selectOne(
                        new QueryWrapper<SurveyTemplate>().eq("type", "SALES").last("LIMIT 1"));
        if (t == null) {
            t =
                    templateMapper.selectOne(
                            new QueryWrapper<SurveyTemplate>()
                                    .eq("type", "SERVICE")
                                    .last("LIMIT 1"));
        }
        Survey s = new Survey();
        s.setSurveyNo(codeGenerator.next("SV"));
        s.setTemplateCode(t == null ? null : t.getCode());
        s.setDealerCode(o.getDealerCode());
        s.setCustomerId(o.getCustomerId());
        s.setSalesOrderId(o.getId());
        s.setChannel("SMS");
        s.setStatus("PENDING");
        s.setSentTime(LocalDateTime.now());
        surveyMapper.insert(s);
        return s.getId();
    }

    /** 提交答卷：计算加权总分(0-100)与NPS；总分<60或任一题<=3 自动生成投诉。 */
    @Transactional
    public Survey answer(Long surveyId, List<Map<String, Object>> answers) {
        Survey s = surveyMapper.selectById(surveyId);
        if (s == null) {
            throw new BizException("调研不存在");
        }
        DataScope.check(s.getDealerCode());
        if ("ANSWERED".equals(s.getStatus())) {
            throw new BizException("该调研已作答");
        }
        SurveyTemplate t =
                templateMapper.selectOne(
                        new QueryWrapper<SurveyTemplate>().eq("code", s.getTemplateCode()));
        List<SurveyQuestion> questions =
                t == null
                        ? new ArrayList<>()
                        : questionMapper.selectList(
                                new QueryWrapper<SurveyQuestion>()
                                        .eq("template_id", t.getId())
                                        .orderByAsc("seq"));
        Map<Long, BigDecimal> scoreMap = new HashMap<>();
        for (Map<String, Object> a : answers) {
            SurveyAnswer sa = new SurveyAnswer();
            sa.setSurveyId(surveyId);
            Long qid = ((Number) a.get("questionId")).longValue();
            sa.setQuestionId(qid);
            if (a.get("score") != null) {
                BigDecimal sc = new BigDecimal(a.get("score").toString());
                sa.setScore(sc);
                scoreMap.put(qid, sc);
            }
            sa.setText((String) a.get("text"));
            answerMapper.insert(sa);
        }
        BigDecimal total = SurveyScorer.totalScore(questions, scoreMap);
        s.setTotalScore(total);
        s.setNpsScore(SurveyScorer.npsScore(questions, scoreMap));
        s.setStatus("ANSWERED");
        s.setAnsweredTime(LocalDateTime.now());
        surveyMapper.updateById(s);

        boolean low = total.compareTo(new BigDecimal("60")) < 0;
        if (!low) {
            for (BigDecimal sc : scoreMap.values()) {
                if (sc.compareTo(new BigDecimal("3")) <= 0) {
                    low = true;
                    break;
                }
            }
        }
        if (low) {
            Complaint c = new Complaint();
            c.setComplaintNo(codeGenerator.next("CP"));
            c.setDealerCode(s.getDealerCode());
            c.setCustomerId(s.getCustomerId());
            c.setSurveyId(s.getId());
            c.setOrderId(s.getOrderId());
            c.setContent("低分调研自动生成：总分 " + total);
            c.setLevel(total.compareTo(new BigDecimal("40")) < 0 ? "HIGH" : "MEDIUM");
            c.setStatus("OPEN");
            complaintMapper.insert(c);
        }
        return s;
    }

    /** 统计：份数、平均分、NPS、分数分布。 */
    public Map<String, Object> stats(String dealerCode, String from, String to) {
        dealerCode = DataScope.effectiveDealer(dealerCode);
        QueryWrapper<Survey> q =
                new QueryWrapper<Survey>().eq("status", "ANSWERED");
        if (dealerCode != null && !dealerCode.isEmpty()) {
            q.eq("dealer_code", dealerCode);
        }
        if (from != null && !from.isEmpty()) {
            q.ge("answered_time", from);
        }
        if (to != null && !to.isEmpty()) {
            q.le("answered_time", to + " 23:59:59");
        }
        List<Survey> list = surveyMapper.selectList(q);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", list.size());
        BigDecimal sum = BigDecimal.ZERO;
        List<BigDecimal> npsAnswers = new ArrayList<>();
        Map<String, Long> dist = new LinkedHashMap<>();
        dist.put("0-20", 0L);
        dist.put("20-40", 0L);
        dist.put("40-60", 0L);
        dist.put("60-80", 0L);
        dist.put("80-100", 0L);
        for (Survey s : list) {
            if (s.getTotalScore() != null) {
                sum = sum.add(s.getTotalScore());
                double sc = s.getTotalScore().doubleValue();
                String bucket =
                        sc < 20
                                ? "0-20"
                                : sc < 40 ? "20-40" : sc < 60 ? "40-60" : sc < 80 ? "60-80" : "80-100";
                dist.put(bucket, dist.get(bucket) + 1);
            }
            if (s.getNpsScore() != null) {
                npsAnswers.add(s.getNpsScore());
            }
        }
        m.put(
                "avgScore",
                list.isEmpty()
                        ? null
                        : sum.divide(new BigDecimal(list.size()), 2, RoundingMode.HALF_UP));
        m.put("nps", SurveyScorer.nps(npsAnswers));
        m.put("distribution", dist);
        return m;
    }

    @Transactional
    public Complaint handleComplaint(Long id, String status, String handler, String resolution) {
        Complaint c = complaintMapper.selectById(id);
        if (c == null) {
            throw new BizException("投诉不存在");
        }
        DataScope.check(c.getDealerCode());
        c.setStatus(status);
        c.setHandler(handler);
        c.setResolution(resolution);
        complaintMapper.updateById(c);
        return c;
    }
}
