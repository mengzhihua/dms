package com.dms.survey.controller;

import com.dms.common.BaseCrudController;
import com.dms.survey.entity.SurveyQuestion;
import com.dms.survey.mapper.SurveyQuestionMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey/question")
public class SurveyQuestionController extends BaseCrudController<SurveyQuestion, SurveyQuestionMapper> {
    public SurveyQuestionController() {
        super(SurveyQuestion.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"text"};
    }
}
