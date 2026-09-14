package com.dms.invoice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_tax_config")
public class TaxConfig extends BaseEntity {
    private String dealerCode;
    private BigDecimal taxRate;
    private String sellerName;
    private String sellerTaxNo;
}
