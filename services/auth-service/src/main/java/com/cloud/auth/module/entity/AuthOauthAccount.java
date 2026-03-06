package com.cloud.auth.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName("auth_oauth_account")
@Data
public class AuthOauthAccount extends BaseEntity<AuthOauthAccount> {

    @TableField("user_id")
    private Long userId;

    @TableField("provider")
    private String provider;

    @TableField("provider_user_id")
    private String providerUserId;

    @TableField("provider_username")
    private String providerUsername;

    @TableField("email")
    private String email;

    @TableField("avatar_url")
    private String avatarUrl;
}
