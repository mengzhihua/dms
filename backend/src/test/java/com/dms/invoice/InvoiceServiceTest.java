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
        WorkOrderMapper wom = mock(WorkOrderMapper.class);
        CodeGenerator cg = mock(CodeGenerator.class);
        when(cg.next(anyString())).thenReturn("INV-TEST-1");
        return new InvoiceService(im, lm, tcm, wom, wlm, wpm, cg, new MockTaxAdapter(0));
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
