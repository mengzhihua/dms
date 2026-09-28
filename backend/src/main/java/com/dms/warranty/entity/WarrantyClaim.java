package com.dms.warranty.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_warranty_claim")
public class WarrantyClaim extends BaseEntity {
    private String claimNo;
    private Long orderId;
    private String dealerCode;
    private String vin;
    private String plateNo;
    private Integer mileage;
    private LocalDate repairDate;
    private String faultCode;
    private String faultDesc;
    private BigDecimal amount;
    private BigDecimal laborAmount;
    private BigDecimal partAmount;
    private BigDecimal approvedAmount;
    private String oemRemark;
    private Boolean partsReturnRequired;
    private String returnShipNo;
    private LocalDateTime returnShippedAt;
    private LocalDateTime returnReceivedAt;
    private Long settlementId;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    /** DRAFT/SUBMITTED/RETURNED/REJECTED/PARTS_RETURNING/PARTS_SHIPPED/APPROVED/SETTLED/PAID */
    private String status;
}
