package com.dms.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 通知模板：content 中支持 {{name}} {{plate}} {{date}} {{dealer}} {{mileage}} 占位符。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dms_notify_template")
public class NotifyTemplate extends BaseEntity {
    @NotBlank(message = "模板编码必填")
    private String code;
    @NotBlank(message = "模板名称必填")
    private String name;
    private String channel; // SMS/WECHAT/ANY
    private String content;
    private Boolean enabled;
}
