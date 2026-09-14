package com.dms.parts.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_part")
public class Part extends BaseEntity {
    @NotBlank(message = "备件号必填")
    private String partNo;
    @NotBlank(message = "备件名称必填")
    private String name;
    private String category;
    private String unit;
    private BigDecimal costPrice;
    private BigDecimal salePrice;
    private BigDecimal taxRate;
    private Integer minStock;
}
