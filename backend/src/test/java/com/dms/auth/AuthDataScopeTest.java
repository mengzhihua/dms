package com.dms.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.DmsApplication;
import com.dms.auth.service.AuthService;
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

@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthDataScopeTest {
    @Autowired TestRestTemplate http;
    @Autowired AuthService authService;

    @SuppressWarnings("unchecked")
    private String login(String username, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login", body, Map.class);
        assertEquals(200, r.getStatusCodeValue());
        assertEquals(0, ((Number) r.getBody().get("code")).intValue());
        return (String) ((Map<String, Object>) r.getBody().get("data")).get("token");
    }

    private HttpEntity<?> auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return new HttpEntity<>(h);
    }

    private HttpEntity<?> auth(String token, Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return new HttpEntity<>(body, h);
    }

    @Test
    void noToken401() {
        ResponseEntity<Map> r =
                http.getForEntity("/api/network/dealer/page?size=5", Map.class);
        assertEquals(401, r.getStatusCodeValue());
        assertEquals(401, ((Number) r.getBody().get("code")).intValue());
    }

    @Test
    void wrongPassword401() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "wrong");
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login", body, Map.class);
        assertEquals(401, ((Number) r.getBody().get("code")).intValue());
    }

    @Test
    void dealerScopeFiltersPage() {
        String admin = login("admin", "123456");
        ResponseEntity<Map> all =
                http.exchange(
                        "/api/network/dealer/page?size=50",
                        HttpMethod.GET,
                        auth(admin),
                        Map.class);
        Map<String, Object> page = (Map<String, Object>) all.getBody().get("data");
        long total = ((Number) page.get("total")).longValue();
        assertTrue(total >= 3, "admin 应看到全部经销商, got " + total);

        String sa = login("d001sa", "123456");
        ResponseEntity<Map> scoped =
                http.exchange(
                        "/api/network/dealer/page?size=50",
                        HttpMethod.GET,
                        auth(sa),
                        Map.class);
        Map<String, Object> page2 = (Map<String, Object>) scoped.getBody().get("data");
        assertEquals(1L, ((Number) page2.get("total")).longValue());
        Map<String, Object> row =
                (Map<String, Object>)
                        ((java.util.List<Object>) page2.get("records")).get(0);
        assertEquals("D001", row.get("code"));
    }

    @Test
    void crossDealerOrderDenied() {
        String sa = login("d001sa", "123456");
        Map<String, Object> cust = new HashMap<>();
        cust.put("name", "数据范围测试客户");
        cust.put("phone", "13800000001");
        cust.put("dealerCode", "D001");
        ResponseEntity<Map> c =
                http.exchange("/api/customer/customer", HttpMethod.POST, auth(sa, cust), Map.class);
        Long cid = ((Number) ((Map) c.getBody().get("data")).get("id")).longValue();

        Map<String, Object> veh = new HashMap<>();
        veh.put("vin", "VINSCOPE" + System.currentTimeMillis());
        veh.put("modelCode", "M001");
        veh.put("customerId", cid);
        veh.put("dealerCode", "D001");
        ResponseEntity<Map> v =
                http.exchange("/api/customer/vehicle", HttpMethod.POST, auth(sa, veh), Map.class);
        Long vid = ((Number) ((Map) v.getBody().get("data")).get("id")).longValue();

        Map<String, Object> order = new HashMap<>();
        order.put("vehicleId", vid);
        order.put("mileageIn", 1000);
        ResponseEntity<Map> o =
                http.exchange("/api/workshop/order", HttpMethod.POST, auth(sa, order), Map.class);
        Long oid = ((Number) ((Map) o.getBody().get("data")).get("id")).longValue();

        String d002 = login("d002mgr", "123456");
        ResponseEntity<Map> denied =
                http.exchange("/api/workshop/order/" + oid, HttpMethod.GET, auth(d002), Map.class);
        assertEquals(400, ((Number) denied.getBody().get("code")).intValue());
        assertTrue(String.valueOf(denied.getBody().get("msg")).contains("无权访问"));

        ResponseEntity<Map> page =
                http.exchange(
                        "/api/workshop/order/page?size=50", HttpMethod.GET, auth(d002), Map.class);
        Map<String, Object> p = (Map<String, Object>) page.getBody().get("data");
        for (Object row : (java.util.List<Object>) p.get("records")) {
            assertEquals("D002", ((Map) row).get("dealerCode"));
        }
    }

    @Test
    void technicianForbiddenWrite() {
        String tech = login("d001tech", "123456");
        Map<String, Object> cust = new HashMap<>();
        cust.put("name", "越权测试");
        cust.put("phone", "13800000002");
        ResponseEntity<Map> r =
                http.exchange(
                        "/api/customer/customer", HttpMethod.POST, auth(tech, cust), Map.class);
        assertEquals(403, r.getStatusCodeValue());
        assertEquals(403, ((Number) r.getBody().get("code")).intValue());
    }

    @Test
    void crossDealerAnswersDenied() {
        String admin = login("admin", "123456");
        Map<String, Object> sv = new HashMap<>();
        sv.put("surveyNo", "SV-IT-" + System.currentTimeMillis());
        sv.put("templateCode", "SV01");
        sv.put("dealerCode", "D001");
        sv.put("customerId", 1);
        sv.put("status", "PENDING");
        ResponseEntity<Map> created =
                http.exchange("/api/survey/record", HttpMethod.POST, auth(admin, sv), Map.class);
        Long sid = ((Number) ((Map) created.getBody().get("data")).get("id")).longValue();

        ResponseEntity<Map> qs =
                http.exchange(
                        "/api/survey/question/list?size=50&templateId=1",
                        HttpMethod.GET,
                        auth(admin),
                        Map.class);
        java.util.List<Object> questions =
                (java.util.List<Object>) qs.getBody().get("data");
        java.util.List<Map<String, Object>> answers = new java.util.ArrayList<>();
        for (Object q : questions) {
            Map<String, Object> a = new HashMap<>();
            a.put("questionId", ((Number) ((Map) q).get("id")).longValue());
            a.put("score", 8);
            answers.add(a);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("answers", answers);
        ResponseEntity<Map> answered =
                http.exchange("/api/survey/" + sid + "/answer", HttpMethod.POST, auth(admin, body), Map.class);
        assertEquals("ANSWERED", ((Map) answered.getBody().get("data")).get("status"));

        ResponseEntity<Map> mine =
                http.exchange(
                        "/api/survey/record/" + sid + "/answers",
                        HttpMethod.GET,
                        auth(login("d001sa", "123456")),
                        Map.class);
        assertEquals(0, ((Number) mine.getBody().get("code")).intValue());
        assertEquals(5, ((java.util.List<Object>) mine.getBody().get("data")).size());

        ResponseEntity<Map> denied =
                http.exchange(
                        "/api/survey/record/" + sid + "/answers",
                        HttpMethod.GET,
                        auth(login("d002mgr", "123456")),
                        Map.class);
        assertEquals(400, ((Number) denied.getBody().get("code")).intValue());
        assertTrue(String.valueOf(denied.getBody().get("msg")).contains("无权访问"));
    }

    @Test
    void loginLockout() {
        String user = "ghost-" + System.currentTimeMillis();
        Map<String, String> bad = new HashMap<>();
        bad.put("username", user);
        bad.put("password", "nope");
        for (int i = 0; i < 5; i++) {
            ResponseEntity<Map> r = http.postForEntity("/api/auth/login", bad, Map.class);
            assertEquals("用户名或密码错误", r.getBody().get("msg"));
        }
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login", bad, Map.class);
        assertTrue(String.valueOf(r.getBody().get("msg")).contains("次数过多"));
    }

    @Test
    void blankUsername400() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "");
        body.put("password", "x");
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login", body, Map.class);
        assertEquals(400, ((Number) r.getBody().get("code")).intValue());
        assertTrue(String.valueOf(r.getBody().get("msg")).contains("必填"));

        Map<String, String> body2 = new HashMap<>();
        body2.put("username", "admin");
        body2.put("password", "");
        ResponseEntity<Map> r2 = http.postForEntity("/api/auth/login", body2, Map.class);
        assertEquals(400, ((Number) r2.getBody().get("code")).intValue());
    }

    @Test
    void lockExpiryResetsCount() throws InterruptedException {
        String user = "ghost2-" + System.currentTimeMillis();
        Map<String, String> bad = new HashMap<>();
        bad.put("username", user);
        bad.put("password", "nope");
        authService.setLockMillisForTest(150);
        try {
            for (int i = 0; i < 5; i++) {
                ResponseEntity<Map> r = http.postForEntity("/api/auth/login", bad, Map.class);
                assertEquals("用户名或密码错误", r.getBody().get("msg"));
            }
            Thread.sleep(300);
            // 锁定已过期，计数清零，应回到普通错误而不是“次数过多”
            ResponseEntity<Map> r = http.postForEntity("/api/auth/login", bad, Map.class);
            assertEquals("用户名或密码错误", r.getBody().get("msg"));
        } finally {
            authService.setLockMillisForTest(15 * 60 * 1000L);
            authService.clearLock(user);
        }
    }

    @Test
    void disabledUserToken401() {
        String admin = login("admin", "123456");
        String uname = "itoff" + System.currentTimeMillis() % 100000;
        Map<String, Object> nu = new HashMap<>();
        nu.put("username", uname);
        nu.put("realName", "测试禁用");
        nu.put("role", "ADVISOR");
        nu.put("dealerCode", "D001");
        nu.put("password", "123456");
        ResponseEntity<Map> c =
                http.exchange("/api/auth/user", HttpMethod.POST, auth(admin, nu), Map.class);
        Long uid = ((Number) ((Map) c.getBody().get("data")).get("id")).longValue();
        String tok = login(uname, "123456");

        nu.put("id", uid);
        nu.put("enabled", false);
        http.exchange("/api/auth/user/" + uid, HttpMethod.PUT, auth(admin, nu), Map.class);

        ResponseEntity<Map> me =
                http.exchange("/api/auth/me", HttpMethod.GET, auth(tok), Map.class);
        assertEquals(401, me.getStatusCodeValue());
        assertTrue(String.valueOf(me.getBody().get("msg")).contains("禁用"));
    }

    @Test
    void meAndPassword() {
        String sa = login("d001sa", "123456");
        ResponseEntity<Map> me =
                http.exchange("/api/auth/me", HttpMethod.GET, auth(sa), Map.class);
        assertEquals("d001sa", ((Map) me.getBody().get("data")).get("username"));
        assertFalse(String.valueOf(me.getBody()).contains("passwordHash"));

        Map<String, String> pwd = new HashMap<>();
        pwd.put("old", "123456");
        pwd.put("new", "abc12345");
        ResponseEntity<Map> r =
                http.exchange("/api/auth/password", HttpMethod.PUT, auth(sa, pwd), Map.class);
        assertEquals(0, ((Number) r.getBody().get("code")).intValue());
        login("d001sa", "abc12345");
        // 还原密码避免影响其他用例
        String sa2 = login("d001sa", "abc12345");
        pwd.put("old", "abc12345");
        pwd.put("new", "123456");
        http.exchange("/api/auth/password", HttpMethod.PUT, auth(sa2, pwd), Map.class);
    }
}
