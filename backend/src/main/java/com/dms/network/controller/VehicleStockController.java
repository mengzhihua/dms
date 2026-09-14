package com.dms.network.controller;

import com.dms.common.BaseCrudController;
import com.dms.network.entity.VehicleStock;
import com.dms.network.mapper.VehicleStockMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/network/vehicle-stock")
public class VehicleStockController extends BaseCrudController<VehicleStock, VehicleStockMapper> {
    public VehicleStockController() {
        super(VehicleStock.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"vin","model_code"};
    }
}
