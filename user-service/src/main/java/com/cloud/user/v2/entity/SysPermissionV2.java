package com.cloud.user.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermissionV2 extends BaseEntity<SysPermissionV2> {

    @TableField("permission_name")
    private String permissionName;
    @TableField("permission_code")
    private String permissionCode;
    @TableField("http_method")
    private String httpMethod;
    @TableField("api_path")
    private String apiPath;
}

