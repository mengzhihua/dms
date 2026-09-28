package com.dms.report;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.DmsApplication;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
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
import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.mapper.WorkOrderMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 多维报表与经营日报：JSON 结构、Excel/PDF 导出、日报幂等、数据范围。 */
@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:reportflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
        })
class ReportFlowTest {
    @Autowired TestRestTemplate http;
    @Autowired WorkOrderMapper orderMapper;

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String token, String path) {
        ResponseEntity<Map> r = http.exchange(path, HttpMethod.GET, auth(token, null), Map.class);
        assertEquals(0, ((Number) r.getBody().get("code")).intValue(), path + " -> " + r.getBody());
        return (Map<String, Object>) r.getBody().get("data");
    }

    private int code(String token, String method, String path, Object body) {
        ResponseEntity<Map> r =
                http.exchange(path, HttpMethod.valueOf(method), auth(token, body), Map.class);
        Object c = r.getBody() == null ? null : r.getBody().get("code");
        return c == null ? r.getStatusCodeValue() : ((Number) c).intValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void workshopReportAndExports() {
        String sa = login("d001sa");
        // 造一单已结算工单（保证区间内有数据）
        WorkOrder o = new WorkOrder();
        o.setOrderNo("WO-RPT-T1");
        o.setDealerCode("D001");
        o.setStatus("DELIVERED");
        o.setCheckInTime(LocalDateTime.now().minusDays(2));
        o.setSettleTime(LocalDateTime.now().minusDays(1));
        o.setDeliverTime(LocalDateTime.now());
        o.setTotalAmount(new BigDecimal("800"));
        o.setLaborAmount(new BigDecimal("300"));
        o.setPartsAmount(new BigDecimal("500"));
        o.setCustomerPayable(new BigDecimal("800"));
        orderMapper.insert(o);

        String from = LocalDate.now().minusDays(30).toString();
        String to = LocalDate.now().toString();

        Map<String, Object> t =
                call(sa, "/api/report/workshop?from=" + from + "&to=" + to + "&groupBy=month");
        List<Map<String, Object>> sections = (List<Map<String, Object>>) t.get("sections");
        assertFalse(sections.isEmpty());
        List<List<Object>> rows = (List<List<Object>>) sections.get(0).get("rows");
        assertFalse(rows.isEmpty());
        Map<String, Object> summary = (Map<String, Object>) t.get("summary");
        assertTrue(new java.math.BigDecimal(String.valueOf(summary.get("结算金额合计"))).signum() > 0);

        // Excel 导出
        ResponseEntity<byte[]> xlsx =
                http.exchange(
                        "/api/report/workshop/export?from=" + from + "&to=" + to + "&format=xlsx",
                        HttpMethod.GET,
                        auth(sa, null),
                        byte[].class);
        assertEquals(200, xlsx.getStatusCodeValue());
        assertTrue(xlsx.getBody().length > 100);
        assertEquals('P', xlsx.getBody()[0]);
        assertEquals('K', xlsx.getBody()[1]);

        // PDF 导出
        ResponseEntity<byte[]> pdf =
                http.exchange(
                        "/api/report/workshop/export?from=" + from + "&to=" + to + "&format=pdf",
                        HttpMethod.GET,
                        auth(sa, null),
                        byte[].class);
        assertEquals(200, pdf.getStatusCodeValue());
        String head = new String(pdf.getBody(), 0, 4);
        assertEquals("%PDF", head);

        // 非法类型/区间
        assertNotEquals(0, code(sa, "GET", "/api/report/xxx?from=" + from + "&to=" + to, null));
        assertNotEquals(
                0,
                code(sa, "GET", "/api/report/workshop?from=2020-01-01&to=2021-12-31&groupBy=day", null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void dailyGenerateIdempotentAndScoped() {
        String sa = login("d001sa");
        String d002 = login("d002mgr");
        String today = LocalDate.now().toString();

        Map<String, Object> gen = new HashMap<>();
        gen.put("dealerCode", "D001");
        gen.put("date", today);
        List<Map<String, Object>> rows1 =
                (List<Map<String, Object>>) exchangeBody(sa, "POST", "/api/report/daily/generate", gen);
        assertEquals(1, rows1.size());
        assertEquals("D001", rows1.get(0).get("dealerCode"));

        // 幂等：再次生成不重复
        List<Map<String, Object>> rows2 =
                (List<Map<String, Object>>) exchangeBody(sa, "POST", "/api/report/daily/generate", gen);
        assertEquals(1, rows2.size());
        List<Map<String, Object>> list =
                (List<Map<String, Object>>) getData(sa, "/api/report/daily?date=" + today);
        assertEquals(1, list.size());

        // 经销商范围：d002 仅能看到自己店的日报
        List<Map<String, Object>> list2 =
                (List<Map<String, Object>>) getData(d002, "/api/report/daily?date=" + today);
        assertTrue(list2.stream().noneMatch(m -> "D001".equals(m.get("dealerCode"))));

        // 技师只读：GET 可以，POST generate 被拒
        String tech = login("d001tech");
        assertEquals(0, code(tech, "GET", "/api/report/daily?date=" + today, null));
        Map<String, Object> g2 = new HashMap<>();
        g2.put("dealerCode", "D001");
        assertNotEquals(0, code(tech, "POST", "/api/report/daily/generate", g2));
    }

    @Test
    void scopeForcesOwnDealer() {
        String sa = login("d001sa");
        String from = LocalDate.now().minusDays(30).toString();
        String to = LocalDate.now().toString();
        // d001sa 请求 dealerCode=D002 仍只能得到 D001 数据
        Map<String, Object> t =
                call(sa, "/api/report/workshop?from=" + from + "&to=" + to + "&dealerCode=D002&groupBy=dealer");
        List<Map<String, Object>> sections = (List<Map<String, Object>>) t.get("sections");
        for (Map<String, Object> sec : sections) {
            for (Object row : (List<?>) sec.get("rows")) {
                assertTrue(String.valueOf(((List<?>) row).get(0)).contains("D001"));
            }
        }
    }

    private Object exchangeBody(String token, String method, String path, Object body) {
        ResponseEntity<Map> r =
                http.exchange(path, HttpMethod.valueOf(method), auth(token, body), Map.class);
        assertEquals(0, ((Number) r.getBody().get("code")).intValue(), path + " -> " + r.getBody());
        return r.getBody().get("data");
    }

    private Object getData(String token, String path) {
        return exchangeBody(token, "GET", path, null);
    }
}
