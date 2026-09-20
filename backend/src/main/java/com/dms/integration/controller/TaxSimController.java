package com.dms.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 进程内模拟税控平台，仅 tax-sim profile 生效，用于 HTTP 网关联调。
 * POST /sim/tax/issue|/query|/red-flush，校验 X-App-Id/X-Timestamp/X-Nonce/X-Sign。
 * remark 含 "PENDING" → 受理中（query 后返回已开）；含 "FAIL" → 拒绝。
 */
@Profile("tax-sim")
@RestController
@RequestMapping("/sim/tax")
public class TaxSimController {
    private final String appId;
    private final String appSecret;
    private final ObjectMapper om = new ObjectMapper();
    private final AtomicLong seq = new AtomicLong(20000001);
    private final Map<String, Map<String, Object>> pendingIssued = new ConcurrentHashMap<>();

    public TaxSimController(
            @Value("${dms.tax.app-id:}") String appId,
            @Value("${dms.taxsim.expected-secret:${dms.tax.app-secret:}}") String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    @PostMapping("/issue")
    public ResponseEntity<Map<String, Object>> issue(
            @RequestBody String body, HttpServletRequest req) {
        if (!verify(req, body)) {
            return badSign();
        }
        Map<String, Object> res = new HashMap<>();
        String remark = text(body, "remark");
        if (remark != null && remark.contains("FAIL")) {
            res.put("success", false);
            res.put("errorMsg", "模拟税控拒绝");
            return ResponseEntity.ok(res);
        }
        String ref = UUID.randomUUID().toString();
        if (remark != null && remark.contains("PENDING")) {
            res.put("success", true);
            res.put("pending", true);
            res.put("providerRef", ref);
            pendingIssued.put(ref, successResult());
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.ok(successResult());
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(
            @RequestBody String body, HttpServletRequest req) {
        if (!verify(req, body)) {
            return badSign();
        }
        String ref = text(body, "providerRef");
        Map<String, Object> res = pendingIssued.remove(ref);
        if (res == null) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("errorMsg", "providerRef 不存在或已完成");
            return ResponseEntity.ok(m);
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/red-flush")
    public ResponseEntity<Map<String, Object>> redFlush(
            @RequestBody String body, HttpServletRequest req) {
        if (!verify(req, body)) {
            return badSign();
        }
        return ResponseEntity.ok(successResult());
    }

    private Map<String, Object> successResult() {
        String number = String.format("%08d", seq.getAndIncrement());
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("code", "044001900111");
        res.put("number", number);
        res.put("checkCode", String.format("%020d", seq.get() * 97L + 13));
        res.put("pdfUrl", "http://sim/pdf/" + number + ".pdf");
        res.put("providerRef", UUID.randomUUID().toString());
        return res;
    }

    private String text(String body, String field) {
        try {
            JsonNode n = om.readTree(body);
            JsonNode v = n.path(field);
            return v.isNull() || v.isMissingNode() ? null : v.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<Map<String, Object>> badSign() {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("errorMsg", "bad sign");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(m);
    }

    private boolean verify(HttpServletRequest req, String body) {
        String id = req.getHeader("X-App-Id");
        String ts = req.getHeader("X-Timestamp");
        String nonce = req.getHeader("X-Nonce");
        String sign = req.getHeader("X-Sign");
        if (id == null || ts == null || nonce == null || sign == null || !id.equals(appId)) {
            return false;
        }
        try {
            String data = id + "\n" + ts + "\n" + nonce + "\n" + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] d = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString().equalsIgnoreCase(sign);
        } catch (Exception e) {
            return false;
        }
    }
}
