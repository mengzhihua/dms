package com.dms.invoice.controller;

import com.dms.common.BaseCrudController;
import com.dms.invoice.entity.InvoiceLine;
import com.dms.invoice.mapper.InvoiceLineMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoice/line")
public class InvoiceLineController extends BaseCrudController<InvoiceLine, InvoiceLineMapper> {
    public InvoiceLineController() {
        super(InvoiceLine.class);
    }

    protected String[] keywordColumns() {
        return new String[]{"name"};
    }
}
