package com.cloud.auth.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName("auth_user")
@Data
public class AuthUser extends BaseEntity<AuthUser> {

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("status")
    private Integer status;
}
