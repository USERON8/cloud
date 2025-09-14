package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员表
 *
 * @author what's up
 * @TableName admin
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "admin")
@Data
public class Admin extends BaseEntity<Admin> {
    /**
     * 用户名
     */
    @TableField(value = "username")
    private String username;

    /**
     * 加密密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 真实姓名
     */
    @TableField(value = "real_name")
    private String realName;

    /**
     * 联系电话
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 角色
     */
    @TableField(value = "role")
    private String role;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField(value = "status")
    private Integer status;
}