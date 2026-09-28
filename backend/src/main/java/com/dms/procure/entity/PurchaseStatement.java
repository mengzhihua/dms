package com.dms.procure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_purchase_statement")
public class PurchaseStatement extends BaseEntity {
    private String statementNo;
    private String dealerCode;
    private String period;
    private Integer orderCount;
    private BigDecimal totalAmount;
    private String status; // DRAFT/CONFIRMED/PAID
    private LocalDateTime paidAt;
}
