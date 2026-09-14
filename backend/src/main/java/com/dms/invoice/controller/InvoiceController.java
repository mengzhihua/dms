package com.dms.invoice.controller;

import com.dms.common.BaseCrudController;
import com.dms.common.R;
import com.dms.invoice.entity.Invoice;
import com.dms.invoice.mapper.InvoiceMapper;
import com.dms.invoice.service.InvoiceService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice")
public class InvoiceController extends BaseCrudController<Invoice, InvoiceMapper> {
    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        super(Invoice.class);
        this.service = service;
    }

    protected String[] keywordColumns() {
        return new String[] {"invoice_no", "buyer_name", "tax_invoice_number"};
    }

    @Override
    public R<Invoice> create(@RequestBody Invoice entity) {
        return R.ok(service.createManual(entity));
    }

    @PostMapping("/{id}/issue")
    public R<Invoice> issue(@PathVariable Long id) {
        return R.ok(service.issue(id));
    }

    @PostMapping("/{id}/red-flush")
    public R<Invoice> redFlush(@PathVariable Long id) {
        return R.ok(service.redFlush(id));
    }

    @GetMapping("/{id}/preview")
    public R<Map<String, Object>> preview(@PathVariable Long id) {
        return R.ok(service.preview(id));
    }

    /** 税控服务商异步回调：POST /api/invoice/callback/MOCK 等。 */
    @PostMapping("/callback/{provider}")
    public R<Invoice> callback(@PathVariable String provider, @RequestBody Map<String, Object> body) {
        return R.ok(service.callback(provider, body));
    }
}
