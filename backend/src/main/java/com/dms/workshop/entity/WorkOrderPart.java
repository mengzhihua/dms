package com.dms.workshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_work_order_part")
public class WorkOrderPart extends BaseEntity {
    private Long orderId;
    private String partNo;
    private String name;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private Boolean isWarranty;
    private Boolean reservedFlag;
    private Boolean consumedFlag;
}
