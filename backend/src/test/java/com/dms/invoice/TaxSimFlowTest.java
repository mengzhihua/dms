package com.dms.invoice;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.DmsApplication;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("tax-sim")
@TestPropertySource(
        properties = {
            "server.port=18099",
            "spring.datasource.url=jdbc:h2:mem:taxsim;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
            "dms.tax.provider=HTTP",
            "dms.tax.endpoint=http://127.0.0.1:18099/sim/tax",
            "dms.tax.app-id=dms-app",
            "dms.tax.app-secret=test-tax-app-secret-0123456789abcdef",
            "dms.tax.callback-url=http://127.0.0.1:18099/api/invoice/callback/SIM"
        })
class TaxSimFlowTest {
    @Autowired TestRestTemplate http;

    @SuppressWarnings("unchecked")
    private String adminToken() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "123456");
        ResponseEntity<Map> r =
                http.postForEntity("http://127.0.0.1:18099/api/auth/login", body, Map.class);
        return (String) ((Map<String, Object>) r.getBody().get("data")).get("token");
    }

    private HttpEntity<?> auth(String token, Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return new HttpEntity<>(body, h);
    }

    @SuppressWarnings("unchecked")
    private Long createInvoice(String token, String remark) {
        Map<String, Object> inv = new HashMap<>();
        inv.put("dealerCode", "D001");
        inv.put("invoiceType", "ELECTRONIC");
        inv.put("buyerName", "测试买家");
        inv.put("amount", new BigDecimal("113.00"));
        inv.put("remark", remark);
        ResponseEntity<Map> r =
                http.exchange(
                        "http://127.0.0.1:18099/api/invoice",
                        HttpMethod.POST,
                        auth(token, inv),
                        Map.class);
        assertEquals(0, ((Number) r.getBody().get("code")).intValue(), String.valueOf(r.getBody()));
        return ((Number) ((Map<String, Object>) r.getBody().get("data")).get("id")).longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String token, String path) {
        ResponseEntity<Map> r =
                http.exchange(
                        "http://127.0.0.1:18099" + path, HttpMethod.POST, auth(token, null),
                        Map.class);
        return r.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String token, String path) {
        ResponseEntity<Map> r =
                http.exchange(
                        "http://127.0.0.1:18099" + path, HttpMethod.GET, auth(token, null),
                        Map.class);
        return (Map<String, Object>) r.getBody().get("data");
    }

    @Test
    void issuePendingSyncFailRedFlush() {
        String admin = adminToken();

        // 直接开具成功
        Long id1 = createInvoice(admin, "");
        Map<String, Object> issued = post(admin, "/api/invoice/" + id1 + "/issue");
        assertEquals("ISSUED", ((Map) issued.get("data")).get("status"));
        assertNotNull(((Map) issued.get("data")).get("taxInvoiceNumber"));

        // PENDING → ISSUING → sync → ISSUED
        Long id2 = createInvoice(admin, "PENDING");
        Map<String, Object> pending = post(admin, "/api/invoice/" + id2 + "/issue");
        assertEquals("ISSUING", ((Map) pending.get("data")).get("status"));
        assertNotNull(((Map) pending.get("data")).get("providerRef"));
        Map<String, Object> synced = post(admin, "/api/invoice/" + id2 + "/sync");
        assertEquals("ISSUED", ((Map) synced.get("data")).get("status"));

        // FAIL → FAILED
        Long id3 = createInvoice(admin, "FAIL");
        Map<String, Object> failed = post(admin, "/api/invoice/" + id3 + "/issue");
        assertEquals("FAILED", ((Map) failed.get("data")).get("status"));
        assertEquals("模拟税控拒绝", ((Map) failed.get("data")).get("errorMsg"));

        // 红冲 id1 → 红票 ISSUED，原票 RED_FLUSHED
        Map<String, Object> red = post(admin, "/api/invoice/" + id1 + "/red-flush");
        assertEquals("ISSUED", ((Map) red.get("data")).get("status"));
        assertEquals(id1.longValue(), ((Number) ((Map) red.get("data")).get("redOfInvoiceId")).longValue());
        Map<String, Object> orig = get(admin, "/api/invoice/" + id1);
        assertEquals("RED_FLUSHED", orig.get("status"));
    }
}
