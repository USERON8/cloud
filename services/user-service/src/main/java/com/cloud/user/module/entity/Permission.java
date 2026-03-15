package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName(value = "permissions")
@Data
public class Permission extends BaseEntity<Permission> {

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("module")
    private String module;
}
