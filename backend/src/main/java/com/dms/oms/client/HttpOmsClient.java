package com.dms.oms.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

/** 通过 X-Api-Key 调用 OMS /api/open/channel/**,响应约定 {code,msg,data},code=0 成功。 */
public class HttpOmsClient implements OmsClient {
    private final String baseUrl;
    private final String apiKey;
    private final RestTemplate http;
    private final ObjectMapper om;

    public HttpOmsClient(String baseUrl, String apiKey, RestTemplate http, ObjectMapper om) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.http = http;
        this.om = om;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrder(Map<String, Object> request) {
        return (Map<String, Object>) data(exchange(HttpMethod.POST, "/api/open/channel/orders", request));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrder(String shopCode, String channelOrderNo) {
        String path = "/api/open/channel/orders/" + enc(shopCode) + "/" + enc(channelOrderNo);
        try {
            return (Map<String, Object>) data(exchange(HttpMethod.GET, path, null));
        } catch (OmsException e) {
            if (e.getMessage() != null && e.getMessage().contains("订单不存在")) {
                return null;
            }
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> cancelOrder(String shopCode, String channelOrderNo, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("shopCode", shopCode);
        body.put("channelOrderNo", channelOrderNo);
        body.put("reason", reason);
        return (Map<String, Object>) data(exchange(HttpMethod.POST, "/api/open/channel/orders/cancel", body));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> inventory(String shopCode, List<String> skus) {
        String path = "/api/open/channel/inventory?shopCode=" + enc(shopCode)
                + "&skus=" + enc(String.join(",", skus));
        Object d = data(exchange(HttpMethod.GET, path, null));
        return d instanceof List ? (List<Map<String, Object>>) d : new ArrayList<>();
    }

    private static String enc(String s) {
        return UriUtils.encodePathSegment(s, "UTF-8");
    }

    private Map<String, Object> exchange(HttpMethod method, String path, Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Api-Key", apiKey);
        String raw;
        try {
            raw = http.exchange(baseUrl + path, method, new HttpEntity<>(body, h), String.class).getBody();
        } catch (HttpStatusCodeException e) {
            raw = e.getResponseBodyAsString();
            if (raw == null || raw.isEmpty()) {
                throw new OmsException("OMS 调用失败 " + e.getStatusCode() + " " + path, e);
            }
        } catch (Exception e) {
            throw new OmsException("OMS 调用失败 " + path + ": " + e.getMessage(), e);
        }
        try {
            return om.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new OmsException("OMS 响应无法解析 " + path + ": " + raw, e);
        }
    }

    private static Object data(Map<String, Object> resp) {
        Object code = resp.get("code");
        if (code == null || !"0".equals(String.valueOf(code))) {
            throw new OmsException("OMS 返回错误: " + resp.get("msg"));
        }
        return resp.get("data");
    }
}
