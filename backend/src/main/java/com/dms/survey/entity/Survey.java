package com.dms.survey.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_survey")
public class Survey extends BaseEntity {
    private String surveyNo;
    private String templateCode;
    private String dealerCode;
    private Long customerId;
    private Long orderId;
    private Long salesOrderId;
    private String channel; // SMS/APP/PHONE
    private String status; // PENDING/ANSWERED/EXPIRED
    private LocalDateTime sentTime;
    private LocalDateTime answeredTime;
    private BigDecimal totalScore;
    private BigDecimal npsScore;
}
