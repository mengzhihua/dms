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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:prodtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
            "dms.auth.bootstrap-admin-password=Prod#Admin123"
        })
class ProdProfileTest {
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;

    @Test
    void prodProfileNoDemoData() {
        // 演示账号不存在
        Map<String, String> bad = new HashMap<>();
        bad.put("username", "d001sa");
        bad.put("password", "123456");
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login", bad, Map.class);
        assertEquals(401, ((Number) r.getBody().get("code")).intValue());

        // 引导 admin 可用
        Map<String, String> good = new HashMap<>();
        good.put("username", "admin");
        good.put("password", "Prod#Admin123");
        ResponseEntity<Map> ok = http.postForEntity("/api/auth/login", good, Map.class);
        assertEquals(200, ok.getStatusCodeValue());
        assertEquals(0, ((Number) ok.getBody().get("code")).intValue());
        assertNotNull(((Map<String, Object>) ok.getBody().get("data")).get("token"));

        // 无演示主数据，但保留税率配置与调研模板
        assertEquals(
                0,
                jdbc.queryForObject("SELECT COUNT(*) FROM dms_dealer", Long.class).longValue());
        assertTrue(
                jdbc.queryForObject("SELECT COUNT(*) FROM dms_tax_config", Long.class) > 0);
        assertTrue(
                jdbc.queryForObject("SELECT COUNT(*) FROM dms_survey_template", Long.class) > 0);
        assertEquals(
                1,
                jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Long.class).longValue());

        // H2 控制台在 prod 下关闭
        ResponseEntity<String> h2 = http.getForEntity("/h2/", String.class);
        assertNotEquals(200, h2.getStatusCodeValue());
    }
}
