package com.dms.invoice.service;

import com.dms.invoice.entity.Invoice;
import com.dms.invoice.entity.InvoiceLine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 税控对接骨架：POST JSON 到 dms.tax.endpoint，携带 appId/appSecret 头。
 * 约定响应：{ "success": true, "code": "发票代码", "number": "发票号码",
 *            "checkCode": "校验码", "pdfUrl": "...", "providerRef": "...", "errorMsg": "" }
 */
public class HttpTaxAdapter implements TaxInvoiceGateway {
    private final String endpoint;
    private final String appId;
    private final String appSecret;
    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    public HttpTaxAdapter(String endpoint, String appId, String appSecret) {
        this.endpoint = endpoint;
        this.appId = appId;
        this.appSecret = appSecret;
    }

    @Override
    public IssueResult issue(Invoice invoice, List<InvoiceLine> lines) {
        return post(endpoint + "/issue", invoice);
    }

    @Override
    public IssueResult query(String providerRef) {
        try {
            String body = "{\"providerRef\":\"" + providerRef + "\"}";
            return parse(postRaw(endpoint + "/query", body));
        } catch (Exception e) {
            IssueResult r = new IssueResult();
            r.setSuccess(false);
            r.setErrorMsg(e.getMessage());
            return r;
        }
    }

    @Override
    public IssueResult redFlush(Invoice original, Invoice redInvoice, List<InvoiceLine> redLines) {
        return post(endpoint + "/red-flush", redInvoice);
    }

    private IssueResult post(String url, Invoice invoice) {
        try {
            return parse(postRaw(url, om.writeValueAsString(invoice)));
        } catch (Exception e) {
            IssueResult r = new IssueResult();
            r.setSuccess(false);
            r.setErrorMsg(e.getMessage());
            return r;
        }
    }

    private String postRaw(String url, String json) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-App-Id", appId);
        h.set("X-App-Secret", appSecret);
        return http.postForObject(url, new HttpEntity<>(json, h), String.class);
    }

    private IssueResult parse(String body) throws Exception {
        JsonNode n = om.readTree(body);
        IssueResult r = new IssueResult();
        r.setSuccess(n.path("success").asBoolean());
        r.setCode(n.path("code").asText(null));
        r.setNumber(n.path("number").asText(null));
        r.setCheckCode(n.path("checkCode").asText(null));
        r.setPdfUrl(n.path("pdfUrl").asText(null));
        r.setProviderRef(n.path("providerRef").asText(null));
        r.setErrorMsg(n.path("errorMsg").asText(null));
        return r;
    }
}
