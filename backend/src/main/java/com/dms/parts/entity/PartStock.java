package com.dms.parts.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_part_stock")
public class PartStock extends BaseEntity {
    private String dealerCode;
    private String partNo;
    private String location;
    private String batchNo;
    private Integer qty;
    private Integer reservedQty;
}
