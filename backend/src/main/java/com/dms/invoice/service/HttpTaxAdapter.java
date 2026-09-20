package com.dms.invoice.service;

import com.dms.invoice.entity.Invoice;
import com.dms.invoice.entity.InvoiceLine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 税控对接：POST TaxRequest JSON 到 dms.tax.endpoint。
 * 签名（sign-mode=HMAC，默认）：X-App-Id + X-Timestamp + X-Nonce +
 * X-Sign = hex(HMAC-SHA256(appSecret, appId + "\n" + ts + "\n" + nonce + "\n" + body))。
 * sign-mode=SECRET_HEADER：仅携带 X-App-Secret 头。
 * 响应约定：{success, pending?, code, number, checkCode, pdfUrl, providerRef, errorMsg}
 */
public class HttpTaxAdapter implements TaxInvoiceGateway {
    private final String endpoint;
    private final String appId;
    private final String appSecret;
    private final String signMode;
    private final String callbackUrl;
    private final RestTemplate http;
    private final ObjectMapper om = new ObjectMapper();

    public HttpTaxAdapter(
            String endpoint,
            String appId,
            String appSecret,
            String signMode,
            int timeoutMs,
            String callbackUrl) {
        this.endpoint = endpoint;
        this.appId = appId;
        this.appSecret = appSecret;
        this.signMode = signMode == null ? "HMAC" : signMode;
        this.callbackUrl = callbackUrl;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(timeoutMs);
        f.setReadTimeout(timeoutMs);
        this.http = new RestTemplate(f);
    }

    @Override
    public IssueResult issue(Invoice invoice, List<InvoiceLine> lines) {
        return call(endpoint + "/issue", TaxRequest.of(invoice, lines, callbackUrl));
    }

    @Override
    public IssueResult query(String providerRef) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("providerRef", providerRef);
        return call(endpoint + "/query", body);
    }

    @Override
    public IssueResult redFlush(Invoice original, Invoice redInvoice, List<InvoiceLine> redLines) {
        TaxRequest req = TaxRequest.of(redInvoice, redLines, callbackUrl);
        TaxRequest.Ref ref = new TaxRequest.Ref();
        ref.setCode(original.getTaxInvoiceCode());
        ref.setNumber(original.getTaxInvoiceNumber());
        req.setRedOf(ref);
        return call(endpoint + "/red-flush", req);
    }

    private IssueResult call(String url, Object payload) {
        try {
            String body = om.writeValueAsString(payload);
            return parse(postRaw(url, body));
        } catch (HttpStatusCodeException e) {
            return fail(
                    "HTTP "
                            + e.getStatusCode().value()
                            + ": "
                            + truncate(e.getResponseBodyAsString()));
        } catch (Exception e) {
            return fail(e.getMessage());
        }
    }

    private IssueResult fail(String msg) {
        IssueResult r = new IssueResult();
        r.setSuccess(false);
        r.setErrorMsg(msg == null ? "未知错误" : truncate(msg));
        return r;
    }

    private static String truncate(String s) {
        return s == null ? "" : (s.length() > 200 ? s.substring(0, 200) : s);
    }

    private String postRaw(String url, String json) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-App-Id", appId);
        if ("SECRET_HEADER".equalsIgnoreCase(signMode)) {
            h.set("X-App-Secret", appSecret);
        } else {
            String ts = String.valueOf(System.currentTimeMillis());
            String nonce = UUID.randomUUID().toString();
            h.set("X-Timestamp", ts);
            h.set("X-Nonce", nonce);
            h.set("X-Sign", sign(ts, nonce, json));
        }
        return http.postForObject(url, new HttpEntity<>(json, h), String.class);
    }

    private String sign(String ts, String nonce, String body) throws Exception {
        String data = appId + "\n" + ts + "\n" + nonce + "\n" + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private IssueResult parse(String body) throws Exception {
        JsonNode n = om.readTree(body);
        IssueResult r = new IssueResult();
        r.setSuccess(n.path("success").asBoolean());
        r.setPending(n.path("pending").asBoolean());
        r.setCode(n.path("code").asText(null));
        r.setNumber(n.path("number").asText(null));
        r.setCheckCode(n.path("checkCode").asText(null));
        r.setPdfUrl(n.path("pdfUrl").asText(null));
        r.setProviderRef(n.path("providerRef").asText(null));
        r.setErrorMsg(n.path("errorMsg").asText(null));
        return r;
    }
}
