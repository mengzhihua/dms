package com.dms.procure;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.DmsApplication;
import com.dms.parts.entity.Part;
import com.dms.parts.entity.PartStock;
import com.dms.parts.mapper.PartMapper;
import com.dms.parts.service.PartStockService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
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

/**
 * 备件采购全流程：询价 → OEM 报价 → 转采购订单 → 提交 → 确认 → 分批到货入库 → 对账付款。
 */
@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:procureflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
        })
class ProcurementFlowTest {
    @Autowired TestRestTemplate http;
    @Autowired PartStockService stockService;
    @Autowired PartMapper partMapper;

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
    private Map<String, Object> call(String token, String method, String path, Object body) {
        ResponseEntity<Map> r =
                http.exchange(path, HttpMethod.valueOf(method), auth(token, body), Map.class);
        assertEquals(0, ((Number) r.getBody().get("code")).intValue(), path + " -> " + r.getBody());
        return (Map<String, Object>) r.getBody().get("data");
    }

    private ResponseEntity<Map> raw(String token, String method, String path, Object body) {
        return http.exchange(path, HttpMethod.valueOf(method), auth(token, body), Map.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void inquiryToStatementFullFlow() {
        String sa = login("d001sa");
        String oem = login("oem");
        String admin = login("admin");

        // 询价：两条明细，同件号合并
        Map<String, Object> inq = new HashMap<>();
        inq.put("dealerCode", "D001");
        inq.put("title", "保养件询价");
        Map<String, Object> l1 = new HashMap<>();
        l1.put("partNo", "P0001");
        l1.put("qty", 4);
        l1.put("targetPrice", 14);
        Map<String, Object> l2 = new HashMap<>();
        l2.put("partNo", "P0001");
        l2.put("qty", 2);
        Map<String, Object> l3 = new HashMap<>();
        l3.put("partNo", "P0002");
        l3.put("qty", 3);
        inq.put("lines", Arrays.asList(l1, l2, l3));
        Map<String, Object> created = call(sa, "POST", "/api/procure/inquiry", inq);
        Long iid = ((Number) created.get("id")).longValue();
        assertEquals("DRAFT", created.get("status"));

        java.util.List<Object> iLines =
                (java.util.List<Object>)
                        raw(sa, "GET", "/api/procure/inquiry/" + iid + "/lines", null)
                                .getBody().get("data");
        assertEquals(2, iLines.size(), "同件号应合并");

        call(sa, "POST", "/api/procure/inquiry/" + iid + "/send", null);

        // OEM 报价：缺一行报价应失败
        Map<String, Object> badQuote = new HashMap<>();
        badQuote.put("lines", Collections.singletonList(
                new HashMap<String, Object>() {{
                    put("lineId", ((Map<String, Object>) iLines.get(0)).get("id"));
                    put("quotedPrice", 13.5);
                }}));
        ResponseEntity<Map> bad = raw(oem, "POST", "/api/procure/inquiry/" + iid + "/quote", badQuote);
        assertNotEquals(0, ((Number) bad.getBody().get("code")).intValue());

        Map<String, Object> quote = new HashMap<>();
        java.util.List<Object> qlines = new java.util.ArrayList<>();
        for (Object row : iLines) {
            Map<String, Object> l = (Map<String, Object>) row;
            Map<String, Object> q = new HashMap<>();
            q.put("lineId", l.get("id"));
            q.put("quotedPrice", 13.50);
            q.put("leadDays", 7);
            qlines.add(q);
        }
        quote.put("lines", qlines);
        quote.put("oemRemark", "含税含运");
        Map<String, Object> quoted = call(oem, "POST", "/api/procure/inquiry/" + iid + "/quote", quote);
        assertEquals("QUOTED", quoted.get("status"));

        // 顾问不能报价
        ResponseEntity<Map> saQuote =
                raw(sa, "POST", "/api/procure/inquiry/" + iid + "/quote", quote);
        assertEquals(403, saQuote.getStatusCodeValue());

        // 转采购订单
        Map<String, Object> po = call(sa, "POST", "/api/procure/inquiry/" + iid + "/order", null);
        Long poid = ((Number) po.get("id")).longValue();
        assertEquals("DRAFT", po.get("status"));
        assertEquals("INQUIRY", po.get("source"));

        call(sa, "POST", "/api/procure/order/" + poid + "/submit", null);
        // 经理不能 OEM 确认
        ResponseEntity<Map> mgrConfirm =
                raw(login("d001mgr"), "POST", "/api/procure/order/" + poid + "/confirm", new HashMap<>());
        assertEquals(403, mgrConfirm.getStatusCodeValue());

        Map<String, Object> confirm = new HashMap<>();
        confirm.put("oemOrderNo", "OEM-T-001");
        call(oem, "POST", "/api/procure/order/" + poid + "/confirm", confirm);

        // 分批到货：第一批每行 1 件
        java.util.List<Object> poLines =
                (java.util.List<Object>)
                        raw(sa, "GET", "/api/procure/order/" + poid + "/lines", null)
                                .getBody().get("data");
        Long line1 = ((Number) ((Map<String, Object>) poLines.get(0)).get("id")).longValue();
        String partNo = (String) ((Map<String, Object>) poLines.get(0)).get("partNo");
        int before = stockService.available("D001", partNo);

        Map<String, Object> rcv1 = new HashMap<>();
        rcv1.put("location", "RCV-01");
        rcv1.put("lines", Collections.singletonList(
                new HashMap<String, Object>() {{ put("orderLineId", line1); put("qty", 2); }}));
        Map<String, Object> poAfter1 = call(sa, "POST", "/api/procure/order/" + poid + "/receive", rcv1);
        assertEquals("PARTIAL_RECEIVED", poAfter1.get("status"));
        assertEquals(before + 2, stockService.available("D001", partNo), "库存应增加 2");

        // 超量到货被拒
        Map<String, Object> over = new HashMap<>();
        over.put("lines", Collections.singletonList(
                new HashMap<String, Object>() {{ put("orderLineId", line1); put("qty", 99); }}));
        ResponseEntity<Map> overRes = raw(sa, "POST", "/api/procure/order/" + poid + "/receive", over);
        assertNotEquals(0, ((Number) overRes.getBody().get("code")).intValue());
        assertTrue(String.valueOf(overRes.getBody().get("msg")).contains("超出"));

        // 收货剩余
        Map<String, Object> rcv2 = new HashMap<>();
        rcv2.put("location", "RCV-01");
        java.util.List<Object> rest = new java.util.ArrayList<>();
        for (Object row : poLines) {
            Map<String, Object> l = (Map<String, Object>) row;
            int qty = ((Number) l.get("qty")).intValue();
            int got = ((Number) l.get("receivedQty")).intValue();
            int remain = qty - (line1.equals(((Number) l.get("id")).longValue()) ? got + 2 : got);
            if (remain > 0) {
                Map<String, Object> m = new HashMap<>();
                m.put("orderLineId", l.get("id"));
                m.put("qty", remain);
                rest.add(m);
            }
        }
        rcv2.put("lines", rest);
        Map<String, Object> poAfter2 = call(sa, "POST", "/api/procure/order/" + poid + "/receive", rcv2);
        assertEquals("RECEIVED", poAfter2.get("status"));

        call(sa, "POST", "/api/procure/order/" + poid + "/close", null);

        // 对账
        Map<String, Object> gen = new HashMap<>();
        gen.put("dealerCode", "D001");
        Map<String, Object> st = call(oem, "POST", "/api/procure/statement/generate", gen);
        Long sid = ((Number) st.get("id")).longValue();
        assertTrue(((Number) st.get("orderCount")).intValue() >= 1);
        BigDecimal expected =
                new BigDecimal(String.valueOf(poAfter2.get("receivedAmount")));
        assertTrue(new BigDecimal(String.valueOf(st.get("totalAmount")))
                        .compareTo(expected) >= 0);
        call(oem, "POST", "/api/procure/statement/" + sid + "/confirm", null);
        Map<String, Object> paid = call(oem, "POST", "/api/procure/statement/" + sid + "/pay", null);
        assertEquals("PAID", paid.get("status"));
    }

    @Test
    void scopeAndGuards() {
        String admin = login("admin");
        // 手工 POST 订单实体（带明细）允许，但 PUT 实体被拒
        Map<String, Object> po = new HashMap<>();
        po.put("dealerCode", "D001");
        po.put("lines", Collections.singletonList(
                new HashMap<String, Object>() {{ put("partNo", "P0003"); put("qty", 1); }}));
        Map<String, Object> created = call(admin, "POST", "/api/procure/order", po);
        Long poid = ((Number) created.get("id")).longValue();

        ResponseEntity<Map> putDenied =
                raw(admin, "PUT", "/api/procure/order/" + poid, created);
        assertNotEquals(0, ((Number) putDenied.getBody().get("code")).intValue());

        // 跨店读明细被拒
        String d002 = login("d002mgr");
        ResponseEntity<Map> denied =
                raw(d002, "GET", "/api/procure/order/" + poid + "/lines", null);
        assertNotEquals(0, ((Number) denied.getBody().get("code")).intValue());

        // 顾问 POST 原始实体带 lines 是允许的（业务接口），但删除已提交订单被拒
        String sa = login("d001sa");
        call(sa, "POST", "/api/procure/order/" + poid + "/submit", null);
        ResponseEntity<Map> delDenied =
                raw(sa, "DELETE", "/api/procure/order/" + poid, null);
        assertNotEquals(0, ((Number) delDenied.getBody().get("code")).intValue());
    }

    @Test
    void fromShortageCreatesOrder() {
        String admin = login("admin");
        // 造一个 D001 缺货：新备件 + 仅 1 件库存
        Part p = new Part();
        p.setPartNo("P-SHORT-T");
        p.setName("缺货测试件");
        p.setUnit("个");
        p.setCostPrice(new BigDecimal("10"));
        p.setSalePrice(new BigDecimal("20"));
        p.setMinStock(10);
        partMapper.insert(p);
        stockService.inbound("D001", "P-SHORT-T", "A-01-01", "B-T", 1);

        Map<String, Object> body = new HashMap<>();
        body.put("dealerCode", "D001");
        Map<String, Object> o = call(admin, "POST", "/api/procure/order/from-shortage", body);
        assertEquals("SHORTAGE", o.get("source"));
        assertEquals("DRAFT", o.get("status"));
        java.util.List<Object> lines =
                (java.util.List<Object>)
                        raw(admin, "GET", "/api/procure/order/" + o.get("id") + "/lines", null)
                                .getBody().get("data");
        boolean found = false;
        for (Object row : lines) {
            Map<String, Object> l = (Map<String, Object>) row;
            if ("P-SHORT-T".equals(l.get("partNo"))) {
                found = true;
                assertEquals(19, ((Number) l.get("qty")).intValue()); // min*2-avail = 20-1
            }
        }
        assertTrue(found, "缺货件应出现在采购明细");
    }

    @Test
    @SuppressWarnings("unchecked")
    void receiveGuardsAndInquiryRestore() {
        String sa = login("d001sa");
        String oem = login("oem");

        // 询价 → 报价 → 转订单（DRAFT, source=INQUIRY）
        Map<String, Object> inq = new HashMap<>();
        inq.put("dealerCode", "D001");
        inq.put("title", "还原测试询价");
        inq.put("lines", Collections.singletonList(
                new HashMap<String, Object>() {{ put("partNo", "P0004"); put("qty", 2); }}));
        Map<String, Object> created = call(sa, "POST", "/api/procure/inquiry", inq);
        Long iid = ((Number) created.get("id")).longValue();
        call(sa, "POST", "/api/procure/inquiry/" + iid + "/send", null);
        java.util.List<Object> iLines =
                (java.util.List<Object>)
                        raw(sa, "GET", "/api/procure/inquiry/" + iid + "/lines", null)
                                .getBody().get("data");
        Map<String, Object> q = new HashMap<>();
        java.util.List<Object> ql = new java.util.ArrayList<>();
        for (Object row : iLines) {
            Map<String, Object> qq = new HashMap<>();
            qq.put("lineId", ((Map<String, Object>) row).get("id"));
            qq.put("quotedPrice", 11.0);
            ql.add(qq);
        }
        q.put("lines", ql);
        call(oem, "POST", "/api/procure/inquiry/" + iid + "/quote", q);
        Map<String, Object> po = call(sa, "POST", "/api/procure/inquiry/" + iid + "/order", null);
        Long poid = ((Number) po.get("id")).longValue();

        // 删除草稿订单 → 询价还原为 QUOTED
        call(sa, "DELETE", "/api/procure/order/" + poid, null);
        Map<String, Object> inq2 =
                (Map<String, Object>) raw(sa, "GET", "/api/procure/inquiry/" + iid, null)
                        .getBody().get("data");
        assertEquals("QUOTED", inq2.get("status"));

        // 手工订单 + 确认 → 两次不带批次到货
        Map<String, Object> manual = new HashMap<>();
        manual.put("dealerCode", "D001");
        manual.put("lines", Collections.singletonList(
                new HashMap<String, Object>() {{ put("partNo", "P0004"); put("qty", 3); put("unitPrice", 5); }}));
        Map<String, Object> po2 = call(sa, "POST", "/api/procure/order", manual);
        Long poid2 = ((Number) po2.get("id")).longValue();
        call(sa, "POST", "/api/procure/order/" + poid2 + "/submit", null);
        call(oem, "POST", "/api/procure/order/" + poid2 + "/confirm", new HashMap<>());

        java.util.List<Object> poLines =
                (java.util.List<Object>)
                        raw(sa, "GET", "/api/procure/order/" + poid2 + "/lines", null)
                                .getBody().get("data");
        Long lid = ((Number) ((Map<String, Object>) poLines.get(0)).get("id")).longValue();

        // 非法数量：超过 int 上限被拒
        Map<String, Object> bad = new HashMap<>();
        bad.put("lines", Collections.singletonList(
                new HashMap<String, Object>() {{
                    put("orderLineId", lid);
                    put("qty", 3000000000L);
                }}));
        ResponseEntity<Map> badRes = raw(sa, "POST", "/api/procure/order/" + poid2 + "/receive", bad);
        assertNotEquals(0, ((Number) badRes.getBody().get("code")).intValue());

        // 两次不带 batchNo 到货：各自落到以其到货单号为批次，库存行不再为 NULL 批次
        int before = stockService.available("D001", "P0004");
        for (int i = 0; i < 2; i++) {
            Map<String, Object> rcv = new HashMap<>();
            rcv.put("location", "RCV-01");
            rcv.put("lines", Collections.singletonList(
                    new HashMap<String, Object>() {{ put("orderLineId", lid); put("qty", 1); }}));
            call(sa, "POST", "/api/procure/order/" + poid2 + "/receive", rcv);
        }
        assertEquals(before + 2, stockService.available("D001", "P0004"));

        // 收货单的 batchNo 默认等于 receipt_no
        java.util.List<Object> receipts =
                (java.util.List<Object>)
                        raw(sa, "GET", "/api/procure/order/" + poid2 + "/receipts", null)
                                .getBody().get("data");
        for (Object row : receipts) {
            Map<String, Object> r = (Map<String, Object>) row;
            assertEquals(r.get("receiptNo"), r.get("batchNo"));
            // 对应库存行批次非空
            com.dms.parts.entity.PartStock st =
                    stockService.find("D001", "P0004", "RCV-01", String.valueOf(r.get("batchNo")));
            assertNotNull(st);
        }
    }
}
