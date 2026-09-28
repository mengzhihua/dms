package com.dms.procure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_purchase_inquiry_line")
public class PurchaseInquiryLine extends BaseEntity {
    private Long inquiryId;
    private String partNo;
    private String name;
    private Integer qty;
    private BigDecimal targetPrice;
    private BigDecimal quotedPrice;
    private Integer leadDays;
}
