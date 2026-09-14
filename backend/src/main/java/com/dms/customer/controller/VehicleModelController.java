package com.dms.customer.controller;

import com.dms.common.BaseCrudController;
import com.dms.customer.entity.VehicleModel;
import com.dms.customer.mapper.VehicleModelMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/model")
public class VehicleModelController extends BaseCrudController<VehicleModel, VehicleModelMapper> {
    public VehicleModelController() {
        super(VehicleModel.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"code","name"};
    }
}
