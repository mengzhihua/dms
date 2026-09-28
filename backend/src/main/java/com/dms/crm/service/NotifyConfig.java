package com.dms.crm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 通知通道装配：dms.notify.provider=MOCK(默认)/HTTP。 */
@Configuration
public class NotifyConfig {

    @Bean
    public NotifyChannel smsChannel(
            @Value("${dms.notify.provider:MOCK}") String provider,
            @Value("${dms.notify.mock-fail-rate:0}") double failRate,
            @Value("${dms.notify.sms.endpoint:}") String endpoint,
            @Value("${dms.notify.sms.app-id:}") String appId,
            @Value("${dms.notify.sms.app-secret:}") String appSecret,
            @Value("${dms.notify.sms.sign-name:DMS经销商}") String signName,
            @Value("${dms.notify.timeout-ms:10000}") int timeoutMs) {
        if ("HTTP".equalsIgnoreCase(provider)) {
            return new HttpNotifyChannel("SMS", endpoint, appId, appSecret, signName, timeoutMs);
        }
        return new MockSmsChannel(failRate);
    }

    @Bean
    public NotifyChannel wechatChannel(
            @Value("${dms.notify.provider:MOCK}") String provider,
            @Value("${dms.notify.mock-fail-rate:0}") double failRate,
            @Value("${dms.notify.wechat.endpoint:}") String endpoint,
            @Value("${dms.notify.wechat.app-id:}") String appId,
            @Value("${dms.notify.wechat.app-secret:}") String appSecret,
            @Value("${dms.notify.wechat.template-id:}") String templateId,
            @Value("${dms.notify.timeout-ms:10000}") int timeoutMs) {
        if ("HTTP".equalsIgnoreCase(provider)) {
            return new HttpNotifyChannel("WECHAT", endpoint, appId, appSecret, templateId, timeoutMs);
        }
        return new MockWechatChannel(failRate);
    }
}
