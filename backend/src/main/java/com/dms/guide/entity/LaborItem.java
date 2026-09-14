package com.dms.guide.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_labor_item")
public class LaborItem extends BaseEntity {
    @NotBlank(message = "工时编码必填")
    private String code;
    @NotBlank(message = "工时名称必填")
    private String name;
    private BigDecimal standardHours;
    private String category;
}
