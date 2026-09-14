package com.dms.invoice.controller;

import com.dms.common.BaseCrudController;
import com.dms.invoice.entity.TaxConfig;
import com.dms.invoice.mapper.TaxConfigMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoice/tax-config")
public class TaxConfigController extends BaseCrudController<TaxConfig, TaxConfigMapper> {
    public TaxConfigController() {
        super(TaxConfig.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"dealer_code"};
    }
}
