package com.dms.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_customer")
public class Customer extends BaseEntity {
    @NotBlank(message = "客户姓名必填")
    private String name;
    @NotBlank(message = "手机号必填")
    private String phone;
    private String idNo;
    private String gender;
    private String level;
    private String dealerCode;
}
