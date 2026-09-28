package com.dms.procure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_purchase_order_line")
public class PurchaseOrderLine extends BaseEntity {
    private Long orderId;
    private String partNo;
    private String name;
    private Integer qty;
    private Integer receivedQty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
