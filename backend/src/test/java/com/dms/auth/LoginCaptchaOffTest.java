package com.dms.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.DmsApplication;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:capoff;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
            "dms.auth.ip-max-per-minute=0",
            "dms.auth.captcha-mode=OFF"
        })
class LoginCaptchaOffTest {
    @Autowired TestRestTemplate http;

    @Test
    void offModeNeverRequiresCaptcha() {
        String user = "capoff-" + System.currentTimeMillis();
        Map<String, String> body = new HashMap<>();
        body.put("username", user);
        body.put("password", "nope");
        for (int i = 0; i < 4; i++) {
            ResponseEntity<Map> r = http.postForEntity("/api/auth/login", body, Map.class);
            assertEquals(401, ((Number) r.getBody().get("code")).intValue());
        }
        ResponseEntity<Map> req =
                http.getForEntity("/api/auth/captcha/required?username=" + user, Map.class);
        assertEquals(
                Boolean.FALSE, ((Map<String, Object>) req.getBody().get("data")).get("required"));
        // OFF 模式下正常登录不需要验证码
        body.put("username", "admin");
        body.put("password", "123456");
        ResponseEntity<Map> ok = http.postForEntity("/api/auth/login", body, Map.class);
        assertEquals(0, ((Number) ok.getBody().get("code")).intValue());
    }
}
