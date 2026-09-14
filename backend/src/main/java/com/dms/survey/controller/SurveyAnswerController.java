package com.dms.survey.controller;

import com.dms.common.BaseCrudController;
import com.dms.survey.entity.SurveyAnswer;
import com.dms.survey.mapper.SurveyAnswerMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey/answer")
public class SurveyAnswerController extends BaseCrudController<SurveyAnswer, SurveyAnswerMapper> {
    public SurveyAnswerController() {
        super(SurveyAnswer.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"text"};
    }
}
