package com.dms.procure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_purchase_receipt_line")
public class PurchaseReceiptLine extends BaseEntity {
    private Long receiptId;
    private Long orderLineId;
    private String partNo;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
