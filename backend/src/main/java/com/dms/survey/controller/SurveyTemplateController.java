package com.dms.survey.controller;

import com.dms.common.BaseCrudController;
import com.dms.survey.entity.SurveyTemplate;
import com.dms.survey.mapper.SurveyTemplateMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey/template")
public class SurveyTemplateController extends BaseCrudController<SurveyTemplate, SurveyTemplateMapper> {
    public SurveyTemplateController() {
        super(SurveyTemplate.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"code","name"};
    }
}
