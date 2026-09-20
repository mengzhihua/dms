package com.dms.invoice;

import static org.junit.jupiter.api.Assertions.*;

import com.dms.invoice.entity.Invoice;
import com.dms.invoice.entity.InvoiceLine;
import com.dms.invoice.service.HttpTaxAdapter;
import com.dms.invoice.service.TaxInvoiceGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpTaxAdapterTest {
    private static final String APP_ID = "dms-app";
    private static final String SECRET = "test-tax-app-secret-0123456789abcdef";
    private final ObjectMapper om = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastSign = new AtomicReference<>();
    private final AtomicReference<String> lastTs = new AtomicReference<>();
    private final AtomicReference<String> lastNonce = new AtomicReference<>();
    private final AtomicReference<String> lastAppId = new AtomicReference<>();

    private HttpTaxAdapter start(String responseBody, int status) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/tax",
                exchange -> {
                    lastAppId.set(exchange.getRequestHeaders().getFirst("X-App-Id"));
                    lastTs.set(exchange.getRequestHeaders().getFirst("X-Timestamp"));
                    lastNonce.set(exchange.getRequestHeaders().getFirst("X-Nonce"));
                    lastSign.set(exchange.getRequestHeaders().getFirst("X-Sign"));
                    byte[] in = exchange.getRequestBody().readAllBytes();
                    lastBody.set(new String(in, StandardCharsets.UTF_8));
                    byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, out.length);
                    exchange.getResponseBody().write(out);
                    exchange.close();
                });
        server.start();
        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/tax";
        return new HttpTaxAdapter(endpoint, APP_ID, SECRET, "HMAC", 5000, "http://cb/x");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private Invoice sampleInvoice() {
        Invoice inv = new Invoice();
        inv.setInvoiceNo("INV-TEST-1");
        inv.setInvoiceType("ELECTRONIC");
        inv.setBuyerName("测试买家");
        inv.setBuyerTaxNo("9131TEST");
        inv.setSellerName("测试卖家");
        inv.setSellerTaxNo("9131SELL");
        inv.setAmount(new BigDecimal("113.00"));
        inv.setTaxAmount(new BigDecimal("13.00"));
        inv.setNetAmount(new BigDecimal("100.00"));
        inv.setTaxRate(new BigDecimal("0.13"));
        return inv;
    }

    private List<InvoiceLine> sampleLines() {
        List<InvoiceLine> ls = new ArrayList<>();
        InvoiceLine l = new InvoiceLine();
        l.setName("维修工时");
        l.setUnit("次");
        l.setQty(BigDecimal.ONE);
        l.setUnitPrice(new BigDecimal("113"));
        l.setAmount(new BigDecimal("113"));
        l.setTaxRate(new BigDecimal("0.13"));
        l.setTaxAmount(new BigDecimal("13"));
        l.setTaxCategoryCode("3040502000000000000");
        ls.add(l);
        return ls;
    }

    private String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] d = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(d.length * 2);
        for (byte b : d) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    @Test
    void signedRequestAndBody() throws Exception {
        HttpTaxAdapter ad = start("{\"success\":true,\"code\":\"044001900111\",\"number\":\"12345678\"}", 200);
        TaxInvoiceGateway.IssueResult r = ad.issue(sampleInvoice(), sampleLines());
        assertTrue(r.isSuccess());
        assertEquals("12345678", r.getNumber());

        assertEquals(APP_ID, lastAppId.get());
        assertNotNull(lastTs.get());
        assertNotNull(lastNonce.get());
        String expected =
                hmac(APP_ID + "\n" + lastTs.get() + "\n" + lastNonce.get() + "\n" + lastBody.get());
        assertEquals(expected, lastSign.get());

        JsonNode n = om.readTree(lastBody.get());
        assertEquals("INV-TEST-1", n.path("requestId").asText());
        assertTrue(n.path("lines").isArray());
        assertEquals(
                "3040502000000000000", n.path("lines").get(0).path("taxCategoryCode").asText());
        assertEquals("http://cb/x", n.path("callbackUrl").asText());
        assertEquals("测试买家", n.path("buyer").path("name").asText());
    }

    @Test
    void httpErrorMapped() throws Exception {
        HttpTaxAdapter ad = start("{\"detail\":\"provider exploded\"}", 500);
        TaxInvoiceGateway.IssueResult r = ad.issue(sampleInvoice(), sampleLines());
        assertFalse(r.isSuccess());
        assertTrue(r.getErrorMsg().startsWith("HTTP 500"));
    }

    @Test
    void pendingResponse() throws Exception {
        HttpTaxAdapter ad =
                start("{\"success\":true,\"pending\":true,\"providerRef\":\"ref-1\"}", 200);
        TaxInvoiceGateway.IssueResult r = ad.issue(sampleInvoice(), sampleLines());
        assertTrue(r.isSuccess());
        assertTrue(r.isPending());
        assertEquals("ref-1", r.getProviderRef());
    }
}
