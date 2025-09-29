package com.cloud.common.domain.dto.user;

import com.cloud.common.enums.UserType;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息DTO
 * 与数据库表users字段完全匹配
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
public class UserDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID - 对应数据库字段: id
     */
    private Long id;

    /**
     * 用户名 - 对应数据库字段: username
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    /**
     * 密码 - 对应数据库字段: password (仅用于创建/更新，查询时不返回)
     */
    @Size(max = 255, message = "密码长度不能超过255个字符")
    private String password;

    /**
     * 手机号 - 对应数据库字段: phone
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 昵称 - 对应数据库字段: nickname
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /**
     * 头像URL - 对应数据库字段: avatar_url
     */
    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatarUrl;

    /**
     * 邮箱地址 - 对应数据库字段: email
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /**
     * GitHub用户ID - 对应数据库字段: github_id
     */
    private Long githubId;

    /**
     * GitHub用户名 - 对应数据库字段: github_username
     */
    @Size(max = 100, message = "GitHub用户名长度不能超过100个字符")
    private String githubUsername;

    /**
     * OAuth提供商 - 对应数据库字段: oauth_provider
     */
    @Size(max = 20, message = "OAuth提供商长度不能超过20个字符")
    private String oauthProvider;

    /**
     * OAuth提供商用户ID - 对应数据库字段: oauth_provider_id
     */
    @Size(max = 100, message = "OAuth提供商用户ID长度不能超过100个字符")
    private String oauthProviderId;

    /**
     * 状态 - 对应数据库字段: status
     * 0-禁用，1-启用
     */
    @Min(value = 0, message = "状态值不能小于0")
    @Max(value = 1, message = "状态值不能大于1")
    private Integer status;

    /**
     * 用户类型 - 对应数据库字段: user_type
     */
    private UserType userType;

    /**
     * 创建时间 - 对应数据库字段: created_at
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 对应数据库字段: updated_at
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除标记 - 对应数据库字段: deleted
     * 0-未删除，1-已删除
     */
    private Integer deleted;
}