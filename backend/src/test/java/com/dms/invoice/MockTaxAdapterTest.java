package com.dms.invoice;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.invoice.entity.Invoice;
import com.dms.invoice.service.MockTaxAdapter;
import com.dms.invoice.service.TaxInvoiceGateway;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class MockTaxAdapterTest {
    @Test
    void issueSuccess() {
        MockTaxAdapter adapter = new MockTaxAdapter(0);
        Invoice inv = new Invoice();
        TaxInvoiceGateway.IssueResult r = adapter.issue(inv, Collections.emptyList());
        assertTrue(r.isSuccess());
        assertEquals(12, r.getCode().length());
        assertEquals(8, r.getNumber().length());
        assertEquals(20, r.getCheckCode().length());
        assertNotNull(r.getPdfUrl());
    }

    @Test
    void issueFailureRate() {
        MockTaxAdapter adapter = new MockTaxAdapter(1.0);
        TaxInvoiceGateway.IssueResult r = adapter.issue(new Invoice(), Collections.emptyList());
        assertFalse(r.isSuccess());
        assertNotNull(r.getErrorMsg());
    }

    @Test
    void redFlush() {
        MockTaxAdapter adapter = new MockTaxAdapter(0);
        TaxInvoiceGateway.IssueResult r =
                adapter.redFlush(new Invoice(), new Invoice(), Collections.emptyList());
        assertTrue(r.isSuccess());
    }
}
