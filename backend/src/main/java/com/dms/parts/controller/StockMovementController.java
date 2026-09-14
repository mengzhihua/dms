package com.dms.parts.controller;

import com.dms.common.BaseCrudController;
import com.dms.parts.entity.StockMovement;
import com.dms.parts.mapper.StockMovementMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parts/movement")
public class StockMovementController extends BaseCrudController<StockMovement, StockMovementMapper> {
    public StockMovementController() {
        super(StockMovement.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"part_no","ref_no"};
    }
}
