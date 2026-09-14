package com.dms.guide.controller;

import com.dms.common.BaseCrudController;
import com.dms.guide.entity.LaborItem;
import com.dms.guide.mapper.LaborItemMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guide/labor")
public class LaborItemController extends BaseCrudController<LaborItem, LaborItemMapper> {
    public LaborItemController() {
        super(LaborItem.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"code","name"};
    }
}
