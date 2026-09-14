package com.dms.network.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_technician")
public class Technician extends BaseEntity {
    private String dealerCode;
    @NotBlank(message = "技师工号必填")
    private String code;
    @NotBlank(message = "技师姓名必填")
    private String name;
    private String level;
    private String skills;
    private String status; // IDLE/BUSY/OFF
}
