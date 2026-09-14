package com.dms.guide.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_labor_item")
public class LaborItem extends BaseEntity {
    private String code;
    private String name;
    private BigDecimal standardHours;
    private String category;
}
