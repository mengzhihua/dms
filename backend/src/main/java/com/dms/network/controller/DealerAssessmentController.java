package com.dms.network.controller;

import com.dms.common.BaseCrudController;
import com.dms.network.entity.DealerAssessment;
import com.dms.network.mapper.DealerAssessmentMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/network/assessment")
public class DealerAssessmentController extends BaseCrudController<DealerAssessment, DealerAssessmentMapper> {
    public DealerAssessmentController() {
        super(DealerAssessment.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"dealer_code","year_month"};
    }
}
