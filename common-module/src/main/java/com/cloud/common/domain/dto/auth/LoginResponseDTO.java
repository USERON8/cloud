package com.cloud.common.domain.dto.auth;

import com.cloud.common.domain.dto.user.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth2标准登录响应DTO
 * 符合RFC 6749 OAuth 2.0标准响应格式
 *
 * @author what's up
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    /**
     * 访问令牌本身，客户端需要用这个Token来访问受保护的资源
     */
    private String access_token;

    /**
     * Token类型，常见的是 Bearer（bearer token）
     */
    private String token_type;

    /**
     * Access Token的剩余有效时间（秒）
     */
    private long expires_in;

    /**
     * 刷新令牌，用于在Access Token过期后获取新的Access Token
     */
    private String refresh_token;

    /**
     * Token的权限范围，表示该Token有权访问哪些资源
     */
    private String scope;

    /**
     * 用户类型（扩展字段）
     */
    private String userType;

    /**
     * 用户昵称（扩展字段）
     */
    private String nickname;

    /**
     * 用户基本信息
     */
    private UserDTO user;
}