package com.dms.network.controller;

import com.dms.common.BaseCrudController;
import com.dms.network.entity.DealerTarget;
import com.dms.network.mapper.DealerTargetMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/network/target")
public class DealerTargetController extends BaseCrudController<DealerTarget, DealerTargetMapper> {
    public DealerTargetController() {
        super(DealerTarget.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"dealer_code","year_month"};
    }
}
