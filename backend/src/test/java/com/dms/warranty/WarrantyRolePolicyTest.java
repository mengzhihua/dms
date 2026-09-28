package com.dms.warranty;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.auth.RolePolicy;
import org.junit.jupiter.api.Test;

/** 保修索赔权限：经销商提交/发货，OEM 审核/结算，财务只读。 */
class WarrantyRolePolicyTest {

    @Test
    void advisorDealerSide() {
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/warranty/claim"));
        assertTrue(RolePolicy.allowed("ADVISOR", "PUT", "/api/warranty/claim/1"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/warranty/claim/1/submit"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/warranty/claim/1/ship"));
        assertTrue(RolePolicy.allowed("ADVISOR", "GET", "/api/warranty/claim/1/lines"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/warranty/claim/1/approve"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/warranty/claim/1/reject"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/warranty/claim/1/return"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/warranty/claim/1/receive"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/warranty/settlement/generate"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/warranty/settlement/1/pay"));
    }

    @Test
    void oemSide() {
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/warranty/claim/1/approve"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/warranty/claim/1/reject"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/warranty/claim/1/return"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/warranty/claim/1/receive"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/warranty/settlement/generate"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/warranty/settlement/1/confirm"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/warranty/settlement/1/pay"));
        assertTrue(RolePolicy.allowed("OEM", "GET", "/api/warranty/claim/page"));
        assertFalse(RolePolicy.allowed("OEM", "POST", "/api/warranty/claim/1/submit"));
        assertFalse(RolePolicy.allowed("OEM", "POST", "/api/warranty/claim/1/ship"));
    }

    @Test
    void dealerManagerDeniedOemActions() {
        assertTrue(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/warranty/claim/1/submit"));
        assertTrue(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/warranty/claim/1/ship"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/warranty/claim/1/approve"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/warranty/claim/1/reject"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/warranty/settlement/generate"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/warranty/settlement/1/pay"));
    }

    @Test
    void financeReadOnly() {
        assertTrue(RolePolicy.allowed("FINANCE", "GET", "/api/warranty/claim/page"));
        assertTrue(RolePolicy.allowed("FINANCE", "GET", "/api/warranty/settlement/page"));
        assertFalse(RolePolicy.allowed("FINANCE", "POST", "/api/warranty/claim/1/submit"));
        assertFalse(RolePolicy.allowed("FINANCE", "POST", "/api/warranty/claim/1/approve"));
        assertFalse(RolePolicy.allowed("FINANCE", "POST", "/api/warranty/settlement/generate"));
    }
}
