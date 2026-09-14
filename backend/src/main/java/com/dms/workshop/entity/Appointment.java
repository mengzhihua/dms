package com.dms.workshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_appointment")
public class Appointment extends BaseEntity {
    private String dealerCode;
    private Long customerId;
    private Long vehicleId;
    private LocalDateTime appointmentTime;
    private String serviceType;
    private String status; // BOOKED/ARRIVED/CANCELLED/NO_SHOW
}
