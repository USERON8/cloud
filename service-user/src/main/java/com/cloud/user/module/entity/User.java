package com.cloud.user.module.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表
 *
 * @TableName users
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "users")
@Data
public class User extends BaseEntity {
    /**
     * 用户ID（雪花算法）
     */
    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    @TableField(value = "username")
    private String username;

    /**
     * 加密密码
     */
    @TableField(value = "password_hash")
    private String passwordHash;

    /**
     * 角色类型
     */
    @TableField(value = "user_type")
    private String userType;

    /**
     * 邮箱（全局唯一）
     */
    @TableField(value = "email")
    private String email;

    /**
     * 手机号（全局唯一）
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
     * 头像文件名
     */
    @TableField(value = "avatar_file_name")
    private String avatarFileName;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 软删除标记
     */
    @TableField(value = "deleted")
    private Integer deleted;

}