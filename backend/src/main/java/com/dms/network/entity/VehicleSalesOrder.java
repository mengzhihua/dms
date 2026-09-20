package com.dms.network.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_vehicle_sales_order")
public class VehicleSalesOrder extends BaseEntity {
    private String orderNo;
    private String dealerCode;
    @NotNull(message = "客户必填")
    private Long customerId;
    @NotBlank(message = "车型必填")
    private String modelCode;
    private String vin;
    private String color;
    @NotNull(message = "车价必填")
    @DecimalMin(value = "0.01", message = "车价必须大于0")
    private BigDecimal price;
    private BigDecimal deposit;
    private String status; // NEW/ALLOCATED/INVOICED/DELIVERED/CANCELLED
    private String paymentType; // FULL/LOAN
    private String loanProvider;
    private BigDecimal loanAmount;
    private Integer loanTermMonths;
    private String loanStatus; // NONE/APPLIED/APPROVED/REJECTED
    private String insuranceCompany;
    private String insurancePolicyNo;
    private BigDecimal insuranceAmount;
    private String insuranceStatus; // NONE/ISSUED
    private BigDecimal paidAmount;
    private Long invoiceId;
    private Long surveyId;
    private Boolean pdiPassed;
    private java.time.LocalDateTime deliveredAt;
    private String deliverRemark;
}
