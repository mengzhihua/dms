package com.dms.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_vehicle")
public class Vehicle extends BaseEntity {
    private String vin;
    private String plateNo;
    private String modelCode;
    private Long customerId;
    private String dealerCode;
    private Integer mileage;
    private LocalDate purchaseDate;
    private LocalDate warrantyStart;
    private LocalDate warrantyEnd;
    private LocalDate lastServiceDate;
    private Integer nextServiceMileage;
}
