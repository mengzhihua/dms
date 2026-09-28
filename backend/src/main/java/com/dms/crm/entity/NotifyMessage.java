package com.dms.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 通知发送记录：短信/微信渠道。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_notify_message")
public class NotifyMessage extends BaseEntity {
    private String dealerCode;
    private String channel; // SMS/WECHAT
    private String receiver;
    private String templateCode;
    private String content;
    private String bizType;
    private Long bizId;
    private String status; // PENDING/SENT/FAILED
    private String providerRef;
    private String errorMsg;
    private LocalDateTime sentAt;
}
