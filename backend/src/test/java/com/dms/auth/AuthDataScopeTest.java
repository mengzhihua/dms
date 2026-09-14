package com.dms.auth;

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

@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthDataScopeTest {
    @Autowired TestRestTemplate http;

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
