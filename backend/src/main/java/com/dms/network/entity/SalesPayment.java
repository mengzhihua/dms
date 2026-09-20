package com.dms.network.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_sales_payment")
public class SalesPayment extends BaseEntity {
    private Long orderId;
    private String dealerCode;
    private String payType; // DEPOSIT/BALANCE/LOAN/INSURANCE
    private BigDecimal amount;
    private String method; // CASH/TRANSFER/POS/LOAN
    private LocalDateTime paidAt;
}
