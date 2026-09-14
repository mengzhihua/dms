package com.dms.survey.controller;

import com.dms.common.BaseCrudController;
import com.dms.survey.entity.Complaint;
import com.dms.survey.mapper.ComplaintMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey/complaint")
public class ComplaintController extends BaseCrudController<Complaint, ComplaintMapper> {
    public ComplaintController() {
        super(Complaint.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"complaint_no","content"};
    }
}
