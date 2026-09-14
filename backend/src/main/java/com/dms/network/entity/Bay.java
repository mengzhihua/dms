package com.dms.network.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_bay")
public class Bay extends BaseEntity {
    private String dealerCode;
    private String code;
    private String type;
    private String status; // IDLE/BUSY/OFF
}
