package com.dms.crm.service;

import com.dms.crm.entity.NotifyMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
 * HTTP 通知通道骨架（短信/微信共用）：POST JSON 到 endpoint，
 * 签名与税控 HTTP 适配一致：X-App-Id + X-Timestamp + X-Nonce +
 * X-Sign = hex(HMAC-SHA256(appSecret, appId + "\n" + ts + "\n" + nonce + "\n" + body))。
 * 响应约定：{success, providerRef?, errorMsg?}。
 */
public class HttpNotifyChannel implements NotifyChannel {
    private final String channel;
    private final String endpoint;
    private final String appId;
    private final String appSecret;
    private final String extra; // 短信签名 / 微信模板ID
    private final RestTemplate http;
    private final ObjectMapper om = new ObjectMapper();

    public HttpNotifyChannel(
            String channel,
            String endpoint,
            String appId,
            String appSecret,
            String extra,
            int timeoutMs) {
        this.channel = channel;
        this.endpoint = endpoint;
        this.appId = appId;
        this.appSecret = appSecret;
        this.extra = extra;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(timeoutMs);
        f.setReadTimeout(timeoutMs);
        this.http = new RestTemplate(f);
    }

    @Override
    public String channel() {
        return channel;
    }

    @Override
    public SendResult send(NotifyMessage m) {
        if (endpoint == null || endpoint.isEmpty()) {
            return SendResult.fail("通知通道 endpoint 未配置");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel);
        payload.put("receiver", m.getReceiver());
        payload.put("templateCode", m.getTemplateCode());
        payload.put("content", m.getContent());
        if ("SMS".equals(channel)) {
            payload.put("signName", extra);
        } else {
            payload.put("templateId", extra);
        }
        try {
            String body = om.writeValueAsString(payload);
            String ts = String.valueOf(System.currentTimeMillis());
            String nonce = UUID.randomUUID().toString();
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.set("X-App-Id", appId);
            h.set("X-Timestamp", ts);
            h.set("X-Nonce", nonce);
            h.set("X-Sign", hmac(appId + "\n" + ts + "\n" + nonce + "\n" + body));
            String resp =
                    http.postForObject(
                            endpoint + "/send",
                            new HttpEntity<>(body, h),
                            String.class);
            JsonNode n = om.readTree(resp);
            if (n.path("success").asBoolean()) {
                return SendResult.ok(n.path("providerRef").asText(null));
            }
            return SendResult.fail(n.path("errorMsg").asText("通道返回失败"));
        } catch (HttpStatusCodeException e) {
            String s = e.getResponseBodyAsString();
            return SendResult.fail(
                    "HTTP " + e.getRawStatusCode() + ": "
                            + (s.length() > 200 ? s.substring(0, 200) : s));
        } catch (Exception e) {
            return SendResult.fail("通道调用异常: " + e.getMessage());
        }
    }

    private String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(
                new SecretKeySpec(
                        (appSecret == null ? "" : appSecret).getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"));
        byte[] b = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }
}
