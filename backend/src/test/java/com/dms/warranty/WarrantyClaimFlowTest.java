package com.dms.warranty;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.DmsApplication;
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
 * 保修索赔全流程：结算生成草稿 → 提交 → OEM 审核(含旧件回收) → 发货 → 签收 → 结算批次 → 付款。
 */
@SpringBootTest(
        classes = DmsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:warrantyflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
        })
class WarrantyClaimFlowTest {
    @Autowired TestRestTemplate http;

    private String login(String username) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", "123456");
        ResponseEntity<Map> r = http.postForEntity("/api/auth/login", body, Map.class);
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String token, String method, String path, Object body) {
        ResponseEntity<Map> r =
                http.exchange(
                        path,
                        HttpMethod.valueOf(method),
                        body == null ? auth(token) : auth(token, body),
                        Map.class);
        assertEquals(0, ((Number) r.getBody().get("code")).intValue(),
                path + " -> " + r.getBody());
        return (Map<String, Object>) r.getBody().get("data");
    }

    private ResponseEntity<Map> raw(String token, String method, String path, Object body) {
        return http.exchange(
                path,
                HttpMethod.valueOf(method),
                body == null ? auth(token) : auth(token, body),
                Map.class);
    }

    /** 走一条保修工单到结算，返回 {orderId, claimId, claim}。 */
    private Map<String, Object> settleWarrantyOrder(String admin) {
        Map<String, Object> veh = new HashMap<>();
        veh.put("vin", "VINWCLM" + System.nanoTime() % 1000000000L);
        veh.put("modelCode", "M001");
        veh.put("customerId", 1);
        veh.put("dealerCode", "D001");
        veh.put("plateNo", "沪T00001");
        Long vid = ((Number) call(admin, "POST", "/api/customer/vehicle", veh).get("id")).longValue();

        Map<String, Object> order = new HashMap<>();
        order.put("vehicleId", vid);
        order.put("mileageIn", 5000);
        order.put("orderType", "WARRANTY");
        order.put("complaint", "保修测试-发动机抖动");
        Long oid = ((Number) call(admin, "POST", "/api/workshop/order", order).get("id")).longValue();

        Map<String, Object> labor = new HashMap<>();
        labor.put("laborCode", "L001");
        labor.put("isWarranty", true);
        call(admin, "POST", "/api/workshop/order/" + oid + "/labor", labor);

        Map<String, Object> part = new HashMap<>();
        part.put("partNo", "P0001");
        part.put("qty", 1);
        part.put("isWarranty", true);
        Map<String, Object> partLine =
                call(admin, "POST", "/api/workshop/order/" + oid + "/part", part);

        Map<String, Object> diag = new HashMap<>();
        diag.put("diagnosis", "点火线圈故障");
        call(admin, "POST", "/api/workshop/order/" + oid + "/diagnose", diag);
        call(admin, "POST", "/api/workshop/order/" + oid + "/quote", new HashMap<>());
        call(admin, "POST", "/api/workshop/order/" + oid + "/approve", new HashMap<>());
        Map<String, Object> disp = new HashMap<>();
        disp.put("technicianCode", "T001");
        disp.put("bayCode", "B02");
        call(admin, "POST", "/api/workshop/order/" + oid + "/dispatch", disp);
        call(admin, "POST", "/api/workshop/order/" + oid + "/start", new HashMap<>());
        call(admin, "POST", "/api/workshop/order/" + oid + "/finish", new HashMap<>());
        call(admin, "POST", "/api/workshop/order/" + oid + "/qc", Collections.singletonMap("pass", true));
        call(admin, "POST", "/api/workshop/order/" + oid + "/settle", new HashMap<>());

        Map<String, Object> page =
                call(admin, "GET", "/api/warranty/claim/page?size=50", null);
        Long claimId = null;
        for (Object row : (java.util.List<Object>) page.get("records")) {
            Map<String, Object> r = (Map<String, Object>) row;
            if (r.get("orderId") != null
                    && oid.equals(((Number) r.get("orderId")).longValue())) {
                claimId = ((Number) r.get("id")).longValue();
            }
        }
        assertNotNull(claimId, "结算后应生成索赔草稿");
        Map<String, Object> res = new HashMap<>();
        res.put("orderId", oid);
        res.put("claimId", claimId);
        res.put("partLineId", ((Number) partLine.get("id")).longValue());
        return res;
    }

    @Test
    void fullFlowWithPartsReturnAndSettlement() {
        String admin = login("admin");
        Map<String, Object> ctx = settleWarrantyOrder(admin);
        Long claimId = (Long) ctx.get("claimId");

        Map<String, Object> draft =
                call(admin, "GET", "/api/warranty/claim/" + claimId, null);
        assertEquals("DRAFT", draft.get("status"));
        assertEquals("点火线圈故障", draft.get("faultDesc"));
        assertNotNull(draft.get("vin"));

        ResponseEntity<Map> lr =
                raw(admin, "GET", "/api/warranty/claim/" + claimId + "/lines", null);
        java.util.List<Object> rows = (java.util.List<Object>) lr.getBody().get("data");
        assertEquals(2, rows.size());
        Long partLineId = null;
        for (Object row : rows) {
            Map<String, Object> l = (Map<String, Object>) row;
            if ("PART".equals(l.get("lineType"))) {
                partLineId = ((Number) l.get("id")).longValue();
            }
        }
        assertNotNull(partLineId);

        call(admin, "POST", "/api/warranty/claim/" + claimId + "/submit", null);
        Map<String, Object> submitted =
                call(admin, "GET", "/api/warranty/claim/" + claimId, null);
        assertEquals("SUBMITTED", submitted.get("status"));
        assertNotNull(submitted.get("submittedAt"));

        Map<String, Object> approveBody = new HashMap<>();
        approveBody.put("returnLineIds", Collections.singletonList(partLineId));
        approveBody.put("oemRemark", "核准，旧件需回收");
        Map<String, Object> approved =
                call(admin, "POST", "/api/warranty/claim/" + claimId + "/approve", approveBody);
        assertEquals("PARTS_RETURNING", approved.get("status"));

        Map<String, Object> ship = new HashMap<>();
        ship.put("returnShipNo", "SF123456789");
        call(admin, "POST", "/api/warranty/claim/" + claimId + "/ship", ship);
        Map<String, Object> received =
                call(admin, "POST", "/api/warranty/claim/" + claimId + "/receive", null);
        assertEquals("APPROVED", received.get("status"));

        Map<String, Object> gen = new HashMap<>();
        gen.put("dealerCode", "D001");
        Map<String, Object> settlement =
                call(admin, "POST", "/api/warranty/settlement/generate", gen);
        assertEquals("DRAFT", settlement.get("status"));
        assertTrue(((Number) settlement.get("claimCount")).intValue() >= 1);
        Long sid = ((Number) settlement.get("id")).longValue();

        call(admin, "POST", "/api/warranty/settlement/" + sid + "/confirm", null);
        call(admin, "POST", "/api/warranty/settlement/" + sid + "/pay", null);
        Map<String, Object> paid =
                call(admin, "GET", "/api/warranty/claim/" + claimId, null);
        assertEquals("PAID", paid.get("status"));
    }

    @Test
    void returnThenResubmit() {
        String admin = login("admin");
        Map<String, Object> ctx = settleWarrantyOrder(admin);
        Long claimId = (Long) ctx.get("claimId");
        call(admin, "POST", "/api/warranty/claim/" + claimId + "/submit", null);

        ResponseEntity<Map> noReason =
                raw(admin, "POST", "/api/warranty/claim/" + claimId + "/return", new HashMap<>());
        assertNotEquals(0, ((Number) noReason.getBody().get("code")).intValue());

        Map<String, Object> back = new HashMap<>();
        back.put("oemRemark", "资料不全，退回补充");
        Map<String, Object> returned =
                call(admin, "POST", "/api/warranty/claim/" + claimId + "/return", back);
        assertEquals("RETURNED", returned.get("status"));

        Map<String, Object> edit = new HashMap<>();
        edit.put("faultDesc", "点火线圈故障（已补充检测报告）");
        call(admin, "PUT", "/api/warranty/claim/" + claimId, edit);
        call(admin, "POST", "/api/warranty/claim/" + claimId + "/submit", null);
        Map<String, Object> again =
                call(admin, "GET", "/api/warranty/claim/" + claimId, null);
        assertEquals("SUBMITTED", again.get("status"));
        assertEquals("点火线圈故障（已补充检测报告）", again.get("faultDesc"));
    }

    @Test
    void negativeChecks() {
        String admin = login("admin");
        // 无明细行不能提交
        Map<String, Object> empty = new HashMap<>();
        empty.put("claimNo", "WC-EMPTY-" + System.nanoTime() % 100000000L);
        empty.put("dealerCode", "D001");
        empty.put("faultDesc", "空索赔单");
        empty.put("amount", 100);
        empty.put("status", "DRAFT");
        Map<String, Object> emptyClaim = call(admin, "POST", "/api/warranty/claim", empty);
        Long emptyId = ((Number) emptyClaim.get("id")).longValue();
        ResponseEntity<Map> submitEmpty =
                raw(admin, "POST", "/api/warranty/claim/" + emptyId + "/submit", null);
        assertNotEquals(0, ((Number) submitEmpty.getBody().get("code")).intValue());

        Map<String, Object> ctx = settleWarrantyOrder(admin);
        Long claimId = (Long) ctx.get("claimId");

        // DRAFT 不能直接审核
        ResponseEntity<Map> approveDraft =
                raw(admin, "POST", "/api/warranty/claim/" + claimId + "/approve", new HashMap<>());
        assertNotEquals(0, ((Number) approveDraft.getBody().get("code")).intValue());

        call(admin, "POST", "/api/warranty/claim/" + claimId + "/submit", null);
        // 核准金额超过索赔金额
        Map<String, Object> tooMuch = new HashMap<>();
        tooMuch.put("approvedAmount", 99999999);
        ResponseEntity<Map> over =
                raw(admin, "POST", "/api/warranty/claim/" + claimId + "/approve", tooMuch);
        assertNotEquals(0, ((Number) over.getBody().get("code")).intValue());
        // 拒绝必须带原因
        ResponseEntity<Map> rejectNoReason =
                raw(admin, "POST", "/api/warranty/claim/" + claimId + "/reject", new HashMap<>());
        assertNotEquals(0, ((Number) rejectNoReason.getBody().get("code")).intValue());
        Map<String, Object> reject = new HashMap<>();
        reject.put("oemRemark", "不在保修范围");
        Map<String, Object> rejected =
                call(admin, "POST", "/api/warranty/claim/" + claimId + "/reject", reject);
        assertEquals("REJECTED", rejected.get("status"));
    }
}
