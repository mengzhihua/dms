package com.dms.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_vehicle_model")
public class VehicleModel extends BaseEntity {
    private String code;
    private String name;
    private String brand;
    private String series;
    private Integer warrantyMonths;
    private Integer warrantyKm;
    private Integer maintenanceIntervalKm;
}
