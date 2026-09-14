package com.dms.workshop.controller;

import com.dms.common.BaseCrudController;
import com.dms.workshop.entity.Appointment;
import com.dms.workshop.mapper.AppointmentMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workshop/appointment")
public class AppointmentController extends BaseCrudController<Appointment, AppointmentMapper> {
    public AppointmentController() {
        super(Appointment.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"dealer_code"};
    }
}
