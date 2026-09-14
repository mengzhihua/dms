package com.dms.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_vehicle_model")
public class VehicleModel extends BaseEntity {
    @NotBlank(message = "车型编码必填")
    private String code;
    @NotBlank(message = "车型名称必填")
    private String name;
    private String brand;
    private String series;
    private Integer warrantyMonths;
    private Integer warrantyKm;
    private Integer maintenanceIntervalKm;
}
