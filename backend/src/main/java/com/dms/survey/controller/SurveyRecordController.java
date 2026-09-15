package com.dms.survey.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.auth.DataScope;
import com.dms.common.BaseCrudController;
import com.dms.common.BizException;
import com.dms.common.R;
import com.dms.survey.entity.Survey;
import com.dms.survey.entity.SurveyAnswer;
import com.dms.survey.mapper.SurveyAnswerMapper;
import com.dms.survey.mapper.SurveyMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey/record")
public class SurveyRecordController extends BaseCrudController<Survey, SurveyMapper> {
    @Autowired private SurveyAnswerMapper answerMapper;

    public SurveyRecordController() {
        super(Survey.class);
    }

    @GetMapping("/{id}/answers")
    public R<List<SurveyAnswer>> answers(@PathVariable Long id) {
        Survey s = mapper.selectById(id);
        if (s == null) {
            throw new BizException("调研不存在");
        }
        DataScope.check(s.getDealerCode());
        return R.ok(answerMapper.selectList(new QueryWrapper<SurveyAnswer>().eq("survey_id", id)));
    }

    protected String[] keywordColumns() {
        return new String[] {"survey_no", "template_code"};
    }
}
