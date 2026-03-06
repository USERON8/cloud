package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_permission")
@Data
public class Permission extends BaseEntity<Permission> {

    @TableField(value = "permission_name")
    private String permissionName;

    @TableField(value = "permission_code")
    private String permissionCode;

    @TableField(value = "http_method")
    private String httpMethod;

    @TableField(value = "api_path")
    private String apiPath;
}
