package com.dms.invoice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.common.CodeGenerator;
import com.dms.invoice.entity.Invoice;
import com.dms.invoice.entity.InvoiceLine;
import com.dms.invoice.mapper.InvoiceLineMapper;
import com.dms.invoice.mapper.InvoiceMapper;
import com.dms.invoice.mapper.TaxConfigMapper;
import com.dms.invoice.service.InvoiceService;
import com.dms.invoice.service.MockTaxAdapter;
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.entity.WorkOrderLabor;
import com.dms.workshop.entity.WorkOrderPart;
import com.dms.workshop.mapper.WorkOrderLaborMapper;
import com.dms.workshop.mapper.WorkOrderMapper;
import com.dms.workshop.mapper.WorkOrderPartMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvoiceServiceTest {
    private InvoiceService newService(
            InvoiceMapper im, InvoiceLineMapper lm, WorkOrderLaborMapper wlm, WorkOrderPartMapper wpm) {
        TaxConfigMapper tcm = mock(TaxConfigMapper.class);
        com.dms.invoice.entity.TaxConfig tc = new com.dms.invoice.entity.TaxConfig();
        tc.setTaxRate(new BigDecimal("0.13"));
        tc.setSellerName("测试经销商");
        tc.setSellerTaxNo("91310000TEST01");
        when(tcm.selectOne(any())).thenReturn(tc);
        com.dms.network.mapper.VehicleSalesOrderMapper som =
                mock(com.dms.network.mapper.VehicleSalesOrderMapper.class);
        WorkOrderMapper wom = mock(WorkOrderMapper.class);
        CodeGenerator cg = mock(CodeGenerator.class);
        when(cg.next(anyString())).thenReturn("INV-TEST-1");
        return new InvoiceService(im, lm, tcm, som, wom, wlm, wpm, cg, new MockTaxAdapter(0));
    }

    private InvoiceService newService(
            InvoiceMapper im,
            InvoiceLineMapper lm,
            com.dms.invoice.service.TaxInvoiceGateway gw) {
        TaxConfigMapper tcm = mock(TaxConfigMapper.class);
        com.dms.invoice.entity.TaxConfig tc = new com.dms.invoice.entity.TaxConfig();
        tc.setTaxRate(new BigDecimal("0.13"));
        when(tcm.selectOne(any())).thenReturn(tc);
        com.dms.network.mapper.VehicleSalesOrderMapper som =
                mock(com.dms.network.mapper.VehicleSalesOrderMapper.class);
        WorkOrderMapper wom = mock(WorkOrderMapper.class);
        CodeGenerator cg = mock(CodeGenerator.class);
        when(cg.next(anyString())).thenReturn("INV-TEST-1");
        return new InvoiceService(
                im, lm, tcm, som, wom,
                mock(WorkOrderLaborMapper.class), mock(WorkOrderPartMapper.class), cg, gw);
    }

    private static com.dms.invoice.service.TaxInvoiceGateway.IssueResult result(
            boolean success, boolean retryable) {
        com.dms.invoice.service.TaxInvoiceGateway.IssueResult r =
                new com.dms.invoice.service.TaxInvoiceGateway.IssueResult();
        r.setSuccess(success);
        r.setRetryable(retryable);
        r.setErrorMsg("err");
        return r;
    }

    @Test
    void issueRetryableStaysIssuing() {
        InvoiceMapper im = mock(InvoiceMapper.class);
        com.dms.invoice.service.TaxInvoiceGateway gw =
                mock(com.dms.invoice.service.TaxInvoiceGateway.class);
        when(gw.issue(any(), any())).thenReturn(result(false, true));
        InvoiceService svc = newService(im, mock(InvoiceLineMapper.class), gw);
        Invoice inv = new Invoice();
        inv.setId(1L);
        inv.setStatus("DRAFT");
        inv.setDealerCode("D001");
        when(im.selectById(1L)).thenReturn(inv);
        when(im.markIssuing(1L)).thenReturn(1);
        svc.issue(1L);
        assertEquals("ISSUING", inv.getStatus());
        assertEquals("err", inv.getErrorMsg());
    }

    @Test
    void issueNonRetryableFails() {
        InvoiceMapper im = mock(InvoiceMapper.class);
        com.dms.invoice.service.TaxInvoiceGateway gw =
                mock(com.dms.invoice.service.TaxInvoiceGateway.class);
        when(gw.issue(any(), any())).thenReturn(result(false, false));
        InvoiceService svc = newService(im, mock(InvoiceLineMapper.class), gw);
        Invoice inv = new Invoice();
        inv.setId(1L);
        inv.setStatus("DRAFT");
        inv.setDealerCode("D001");
        when(im.selectById(1L)).thenReturn(inv);
        when(im.markIssuing(1L)).thenReturn(1);
        svc.issue(1L);
        assertEquals("FAILED", inv.getStatus());
    }

    @Test
    void redFlushRetryableThenSyncNonRetryable() {
        InvoiceMapper im = mock(InvoiceMapper.class);
        InvoiceLineMapper lm = mock(InvoiceLineMapper.class);
        com.dms.invoice.service.TaxInvoiceGateway gw =
                mock(com.dms.invoice.service.TaxInvoiceGateway.class);
        InvoiceService svc = newService(im, lm, gw);

        Invoice orig = new Invoice();
        orig.setId(1L);
        orig.setStatus("ISSUED");
        orig.setDealerCode("D001");
        orig.setAmount(new BigDecimal("100.00"));
        orig.setTaxAmount(new BigDecimal("11.50"));
        orig.setNetAmount(new BigDecimal("88.50"));
        when(im.selectById(1L)).thenReturn(orig);
        // 库内已由 markRedFlushing 置为 RED_FLUSHING，但内存对象不变，写回时不应覆盖为 ISSUED
        when(im.markRedFlushing(1L)).thenReturn(1);
        when(gw.redFlush(any(), any(), any())).thenReturn(result(false, true));

        Invoice red = svc.redFlush(1L);
        assertEquals("ISSUING", red.getStatus());
        assertEquals("err", red.getErrorMsg());
        assertEquals("RED_FLUSHING", orig.getStatus());
        verify(im).updateById(orig);

        // sync：无 providerRef，requestId 为红票 invoiceNo；非可重试失败 → FAILED + 原票恢复
        when(im.selectById(red.getId())).thenReturn(red);
        when(gw.query(isNull(), eq(red.getInvoiceNo()))).thenReturn(result(false, false));
        svc.sync(red.getId());
        assertEquals("FAILED", red.getStatus());
        assertEquals("ISSUED", orig.getStatus());
    }

    @Test
    void callbackResolvesByRequestId() {
        InvoiceMapper im = mock(InvoiceMapper.class);
        InvoiceLineMapper lm = mock(InvoiceLineMapper.class);
        InvoiceService svc = newService(im, lm, mock(com.dms.invoice.service.TaxInvoiceGateway.class));
        Invoice inv = new Invoice();
        inv.setId(9L);
        inv.setInvoiceNo("INV-TEST-1");
        inv.setStatus("ISSUING");
        // providerRef/号码均缺 → 直接命中 requestId 回退
        when(im.selectOne(any())).thenReturn(inv);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("requestId", "INV-TEST-1");
        body.put("status", "ISSUED");
        Invoice r = svc.callback("SIM", body);
        assertEquals("ISSUED", r.getStatus());
    }

    @Test
    void discountedOrderLinesSumEqualsHeader() {
        InvoiceMapper im = mock(InvoiceMapper.class);
        InvoiceLineMapper lm = mock(InvoiceLineMapper.class);
        WorkOrderLaborMapper wlm = mock(WorkOrderLaborMapper.class);
        WorkOrderPartMapper wpm = mock(WorkOrderPartMapper.class);

        WorkOrderLabor l1 = new WorkOrderLabor();
        l1.setName("小保养");
        l1.setHours(new BigDecimal("2"));
        l1.setRate(new BigDecimal("180"));
        l1.setAmount(new BigDecimal("360.00"));
        l1.setIsWarranty(false);
        WorkOrderLabor wl = new WorkOrderLabor();
        wl.setName("保修工时");
        wl.setHours(new BigDecimal("1"));
        wl.setRate(new BigDecimal("180"));
        wl.setAmount(new BigDecimal("180.00"));
        wl.setIsWarranty(true);
        when(wlm.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(l1, wl));
        WorkOrderPart p1 = new WorkOrderPart();
        p1.setName("机油");
        p1.setQty(2);
        p1.setUnitPrice(new BigDecimal("328"));
        p1.setAmount(new BigDecimal("656.00"));
        p1.setIsWarranty(false);
        when(wpm.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(p1));

        WorkOrder o = new WorkOrder();
        o.setId(1L);
        o.setDealerCode("D001");
        // 非保修合计 1016，折扣 100 → 应付 916
        o.setCustomerPayable(new BigDecimal("916.00"));
        o.setDiscountAmount(new BigDecimal("100.00"));

        InvoiceService svc = newService(im, lm, wlm, wpm);
        svc.createFromWorkOrder(o, "ELECTRONIC", "客户", null);

        ArgumentCaptor<Invoice> invCap = ArgumentCaptor.forClass(Invoice.class);
        verify(im).insert(invCap.capture());
        Invoice inv = invCap.getValue();
        ArgumentCaptor<InvoiceLine> lineCap = ArgumentCaptor.forClass(InvoiceLine.class);
        verify(lm, times(3)).insert(lineCap.capture()); // 工时 + 备件 + 折扣行
        List<InvoiceLine> lines = lineCap.getAllValues();
        BigDecimal sum = BigDecimal.ZERO;
        for (InvoiceLine l : lines) {
            sum = sum.add(l.getAmount());
        }
        assertEquals(new BigDecimal("916.00"), inv.getAmount());
        assertEquals(0, sum.compareTo(inv.getAmount()), "Σ行金额应等于发票含税金额");
        assertTrue(lines.stream().anyMatch(l -> "折扣".equals(l.getName())
                && l.getAmount().compareTo(BigDecimal.ZERO) < 0));
    }

    @Test
    void issueIdempotentWhenConcurrent() {
        InvoiceMapper im = mock(InvoiceMapper.class);
        InvoiceLineMapper lm = mock(InvoiceLineMapper.class);
        InvoiceService svc =
                newService(im, lm, mock(WorkOrderLaborMapper.class), mock(WorkOrderPartMapper.class));
        Invoice inv = new Invoice();
        inv.setId(1L);
        inv.setStatus("DRAFT");
        inv.setDealerCode("D001");
        when(im.selectById(1L)).thenReturn(inv);
        // 并发抢占失败且当前已 ISSUED → 直接返回
        when(im.markIssuing(1L)).thenReturn(0);
        inv.setStatus("ISSUED");
        assertSame(inv, svc.issue(1L));
        // 并发抢占失败且未开具 → 抛错
        inv.setStatus("ISSUING");
        assertThrows(com.dms.common.BizException.class, () -> svc.issue(1L));
    }
}
