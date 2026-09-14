package com.dms.parts.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_stock_movement")
public class StockMovement extends BaseEntity {
    private String dealerCode;
    private String partNo;
    private String type; // IN/OUT/RESERVE/RELEASE/ADJUST
    private Integer qty;
    private String refType;
    private String refNo;
    private String location;
    private String batchNo;
}
