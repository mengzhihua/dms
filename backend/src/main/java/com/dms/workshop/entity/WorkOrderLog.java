package com.dms.workshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_work_order_log")
public class WorkOrderLog extends BaseEntity {
    private Long orderId;
    private String fromStatus;
    private String toStatus;
    private String operator;
    private LocalDateTime logTime;
}
