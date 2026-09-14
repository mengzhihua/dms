package com.dms.network.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_dealer")
public class Dealer extends BaseEntity {
    private String code;
    private String name;
    private String type; // DEALER/DIRECT
    private String level;
    private String region;
    private String province;
    private String city;
    private String address;
    private String contact;
    private String phone;
    private String status; // ACTIVE/SUSPENDED/TERMINATED
    private LocalDate contractStart;
    private LocalDate contractEnd;
    private BigDecimal creditLimit;
    private BigDecimal laborRate;
    private String taxNo;
    private String bankAccount;
    private String parentDealerCode;
}
