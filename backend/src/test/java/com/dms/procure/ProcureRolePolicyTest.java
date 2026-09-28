package com.dms.procure;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.auth.RolePolicy;
import org.junit.jupiter.api.Test;

/** 备件采购权限：经销商询价/下单/收货，OEM 报价/确认/对账，财务只读。 */
class ProcureRolePolicyTest {

    @Test
    void advisorDealerSide() {
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/inquiry"));
        assertTrue(RolePolicy.allowed("ADVISOR", "PUT", "/api/procure/inquiry/1/lines"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/inquiry/1/send"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/inquiry/1/order"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/order"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/order/from-shortage"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/order/1/submit"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/order/1/receive"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/order/1/cancel"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/inquiry/1/quote"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/order/1/confirm"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/order/1/reject"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/statement/generate"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/procure/statement/1/pay"));
    }

    @Test
    void oemSide() {
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/procure/inquiry/1/quote"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/procure/order/1/confirm"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/procure/order/1/reject"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/procure/statement"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/procure/statement/generate"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/procure/statement/1/confirm"));
        assertFalse(RolePolicy.allowed("OEM", "POST", "/api/procure/order/1/receive"));
        assertFalse(RolePolicy.allowed("OEM", "POST", "/api/procure/inquiry"));
    }

    @Test
    void dealerManagerDeniedOemActions() {
        assertTrue(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/procure/order/1/submit"));
        assertTrue(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/procure/order/1/receive"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/procure/inquiry/1/quote"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/procure/order/1/confirm"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/procure/statement/generate"));
    }

    @Test
    void financeReadOnly() {
        assertTrue(RolePolicy.allowed("FINANCE", "GET", "/api/procure/order/page"));
        assertTrue(RolePolicy.allowed("FINANCE", "GET", "/api/procure/statement/page"));
        assertFalse(RolePolicy.allowed("FINANCE", "POST", "/api/procure/order/1/receive"));
        assertFalse(RolePolicy.allowed("FINANCE", "POST", "/api/procure/statement/generate"));
        assertFalse(RolePolicy.allowed("FINANCE", "POST", "/api/procure/order"));
    }
}
