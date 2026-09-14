package com.dms.workshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_work_order_labor")
public class WorkOrderLabor extends BaseEntity {
    private Long orderId;
    private String laborCode;
    private String name;
    private BigDecimal hours;
    private BigDecimal rate;
    private BigDecimal amount;
    private Boolean isWarranty;
}
