package com.dms.network.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_vehicle_stock")
public class VehicleStock extends BaseEntity {
    private String dealerCode;
    private String vin;
    private String modelCode;
    private String color;
    private String status; // IN_STOCK/ALLOCATED/SOLD
}
