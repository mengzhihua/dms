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
            "spring.datasource.url=jdbc:h2:mem:ratetest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
            "dms.auth.ip-max-per-minute=5",
            "dms.auth.captcha-mode=OFF"
        })
class LoginRateLimitTest {
    @Autowired TestRestTemplate http;

    @Test
    void rateLimit429() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "123456");
        ResponseEntity<Map> last = null;
        for (int i = 0; i < 5; i++) {
            last = http.postForEntity("/api/auth/login", body, Map.class);
            assertEquals(0, ((Number) last.getBody().get("code")).intValue());
        }
        ResponseEntity<Map> sixth = http.postForEntity("/api/auth/login", body, Map.class);
        assertEquals(429, sixth.getStatusCodeValue());
        assertEquals(429, ((Number) sixth.getBody().get("code")).intValue());
    }
}
