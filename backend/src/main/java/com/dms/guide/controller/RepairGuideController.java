package com.dms.guide.controller;

import com.dms.common.BaseCrudController;
import com.dms.guide.entity.RepairGuide;
import com.dms.guide.mapper.RepairGuideMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guide/guide")
public class RepairGuideController extends BaseCrudController<RepairGuide, RepairGuideMapper> {
    public RepairGuideController() {
        super(RepairGuide.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"code","title"};
    }
}
