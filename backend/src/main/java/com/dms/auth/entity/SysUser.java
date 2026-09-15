package com.dms.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    @NotBlank(message = "用户名必填")
    private String username;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;

    @NotBlank(message = "姓名必填")
    private String realName;

    @NotBlank(message = "角色必填")
    private String role;

    private String dealerCode;

    private Boolean enabled;

    /** 仅用于新建/重置密码，不落库。 */
    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
