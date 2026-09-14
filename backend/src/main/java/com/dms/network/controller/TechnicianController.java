package com.dms.network.controller;

import com.dms.common.BaseCrudController;
import com.dms.network.entity.Technician;
import com.dms.network.mapper.TechnicianMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/network/technician")
public class TechnicianController extends BaseCrudController<Technician, TechnicianMapper> {
    public TechnicianController() {
        super(Technician.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"code","name"};
    }
}
