package com.dms.workshop;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.entity.WorkOrderLabor;
import com.dms.workshop.entity.WorkOrderPart;
import com.dms.workshop.service.WorkOrderCalculator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkOrderCalculatorTest {
    @Test
    void legalTransitions() {
        assertTrue(WorkOrderCalculator.canTransit("CHECKED_IN", "DIAGNOSED"));
        assertTrue(WorkOrderCalculator.canTransit("DIAGNOSED", "QUOTED"));
        assertTrue(WorkOrderCalculator.canTransit("QUOTED", "APPROVED"));
        assertTrue(WorkOrderCalculator.canTransit("APPROVED", "DISPATCHED"));
        assertTrue(WorkOrderCalculator.canTransit("DISPATCHED", "IN_REPAIR"));
        assertTrue(WorkOrderCalculator.canTransit("IN_REPAIR", "QC_PENDING"));
        assertTrue(WorkOrderCalculator.canTransit("QC_PENDING", "QC_PASSED"));
        assertTrue(WorkOrderCalculator.canTransit("QC_PENDING", "QC_FAILED"));
        assertTrue(WorkOrderCalculator.canTransit("QC_FAILED", "IN_REPAIR"));
        assertTrue(WorkOrderCalculator.canTransit("QC_PASSED", "SETTLED"));
        assertTrue(WorkOrderCalculator.canTransit("SETTLED", "DELIVERED"));
        assertTrue(WorkOrderCalculator.canTransit("DELIVERED", "CLOSED"));
        assertTrue(WorkOrderCalculator.canTransit("APPROVED", "CANCELLED"));
    }

    @Test
    void illegalTransitions() {
        assertFalse(WorkOrderCalculator.canTransit("CHECKED_IN", "SETTLED"));
        assertFalse(WorkOrderCalculator.canTransit("DISPATCHED", "CANCELLED"));
        assertFalse(WorkOrderCalculator.canTransit("IN_REPAIR", "CANCELLED"));
        assertFalse(WorkOrderCalculator.canTransit("SETTLED", "CANCELLED"));
        assertFalse(WorkOrderCalculator.canTransit("CLOSED", "CHECKED_IN"));
        assertFalse(WorkOrderCalculator.canTransit("QUOTED", "DISPATCHED"));
    }

    @Test
    void settleAmountAndTax() {
        WorkOrder o = new WorkOrder();
        o.setDiscountAmount(new BigDecimal("100.00"));
        List<WorkOrderLabor> labors = new ArrayList<>();
        WorkOrderLabor l1 = new WorkOrderLabor();
        l1.setHours(new BigDecimal("2.0"));
        l1.setRate(new BigDecimal("180.00"));
        l1.setIsWarranty(false);
        labors.add(l1);
        WorkOrderLabor wl = new WorkOrderLabor();
        wl.setHours(new BigDecimal("1.0"));
        wl.setRate(new BigDecimal("180.00"));
        wl.setIsWarranty(true);
        labors.add(wl);
        List<WorkOrderPart> parts = new ArrayList<>();
        WorkOrderPart p1 = new WorkOrderPart();
        p1.setQty(2);
        p1.setUnitPrice(new BigDecimal("328.00"));
        p1.setIsWarranty(false);
        parts.add(p1);
        WorkOrderCalculator.compute(o, labors, parts);
        // labor = 360+180=540, parts=656, total=1196, warranty=180, payable=1196-180-100=916
        assertEquals(new BigDecimal("540.00"), o.getLaborAmount());
        assertEquals(new BigDecimal("656.00"), o.getPartsAmount());
        assertEquals(new BigDecimal("1196.00"), o.getTotalAmount());
        assertEquals(new BigDecimal("180.00"), o.getWarrantyAmount());
        assertEquals(new BigDecimal("916.00"), o.getCustomerPayable());
        // tax = 916/1.13*0.13 = 105.38
        assertEquals(new BigDecimal("105.38"), o.getTaxAmount());
    }
}
