package com.dms.survey.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_complaint")
public class Complaint extends BaseEntity {
    private String complaintNo;
    private String dealerCode;
    private Long customerId;
    private Long surveyId;
    private Long orderId;
    private String content;
    private String level; // HIGH/MEDIUM
    private String status; // OPEN/PROCESSING/RESOLVED/CLOSED
    private String handler;
    private String resolution;
}
