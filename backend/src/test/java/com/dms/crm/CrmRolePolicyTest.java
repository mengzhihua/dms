package com.dms.crm;

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

/** CRM 权限矩阵：技师只读、顾问可操作任务、模板仅 OEM/管理员。 */
@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:crmrole;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
        })
class CrmRolePolicyTest {
    @Autowired TestRestTemplate http;

    private String login(String username) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", "123456");
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login", body, Map.class);
        return (String) ((Map<String, Object>) r.getBody().get("data")).get("token");
    }

    private HttpEntity<?> auth(String token, Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return body == null ? new HttpEntity<>(h) : new HttpEntity<>(body, h);
    }

    private int code(String token, String method, String path, Object body) {
        ResponseEntity<Map> r =
                http.exchange(path, HttpMethod.valueOf(method), auth(token, body), Map.class);
        return ((Number) r.getBody().get("code")).intValue();
    }

    @Test
    void advisorCanTaskButNotTemplate() {
        String sa = login("d001sa");
        Map<String, Object> t = new HashMap<>();
        t.put("title", "回访");
        assertEquals(0, code(sa, "POST", "/api/crm/task", t));
        Map<String, Object> tpl = new HashMap<>();
        tpl.put("code", "X1");
        tpl.put("name", "测试模板");
        assertNotEquals(0, code(sa, "POST", "/api/crm/template", tpl));
        assertEquals(0, code(sa, "GET", "/api/crm/template/list", null));
    }

    @Test
    void techReadOnly() {
        String tech = login("d001tech");
        assertEquals(0, code(tech, "GET", "/api/crm/task/list", null));
        Map<String, Object> t = new HashMap<>();
        t.put("title", "回访");
        assertNotEquals(0, code(tech, "POST", "/api/crm/task", t));
        Map<String, Object> done = new HashMap<>();
        done.put("result", "x");
        assertNotEquals(0, code(tech, "POST", "/api/crm/task/1/complete", done));
        Map<String, Object> n = new HashMap<>();
        n.put("channel", "SMS");
        assertNotEquals(0, code(tech, "POST", "/api/crm/message/1/retry", n));
    }

    @Test
    void managerDeniedTemplateOemAllowed() {
        String mgr = login("d001mgr");
        String oem = login("oem");
        Map<String, Object> tpl = new HashMap<>();
        tpl.put("code", "X2");
        tpl.put("name", "OEM模板");
        assertNotEquals(0, code(mgr, "POST", "/api/crm/template", tpl));
        assertEquals(0, code(oem, "POST", "/api/crm/template", tpl));
        // 经理可操作本店任务（不在 deny 列表）
        Map<String, Object> t = new HashMap<>();
        t.put("title", "经理任务");
        assertEquals(0, code(mgr, "POST", "/api/crm/task", t));
    }

    @Test
    void financeReadOnly() {
        String fin = login("d001fin");
        assertEquals(0, code(fin, "GET", "/api/crm/task/list", null));
        Map<String, Object> t = new HashMap<>();
        t.put("title", "回访");
        assertNotEquals(0, code(fin, "POST", "/api/crm/task", t));
    }
}
