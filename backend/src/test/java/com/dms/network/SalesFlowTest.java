package com.dms.network;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.DmsApplication;
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
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:salestest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
        })
class SalesFlowTest {
    @Autowired TestRestTemplate http;

    @SuppressWarnings("unchecked")
    private String login(String username, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login", body, Map.class);
        return (String) ((Map<String, Object>) r.getBody().get("data")).get("token");
    }

    private HttpEntity<?> auth(String token, Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return new HttpEntity<>(body, h);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String token, String path, Map<String, Object> body) {
        ResponseEntity<Map> r = http.exchange(path, HttpMethod.POST, auth(token, body), Map.class);
        return r.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String token, String path) {
        ResponseEntity<Map> r =
                http.exchange(path, HttpMethod.GET, auth(token, null), Map.class);
        return r.getBody();
    }

    private Long createOrder(String token, int price, int deposit) {
        Map<String, Object> o = new HashMap<>();
        o.put("dealerCode", "D001");
        o.put("customerId", 1);
        o.put("modelCode", "M001");
        o.put("price", price);
        o.put("deposit", deposit);
        Map<String, Object> r = post(token, "/api/network/sales-order", o);
        assertEquals(0, ((Number) r.get("code")).intValue(), String.valueOf(r));
        return ((Number) ((Map<String, Object>) r.get("data")).get("id")).longValue();
    }

    @Test
    void loanHappyPath() {
        String admin = login("admin", "123456");
        Long id = createOrder(admin, 150000, 5000);

        Map<String, Object> fin = new HashMap<>();
        fin.put("loanProvider", "上汽通用金融");
        fin.put("loanAmount", 100000);
        fin.put("loanTermMonths", 24);
        Map<String, Object> r = post(admin, "/api/network/sales-order/" + id + "/finance", fin);
        assertEquals("LOAN", ((Map) r.get("data")).get("paymentType"));
        assertEquals("APPLIED", ((Map) r.get("data")).get("loanStatus"));

        Map<String, Object> dec = new HashMap<>();
        dec.put("approved", true);
        r = post(admin, "/api/network/sales-order/" + id + "/finance/decision", dec);
        assertEquals("APPROVED", ((Map) r.get("data")).get("loanStatus"));

        r = post(admin, "/api/network/sales-order/" + id + "/allocate", null);
        assertEquals("ALLOCATED", ((Map) r.get("data")).get("status"));

        Map<String, Object> pay = new HashMap<>();
        pay.put("payType", "BALANCE");
        pay.put("amount", 45000);
        pay.put("method", "TRANSFER");
        r = post(admin, "/api/network/sales-order/" + id + "/payment", pay);
        assertEquals(0, ((Number) r.get("code")).intValue());

        // 未开票不能交付
        Map<String, Object> d = new HashMap<>();
        d.put("pdiPassed", true);
        r = post(admin, "/api/network/sales-order/" + id + "/deliver", d);
        assertNotEquals(0, ((Number) r.get("code")).intValue());

        r = post(admin, "/api/network/sales-order/" + id + "/invoice",
                new HashMap<String, Object>());
        assertEquals("INVOICED", ((Map) r.get("data")).get("status"));
        assertNotNull(((Map) r.get("data")).get("invoiceId"));

        Map<String, Object> detail = get(admin, "/api/network/sales-order/" + id + "/detail");
        Map<String, Object> dd = (Map<String, Object>) detail.get("data");
        Map<String, Object> inv = (Map<String, Object>) dd.get("invoice");
        assertEquals("DRAFT", inv.get("status"));
        assertEquals(id.longValue(), ((Number) inv.get("salesOrderId")).longValue());
        assertEquals(2, ((java.util.List<Object>) dd.get("payments")).size());

        // PDI 未通过不能交付
        Map<String, Object> bad = new HashMap<>();
        bad.put("pdiPassed", false);
        r = post(admin, "/api/network/sales-order/" + id + "/deliver", bad);
        assertTrue(String.valueOf(r.get("msg")).contains("PDI"));

        Map<String, Object> good = new HashMap<>();
        good.put("pdiPassed", true);
        good.put("remark", "PDI OK");
        r = post(admin, "/api/network/sales-order/" + id + "/deliver", good);
        assertEquals("DELIVERED", ((Map) r.get("data")).get("status"));
        assertNotNull(((Map) r.get("data")).get("surveyId"));
        assertNotNull(((Map) r.get("data")).get("deliveredAt"));
    }

    @Test
    void fullPayMustSettleBalance() {
        String admin = login("admin", "123456");
        Long id = createOrder(admin, 100000, 2000);
        post(admin, "/api/network/sales-order/" + id + "/allocate", null);
        post(admin, "/api/network/sales-order/" + id + "/invoice",
                new HashMap<String, Object>());

        Map<String, Object> d = new HashMap<>();
        d.put("pdiPassed", true);
        Map<String, Object> r = post(admin, "/api/network/sales-order/" + id + "/deliver", d);
        assertTrue(String.valueOf(r.get("msg")).contains("尾款未结清"));
    }
}
