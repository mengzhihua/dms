package com.dms.parts.controller;

import com.dms.common.BaseCrudController;
import com.dms.parts.entity.Part;
import com.dms.parts.mapper.PartMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parts/part")
public class PartController extends BaseCrudController<Part, PartMapper> {
    public PartController() {
        super(Part.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"part_no","name"};
    }
}
