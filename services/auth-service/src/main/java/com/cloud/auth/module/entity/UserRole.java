package com.cloud.auth.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
@Data
public class UserRole extends BaseEntity<UserRole> {

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;
}
