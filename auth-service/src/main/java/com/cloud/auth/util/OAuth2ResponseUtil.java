package com.cloud.auth.util;

import com.cloud.common.domain.dto.auth.LoginResponseDTO;
import com.cloud.common.domain.dto.user.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * OAuth2响应工具类
 * 用于生成符合OAuth2.0标准的响应数据
 * 支持测试和生产环境的不同令牌有效期配置
 */
@Slf4j
@Component
public class OAuth2ResponseUtil {

    @Value("${app.jwt.test-mode:false}")
    private boolean testMode;

    @Value("${app.jwt.issuer:http://localhost:8080}")
    private String jwtIssuer;

    @Value("${app.jwt.access-token-validity:PT2H}")
    private String accessTokenValidity;

    @Value("${app.jwt.test-token-validity:P365D}")
    private String testTokenValidity;

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
            response.setUserType(userDTO.getUserType() != null ? userDTO.getUserType().getCode() : null);
            response.setNickname(userDTO.getNickname());
        }

        // 设置默认令牌信息
        response.setToken_type("Bearer");
        response.setExpires_in(31536000L); // 365天，方便测试
        response.setScope("read write user.read user.write internal_api"); // 扩大默认权限范围

        return response;
    }

    /**
     * 构建登录响应DTO
     *
     * @param authorization OAuth2授权信息
     * @param userDTO       用户信息
     * @return LoginResponseDTO 登录响应DTO
     */
    public LoginResponseDTO buildLoginResponse(OAuth2Authorization authorization, UserDTO userDTO) {
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
            response.setUserType(userDTO.getUserType() != null ? userDTO.getUserType().getCode() : null);
            response.setNickname(userDTO.getNickname());
        }

        return response;
    }

    /**
     * 构建简化版的登录响应DTO（用于注册等场景）
     * 支持测试模式和生产模式的不同令牌有效期
     *
     * @param userDTO    用户信息
     * @param jwtEncoder JWT编码器
     * @return LoginResponseDTO 登录响应DTO
     */
    public LoginResponseDTO buildSimpleLoginResponse(UserDTO userDTO, JwtEncoder jwtEncoder) {
        LoginResponseDTO response = new LoginResponseDTO();

        // 生成JWT访问令牌
        if (userDTO != null && jwtEncoder != null) {
            // 创建JWT声明
            Instant now = Instant.now();

            // 根据测试模式决定令牌有效期
            Instant expiresAt;
            long expiresInSeconds;

            if (testMode) {
                // 测试模式：365天有效期，方便API测试
                expiresAt = now.plus(365, ChronoUnit.DAYS);
                expiresInSeconds = 365 * 24 * 3600L; // 365天转换为秒
                log.info("🧪 测试模式：生成365天有效期的JWT令牌");
            } else {
                // 生产模式：2小时有效期，符合安全最佳实践
                expiresAt = now.plus(2, ChronoUnit.HOURS);
                expiresInSeconds = 2 * 3600L; // 2小时转换为秒
                log.info("🔒 生产模式：生成2小时有效期的JWT令牌");
            }

            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(jwtIssuer) // 使用配置的发行者URL
                    .issuedAt(now)
                    .expiresAt(expiresAt)
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
            response.setExpires_in(expiresInSeconds);
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
            response.setUserType(userDTO.getUserType() != null ? userDTO.getUserType().getCode() : null);
            response.setNickname(userDTO.getNickname());
        }

        return response;
    }
}