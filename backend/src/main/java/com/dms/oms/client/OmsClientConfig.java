package com.dms.oms.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OmsClientConfig {
    @Bean
    public OmsClient omsClient(
            @Value("${dms.oms.mock:true}") boolean mock,
            @Value("${dms.oms.base-url:}") String baseUrl,
            @Value("${dms.oms.api-key:}") String apiKey,
            @Value("${dms.oms.timeout-ms:5000}") long timeoutMs,
            RestTemplateBuilder builder,
            ObjectMapper om) {
        if (mock || baseUrl == null || baseUrl.trim().isEmpty()) {
            return new MockOmsClient();
        }
        return new HttpOmsClient(
                baseUrl.trim(),
                apiKey,
                builder.setConnectTimeout(Duration.ofMillis(timeoutMs))
                        .setReadTimeout(Duration.ofMillis(timeoutMs))
                        .build(),
                om);
    }
}
