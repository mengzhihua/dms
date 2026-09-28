package com.dms.crm.service;

import com.dms.crm.entity.NotifyMessage;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 模拟短信通道：按 mockFailRate 概率失败，否则成功返回流水号。 */
public class MockSmsChannel implements NotifyChannel {
    private static final Logger log = LoggerFactory.getLogger(MockSmsChannel.class);
    private final double failRate;

    public MockSmsChannel(double failRate) {
        this.failRate = failRate;
    }

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public SendResult send(NotifyMessage m) {
        if (Math.random() < failRate) {
            return SendResult.fail("模拟短信网关失败");
        }
        String ref = "MOCK-SMS-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[MOCK-SMS] -> {} : {}", m.getReceiver(), m.getContent());
        return SendResult.ok(ref);
    }
}
