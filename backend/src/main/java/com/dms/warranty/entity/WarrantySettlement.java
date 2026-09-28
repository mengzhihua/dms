package com.dms.warranty.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_warranty_settlement")
public class WarrantySettlement extends BaseEntity {
    private String settlementNo;
    private String dealerCode;
    private String period;
    private Integer claimCount;
    private BigDecimal totalAmount;
    private String status; // DRAFT/CONFIRMED/PAID
    private LocalDateTime paidAt;
}
