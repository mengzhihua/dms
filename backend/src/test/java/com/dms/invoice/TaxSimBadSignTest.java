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
            "server.port=18098",
            "spring.datasource.url=jdbc:h2:mem:taxbadsign;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
            "dms.tax.provider=HTTP",
            "dms.tax.endpoint=http://127.0.0.1:18098/sim/tax",
            "dms.tax.app-id=dms-app",
            "dms.tax.app-secret=test-tax-app-secret-0123456789abcdef",
            "dms.taxsim.expected-secret=wrong-secret-padded-to-32-bytes-000000"
        })
class TaxSimBadSignTest {
    @Autowired TestRestTemplate http;

    @Test
    void badSignRejected() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "123456");
        ResponseEntity<Map> lr =
                http.postForEntity("http://127.0.0.1:18098/api/auth/login", body, Map.class);
        String token =
                (String) ((Map<String, Object>) lr.getBody().get("data")).get("token");

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        Map<String, Object> inv = new HashMap<>();
        inv.put("dealerCode", "D001");
        inv.put("invoiceType", "ELECTRONIC");
        inv.put("buyerName", "测试买家");
        inv.put("amount", new BigDecimal("50.00"));
        ResponseEntity<Map> cr =
                http.exchange(
                        "http://127.0.0.1:18098/api/invoice",
                        HttpMethod.POST,
                        new HttpEntity<>(inv, h),
                        Map.class);
        Long id =
                ((Number) ((Map<String, Object>) cr.getBody().get("data")).get("id")).longValue();

        ResponseEntity<Map> issue =
                http.exchange(
                        "http://127.0.0.1:18098/api/invoice/" + id + "/issue",
                        HttpMethod.POST,
                        new HttpEntity<>(h),
                        Map.class);
        Map<String, Object> data = (Map<String, Object>) issue.getBody().get("data");
        assertEquals("FAILED", data.get("status"));
        assertTrue(String.valueOf(data.get("errorMsg")).contains("401"));
    }
}
