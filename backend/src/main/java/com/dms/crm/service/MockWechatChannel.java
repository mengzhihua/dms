package com.dms.crm.service;

import com.dms.crm.entity.NotifyMessage;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 模拟微信模板消息通道。 */
public class MockWechatChannel implements NotifyChannel {
    private static final Logger log = LoggerFactory.getLogger(MockWechatChannel.class);
    private final double failRate;

    public MockWechatChannel(double failRate) {
        this.failRate = failRate;
    }

    @Override
    public String channel() {
        return "WECHAT";
    }

    @Override
    public SendResult send(NotifyMessage m) {
        if (Math.random() < failRate) {
            return SendResult.fail("模拟微信网关失败");
        }
        String ref = "MOCK-WX-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[MOCK-WECHAT] -> {} : {}", m.getReceiver(), m.getContent());
        return SendResult.ok(ref);
    }
}
