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
    private Map<String, Object> put(String token, String path, Map<String, Object> body) {
        ResponseEntity<Map> r = http.exchange(path, HttpMethod.PUT, auth(token, body), Map.class);
        return r.getBody();
    }

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
        // 定金 + 贷款到账(审批自动入账) + 尾款 = 3 条流水
        assertEquals(3, ((java.util.List<Object>) dd.get("payments")).size());

        // 草稿发票不能交车
        Map<String, Object> d0 = new HashMap<>();
        d0.put("pdiPassed", true);
        r = post(admin, "/api/network/sales-order/" + id + "/deliver", d0);
        assertTrue(String.valueOf(r.get("msg")).contains("发票未开具"));

        // 开具发票后才可交车
        Long invId = ((Number) inv.get("id")).longValue();
        r = post(admin, "/api/invoice/" + invId + "/issue", new HashMap<String, Object>());
        assertEquals(0, ((Number) r.get("code")).intValue(), String.valueOf(r));

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
        Map<String, Object> invoiced =
                post(admin, "/api/network/sales-order/" + id + "/invoice",
                        new HashMap<String, Object>());
        Long invId =
                ((Number) ((Map<String, Object>) invoiced.get("data")).get("invoiceId")).longValue();
        post(admin, "/api/invoice/" + invId + "/issue", new HashMap<String, Object>());

        Map<String, Object> d = new HashMap<>();
        d.put("pdiPassed", true);
        Map<String, Object> r = post(admin, "/api/network/sales-order/" + id + "/deliver", d);
        assertTrue(String.valueOf(r.get("msg")).contains("尾款未结清"));
    }

    @Test
    void editRejectsDepositChange() {
        String admin = login("admin", "123456");
        Long id = createOrder(admin, 100000, 5000);

        // 改定金 → 拒绝（已入账）
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", 1);
        body.put("modelCode", "M001");
        body.put("price", 100000);
        body.put("deposit", 6000);
        Map<String, Object> r = put(admin, "/api/network/sales-order/" + id, body);
        assertTrue(String.valueOf(r.get("msg")).contains("定金已入账"));

        // 定金不变 + 改其他字段 → 成功
        body.put("deposit", 5000);
        body.put("color", "白色");
        body.put("remark", "修改备注");
        r = put(admin, "/api/network/sales-order/" + id, body);
        assertEquals(0, ((Number) r.get("code")).intValue(), String.valueOf(r));
        assertEquals("白色", ((Map<?, ?>) r.get("data")).get("color"));
    }

    @Test
    void editPriceNotBelowAppliedLoan() {
        String admin = login("admin", "123456");
        Long id = createOrder(admin, 150000, 5000);

        Map<String, Object> fin = new HashMap<>();
        fin.put("loanProvider", "上汽通用金融");
        fin.put("loanAmount", 100000);
        post(admin, "/api/network/sales-order/" + id + "/finance", fin);

        // 车价低于已申请贷款 → 拒绝
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", 1);
        body.put("modelCode", "M001");
        body.put("deposit", 5000);
        body.put("price", 80000);
        Map<String, Object> r = put(admin, "/api/network/sales-order/" + id, body);
        assertTrue(String.valueOf(r.get("msg")).contains("车价不能低于已申请贷款金额"));

        // 车价 120000 ≥ 贷款额 → 成功
        body.put("price", 120000);
        r = put(admin, "/api/network/sales-order/" + id, body);
        assertEquals(0, ((Number) r.get("code")).intValue(), String.valueOf(r));
    }
}
