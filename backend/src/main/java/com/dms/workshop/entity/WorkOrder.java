package com.dms.workshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_work_order")
public class WorkOrder extends BaseEntity {
    private String orderNo;
    private String dealerCode;
    private Long appointmentId;
    private Long customerId;
    private Long vehicleId;
    private String vin;
    private String plateNo;
    private Integer mileageIn;
    private String fuelLevel;
    private String serviceType;
    private String orderType; // REGULAR/WARRANTY/INSURANCE/INTERNAL
    private String complaint;
    private String diagnosis;
    private String technicianCode;
    private String bayCode;
    private String advisorName;
    private String status;
    private LocalDateTime checkInTime;
    private LocalDateTime quoteTime;
    private LocalDateTime approveTime;
    private LocalDateTime dispatchTime;
    private LocalDateTime repairStartTime;
    private LocalDateTime repairEndTime;
    private LocalDateTime qcTime;
    private LocalDateTime settleTime;
    private LocalDateTime deliverTime;
    private BigDecimal laborAmount;
    private BigDecimal partsAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal warrantyAmount;
    private BigDecimal customerPayable;
    private String qcResult;
    private String qcRemark;
    private Long invoiceId;
    private Long surveyId;
}
