package com.dms.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_customer")
public class Customer extends BaseEntity {
    private String name;
    private String phone;
    private String idNo;
    private String gender;
    private String level;
    private String dealerCode;
}
