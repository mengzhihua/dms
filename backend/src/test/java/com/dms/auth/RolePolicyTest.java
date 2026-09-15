package com.dms.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RolePolicyTest {

    @Test
    void adminEverything() {
        assertTrue(RolePolicy.allowed("ADMIN", "DELETE", "/api/network/dealer/1"));
        assertTrue(RolePolicy.allowed("ADMIN", "POST", "/api/auth/user"));
        assertTrue(RolePolicy.allowed("ADMIN", "GET", "/api/dashboard"));
    }

    @Test
    void oemReadAllWriteWhitelist() {
        assertTrue(RolePolicy.allowed("OEM", "GET", "/api/workshop/order/page"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/network/target"));
        assertTrue(RolePolicy.allowed("OEM", "POST", "/api/guide/guide"));
        assertTrue(RolePolicy.allowed("OEM", "PUT", "/api/customer/model/1"));
        assertFalse(RolePolicy.allowed("OEM", "POST", "/api/workshop/order"));
        assertFalse(RolePolicy.allowed("OEM", "POST", "/api/invoice/1/issue"));
        assertFalse(RolePolicy.allowed("OEM", "POST", "/api/customer/customer"));
    }

    @Test
    void dealerManagerMostlyWriteExceptHeadquarters() {
        assertTrue(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/workshop/order"));
        assertTrue(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/network/sales-order"));
        assertTrue(RolePolicy.allowed("DEALER_MANAGER", "GET", "/api/network/dealer/page"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/network/dealer"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/network/target"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/network/assessment/1"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/guide/labor"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/survey/template"));
        assertFalse(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/auth/user"));
        assertTrue(RolePolicy.allowed("DEALER_MANAGER", "POST", "/api/guide/recommend"));
    }

    @Test
    void advisorReadAllWriteWorkshopCustomerSurvey() {
        assertTrue(RolePolicy.allowed("ADVISOR", "GET", "/api/network/dealer/page"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/workshop/order/1/dispatch"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/customer/customer"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/survey/5/answer"));
        assertTrue(RolePolicy.allowed("ADVISOR", "POST", "/api/survey/complaint/1/handle"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/invoice/1/issue"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/network/dealer"));
        assertFalse(RolePolicy.allowed("ADVISOR", "POST", "/api/parts/part"));
    }

    @Test
    void technicianLimitedReadAndQcWrite() {
        assertTrue(RolePolicy.allowed("TECHNICIAN", "GET", "/api/workshop/order/1"));
        assertTrue(RolePolicy.allowed("TECHNICIAN", "GET", "/api/guide/guide/page"));
        assertTrue(RolePolicy.allowed("TECHNICIAN", "POST", "/api/workshop/order/1/start"));
        assertTrue(RolePolicy.allowed("TECHNICIAN", "POST", "/api/workshop/order/1/finish"));
        assertTrue(RolePolicy.allowed("TECHNICIAN", "POST", "/api/workshop/order/1/qc"));
        assertTrue(RolePolicy.allowed("TECHNICIAN", "GET", "/api/network/dealer/page"));
        assertTrue(RolePolicy.allowed("TECHNICIAN", "GET", "/api/dashboard"));
        assertFalse(RolePolicy.allowed("TECHNICIAN", "POST", "/api/network/dealer"));
        assertFalse(RolePolicy.allowed("TECHNICIAN", "GET", "/api/invoice/page"));
        assertFalse(RolePolicy.allowed("TECHNICIAN", "POST", "/api/workshop/order"));
        assertFalse(RolePolicy.allowed("TECHNICIAN", "POST", "/api/workshop/order/1/settle"));
        assertFalse(RolePolicy.allowed("TECHNICIAN", "POST", "/api/customer/customer"));
    }

    @Test
    void financeInvoiceAndSettle() {
        assertTrue(RolePolicy.allowed("FINANCE", "GET", "/api/workshop/order/page"));
        assertTrue(RolePolicy.allowed("FINANCE", "POST", "/api/invoice/1/issue"));
        assertTrue(RolePolicy.allowed("FINANCE", "POST", "/api/workshop/order/1/settle"));
        assertTrue(RolePolicy.allowed("FINANCE", "POST", "/api/network/sales-order/1/invoice"));
        assertFalse(RolePolicy.allowed("FINANCE", "POST", "/api/customer/customer"));
        assertFalse(RolePolicy.allowed("FINANCE", "POST", "/api/workshop/order"));
    }

    @Test
    void unknownRoleDenied() {
        assertFalse(RolePolicy.allowed("GHOST", "GET", "/api/dashboard"));
    }
}
