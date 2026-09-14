package com.dms.network.controller;

import com.dms.common.BaseCrudController;
import com.dms.network.entity.Bay;
import com.dms.network.mapper.BayMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/network/bay")
public class BayController extends BaseCrudController<Bay, BayMapper> {
    public BayController() {
        super(Bay.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"code","type"};
    }
}
