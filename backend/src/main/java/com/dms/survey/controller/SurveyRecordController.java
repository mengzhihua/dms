package com.dms.survey.controller;

import com.dms.common.BaseCrudController;
import com.dms.survey.entity.Survey;
import com.dms.survey.mapper.SurveyMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey/record")
public class SurveyRecordController extends BaseCrudController<Survey, SurveyMapper> {
    public SurveyRecordController() {
        super(Survey.class);
    }

    protected String[] keywordColumns() {
        return new String[] {"survey_no", "template_code"};
    }
}
