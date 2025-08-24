package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户表
 *
 * @TableName users
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "users")
@Data
public class User extends BaseEntity<User> {
    /**
     * 用户ID
     */
    @TableId(value = "id")
    private Long id;
    
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
     * 手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 昵称
     */
    @TableField(value = "nickname")
    private String nickname;

    /**
     * 头像URL
     */
    @TableField(value = "avatar_url")
    private String avatarUrl;

    /**
     * 用户类型：USER-普通用户，MERCHANT-商家，ADMIN-管理员
     */
    @TableField(value = "user_type")
    private String userType;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 最后登录时间
     */
    @TableField(value = "last_login_at")
    private LocalDateTime lastLoginAt;
}