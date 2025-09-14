package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表
 *
 * @author what's up
 * @TableName users
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "users")
@Data
public class User extends BaseEntity<User> {
    /**
     * 用户名
     */
    @TableField(value = "username", insertStrategy = FieldStrategy.NOT_NULL, updateStrategy = FieldStrategy.NOT_NULL)
    private String username;

    /**
     * 加密密码
     */
    @TableField(value = "password", insertStrategy = FieldStrategy.NOT_NULL, updateStrategy = FieldStrategy.NOT_NULL)
    private String password;

    /**
     * 手机号
     */
    @TableField(value = "phone", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String phone;

    /**
     * 昵称
     */
    @TableField(value = "nickname", insertStrategy = FieldStrategy.NOT_NULL, updateStrategy = FieldStrategy.NOT_NULL)
    private String nickname;

    /**
     * 头像URL
     */
    @TableField(value = "avatar_url", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String avatarUrl;

    /**
     * 邮箱地址（用于GitHub登录）
     */
    @TableField(value = "email", insertStrategy = FieldStrategy.NOT_EMPTY, updateStrategy = FieldStrategy.NOT_EMPTY)
    private String email;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField(value = "status", insertStrategy = FieldStrategy.NOT_NULL, updateStrategy = FieldStrategy.NOT_NULL)
    private Integer status;

    /**
     * 用户类型
     */
    @TableField(value = "user_type", insertStrategy = FieldStrategy.NOT_NULL, updateStrategy = FieldStrategy.NOT_NULL)
    private String userType;
}