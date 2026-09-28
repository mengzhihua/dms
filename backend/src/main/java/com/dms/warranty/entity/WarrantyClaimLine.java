package com.dms.warranty.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_warranty_claim_line")
public class WarrantyClaimLine extends BaseEntity {
    private Long claimId;
    private String lineType; // LABOR/PART
    private String code;
    private String name;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal approvedAmount;
    private Boolean returnRequired;
}
