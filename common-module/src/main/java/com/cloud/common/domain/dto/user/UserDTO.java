package com.cloud.common.domain.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息DTO
 */
@Data
public class UserDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 邮箱地址（用于GitHub登录）
     */
    private String email;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 用户类型 (USER, ADMIN, MERCHANT)
     */
    private String userType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * GitHub用户ID（OAuth登录专用）
     */
    private Long githubId;

    /**
     * GitHub用户名（OAuth登录专用）
     */
    private String githubUsername;

    /**
     * OAuth提供商（github, wechat等）
     */
    private String oauthProvider;

    /**
     * OAuth提供商用户ID
     */
    private String oauthProviderId;
}