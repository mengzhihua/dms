package com.dms.invoice.service;

import com.dms.invoice.entity.Invoice;
import com.dms.invoice.entity.InvoiceLine;
import java.util.List;
import java.util.Random;

/** 模拟税控服务商：生成 12 位发票代码 + 8 位发票号码 + 20 位校验码；按 mock-fail-rate 概率失败。 */
public class MockTaxAdapter implements TaxInvoiceGateway {
    private final double failRate;
    private final Random random = new Random();

    public MockTaxAdapter(double failRate) {
        this.failRate = failRate;
    }

    @Override
    public IssueResult issue(Invoice invoice, List<InvoiceLine> lines) {
        return simulate();
    }

    @Override
    public IssueResult query(String providerRef) {
        IssueResult r = simulate();
        r.setProviderRef(providerRef);
        return r;
    }

    @Override
    public IssueResult redFlush(Invoice original, Invoice redInvoice, List<InvoiceLine> redLines) {
        return simulate();
    }

    private IssueResult simulate() {
        IssueResult r = new IssueResult();
        if (random.nextDouble() < failRate) {
            r.setSuccess(false);
            r.setErrorMsg("模拟税控返回：开票失败（网络超时）");
            return r;
        }
        r.setSuccess(true);
        r.setCode(digits(12));
        r.setNumber(digits(8));
        r.setCheckCode(digits(20));
        r.setPdfUrl("https://mock-tax.example.com/pdf/" + r.getNumber() + ".pdf");
        r.setProviderRef("MOCK-" + r.getNumber());
        return r;
    }

    private String digits(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
