package com.dms.crm.service;

import com.dms.crm.entity.NotifyMessage;

/** 通知通道：短信/微信等实现（MOCK 或 HTTP）。 */
public interface NotifyChannel {

    String channel();

    SendResult send(NotifyMessage message);

    /** 发送结果：providerRef 为通道侧流水号，errorMsg 为失败原因。 */
    class SendResult {
        public boolean success;
        public String providerRef;
        public String errorMsg;

        public static SendResult ok(String providerRef) {
            SendResult r = new SendResult();
            r.success = true;
            r.providerRef = providerRef;
            return r;
        }

        public static SendResult fail(String errorMsg) {
            SendResult r = new SendResult();
            r.success = false;
            r.errorMsg = errorMsg;
            return r;
        }
    }
}
