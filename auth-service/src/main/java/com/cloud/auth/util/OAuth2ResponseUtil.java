package com.cloud.auth.util;

import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * OAuth2响应工具类
 * 用于生成符合OAuth2.0标准的响应数据
 */
public class OAuth2ResponseUtil {

    /**
     * 构建登录响应DTO
     *
     * @param authorization OAuth2授权信息
     * @param userDTO       用户信息
     * @return LoginResponseDTO 登录响应DTO
     */
    public static LoginResponseDTO buildLoginResponse(OAuth2Authorization authorization, UserDTO userDTO) {
        LoginResponseDTO response = new LoginResponseDTO();

        // 获取访问令牌信息
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null) {
            response.setAccess_token(accessToken.getToken().getTokenValue());
            response.setToken_type(accessToken.getToken().getTokenType().getValue());

            // 设置过期时间
            Instant expiresAt = accessToken.getToken().getExpiresAt();
            if (expiresAt != null) {
                long expiresIn = Instant.now().until(expiresAt, java.time.temporal.ChronoUnit.SECONDS);
                response.setExpires_in(expiresIn);
            }

            // 设置权限范围
            Set<String> scopes = accessToken.getToken().getScopes();
            if (scopes != null && !scopes.isEmpty()) {
                response.setScope(String.join(" ", scopes));
            }
        }

        // 获取刷新令牌信息
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            response.setRefresh_token(refreshToken.getToken().getTokenValue());
        }

        // 设置用户相关信息
        if (userDTO != null) {
            response.setUser(userDTO);
            response.setUserType(userDTO.getUserType());
            response.setNickname(userDTO.getNickname());
        }

        return response;
    }

    /**
     * 构建简化版的登录响应DTO（用于注册等场景）
     *
     * @param userDTO 用户信息
     * @return LoginResponseDTO 登录响应DTO
     */
    public static LoginResponseDTO buildSimpleLoginResponse(UserDTO userDTO, JwtEncoder jwtEncoder) {
        LoginResponseDTO response = new LoginResponseDTO();

        // 生成JWT访问令牌
        if (userDTO != null && jwtEncoder != null) {
            // 创建JWT声明
            Instant now = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("self")
                    .issuedAt(now)
                    .expiresAt(now.plus(365, ChronoUnit.DAYS)) // 1年有效期，方便测试
                    .subject(userDTO.getUsername())
                    .claim("scope", "read write user.read user.write internal_api")
                    .claim("user_id", userDTO.getId())
                    .claim("user_type", userDTO.getUserType())
                    .claim("nickname", userDTO.getNickname())
                    .build();

            // 编码JWT
            Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));

            // 设置令牌信息
            response.setAccess_token(jwt.getTokenValue());
            response.setToken_type("Bearer");
            response.setExpires_in(31536000L); // 365天 = 365 * 24 * 3600 秒，方便测试
            response.setScope("read write user.read user.write internal_api");
        } else {
            // 设置默认令牌信息
            response.setToken_type("Bearer");
            response.setExpires_in(31536000L); // 365天，方便测试
            response.setScope("read write user.read user.write internal_api"); // 扩大默认权限范围
        }

        // 设置用户相关信息
        if (userDTO != null) {
            response.setUser(userDTO);
            response.setUserType(userDTO.getUserType());
            response.setNickname(userDTO.getNickname());
        }

        return response;
    }

    /**
     * 构建简化版的登录响应DTO（用于注册等场景）
     *
     * @param userDTO 用户信息
     * @return LoginResponseDTO 登录响应DTO
     */
    public static LoginResponseDTO buildSimpleLoginResponse(UserDTO userDTO) {
        LoginResponseDTO response = new LoginResponseDTO();

        // 设置基本用户信息
        if (userDTO != null) {
            response.setUser(userDTO);
            response.setUserType(userDTO.getUserType());
            response.setNickname(userDTO.getNickname());
        }

        // 设置默认令牌信息
        response.setToken_type("Bearer");
        response.setExpires_in(31536000L); // 365天，方便测试
        response.setScope("read write user.read user.write internal_api"); // 扩大默认权限范围

        return response;
    }
}