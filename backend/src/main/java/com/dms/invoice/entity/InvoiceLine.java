package com.dms.invoice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_invoice_line")
public class InvoiceLine extends BaseEntity {
    private Long invoiceId;
    private String name;
    private String spec;
    private String unit;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private String taxCategoryCode;
}
