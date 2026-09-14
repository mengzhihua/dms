package com.dms.workshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_warranty_claim")
public class WarrantyClaim extends BaseEntity {
    private String claimNo;
    private Long orderId;
    private String dealerCode;
    private BigDecimal amount;
    private String status; // SUBMITTED/APPROVED/REJECTED/PAID
}
