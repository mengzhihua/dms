package com.dms.customer.controller;

import com.dms.common.BaseCrudController;
import com.dms.customer.entity.Customer;
import com.dms.customer.mapper.CustomerMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/customer")
public class CustomerController extends BaseCrudController<Customer, CustomerMapper> {
    public CustomerController() {
        super(Customer.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"name","phone"};
    }
}
