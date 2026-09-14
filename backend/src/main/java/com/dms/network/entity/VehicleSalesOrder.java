package com.dms.network.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_vehicle_sales_order")
public class VehicleSalesOrder extends BaseEntity {
    private String orderNo;
    private String dealerCode;
    private Long customerId;
    private String modelCode;
    private String vin;
    private String color;
    private BigDecimal price;
    private BigDecimal deposit;
    private String status; // NEW/ALLOCATED/INVOICED/DELIVERED/CANCELLED
}
