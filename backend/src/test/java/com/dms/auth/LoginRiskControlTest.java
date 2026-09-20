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
            "spring.datasource.url=jdbc:h2:mem:risktest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
            "dms.auth.ip-max-per-minute=0",
            "dms.auth.captcha-mode=ADAPTIVE"
        })
class LoginRiskControlTest {
    @Autowired TestRestTemplate http;
    @Autowired CaptchaService captchaService;

    private ResponseEntity<Map> login(String username, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        return http.postForEntity("/api/auth/login", body, Map.class);
    }

    @Test
    void adaptiveCaptchaFlow() {
        String user = "risk-" + System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            ResponseEntity<Map> r = login(user, "nope");
            assertEquals(401, ((Number) r.getBody().get("code")).intValue());
        }
        // 累计 3 次失败后要求验证码
        ResponseEntity<Map> req =
                http.getForEntity("/api/auth/captcha/required?username=" + user, Map.class);
        assertEquals(Boolean.TRUE, ((Map<String, Object>) req.getBody().get("data")).get("required"));

        // 不带验证码 → 4001
        ResponseEntity<Map> noCaptcha = login(user, "nope");
        assertEquals(4001, ((Number) noCaptcha.getBody().get("code")).intValue());
        // 验证码错误不计入密码失败

        // 拿验证码并以正确密码+验证码登录
        ResponseEntity<Map> cap = http.getForEntity("/api/auth/captcha", Map.class);
        Map<String, Object> capData = (Map<String, Object>) cap.getBody().get("data");
        String captchaId = (String) capData.get("captchaId");
        String code = captchaService.peek(captchaId);
        assertNotNull(code);
        assertTrue(String.valueOf(capData.get("image")).startsWith("data:image/png;base64,"));

        // d001sa 累计 3 次失败后也必须带验证码
        for (int i = 0; i < 3; i++) {
            login("d001sa", "wrong");
        }
        Map<String, String> body = new HashMap<>();
        body.put("username", "d001sa");
        body.put("password", "123456");
        body.put("captchaId", captchaId);
        body.put("captchaCode", code);
        ResponseEntity<Map> ok = http.postForEntity("/api/auth/login", body, Map.class);
        assertEquals(0, ((Number) ok.getBody().get("code")).intValue());
        assertNotNull(((Map<String, Object>) ok.getBody().get("data")).get("token"));

        // 验证码一次性消费：同一 id 不能复用
        assertFalse(captchaService.verify(captchaId, code));
    }
}
