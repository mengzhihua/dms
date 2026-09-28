package com.dms.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 客户跟进任务：保养提醒/售后回访/投诉跟进/销售回访/生日关怀/续保提醒/手工任务。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_follow_task")
public class FollowTask extends BaseEntity {
    private String taskNo;
    private String dealerCode;
    private Long customerId;
    private Long vehicleId;
    private String vin;
    private String plateNo;
    private String customerName;
    private String phone;
    private String type;
    private String source; // AUTO/MANUAL
    private String sourceRef;
    private String title;
    private String content;
    private LocalDate dueDate;
    private String status; // PENDING/DONE/CANCELLED
    private String assignee;
    private String result;
    private LocalDateTime doneAt;
}
