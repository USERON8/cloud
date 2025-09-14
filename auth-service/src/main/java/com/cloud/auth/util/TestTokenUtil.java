package com.cloud.auth.util;

import com.cloud.common.domain.dto.user.UserDTO;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 测试用Token生成工具
 * 专门为测试环境生成长期有效的Token
 *
 * @author what's up
 */
@Component
public class TestTokenUtil {

    /**
     * 生成永久测试Token（实际365天有效期）
     *
     * @param userDTO    用户信息
     * @param jwtEncoder JWT编码器
     * @return 生成的JWT Token字符串
     */
    public static String generatePermanentTestToken(UserDTO userDTO, JwtEncoder jwtEncoder) {
        if (userDTO == null || jwtEncoder == null) {
            throw new IllegalArgumentException("UserDTO和JwtEncoder不能为空");
        }

        Instant now = Instant.now();

        // 创建JWT Claims，设置365天有效期
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("test-auth-server")
                .issuedAt(now)
                .expiresAt(now.plus(365, ChronoUnit.DAYS)) // 365天有效期
                .subject(userDTO.getUsername())
                .audience(java.util.List.of("test-client", "web-client", "mobile-client"))

                // OAuth2 标准Claims
                .claim("scope", "openid profile read write user.read user.write internal_api order.read order.write")
                .claim("token_type", "Bearer")

                // 用户相关Claims
                .claim("user_id", userDTO.getId())
                .claim("username", userDTO.getUsername())
                .claim("user_type", userDTO.getUserType())
                .claim("nickname", userDTO.getNickname())
                .claim("email", userDTO.getEmail())
                .claim("phone", userDTO.getPhone())

                // 测试标识
                .claim("is_test_token", true)
                .claim("token_purpose", "long_term_testing")

                .build();

        // 编码为JWT
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 生成管理员测试Token
     *
     * @param userDTO    用户信息
     * @param jwtEncoder JWT编码器
     * @return 生成的JWT Token字符串
     */
    public static String generateAdminTestToken(UserDTO userDTO, JwtEncoder jwtEncoder) {
        if (userDTO == null || jwtEncoder == null) {
            throw new IllegalArgumentException("UserDTO和JwtEncoder不能为空");
        }

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("test-auth-server")
                .issuedAt(now)
                .expiresAt(now.plus(365, ChronoUnit.DAYS)) // 365天有效期
                .subject(userDTO.getUsername())
                .audience(java.util.List.of("admin-client", "web-client"))

                // 管理员权限
                .claim("scope", "openid profile read write user.read user.write internal_api admin.read admin.write")
                .claim("authorities", java.util.List.of("ROLE_ADMIN", "ROLE_USER"))
                .claim("token_type", "Bearer")

                // 用户相关Claims
                .claim("user_id", userDTO.getId())
                .claim("username", userDTO.getUsername())
                .claim("user_type", "ADMIN")
                .claim("nickname", userDTO.getNickname())
                .claim("email", userDTO.getEmail())
                .claim("phone", userDTO.getPhone())

                // 测试标识
                .claim("is_test_token", true)
                .claim("is_admin_token", true)
                .claim("token_purpose", "admin_testing")

                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 生成内部服务调用Token
     *
     * @param jwtEncoder JWT编码器
     * @return 生成的JWT Token字符串
     */
    public static String generateInternalServiceToken(JwtEncoder jwtEncoder) {
        if (jwtEncoder == null) {
            throw new IllegalArgumentException("JwtEncoder不能为空");
        }

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("test-auth-server")
                .issuedAt(now)
                .expiresAt(now.plus(365, ChronoUnit.DAYS)) // 365天有效期
                .subject("internal-service")
                .audience(java.util.List.of("internal-api"))

                // 内部服务权限
                .claim("scope", "internal_api")
                .claim("token_type", "Bearer")
                .claim("client_id", "client-service")

                // 测试标识
                .claim("is_test_token", true)
                .claim("is_service_token", true)
                .claim("token_purpose", "internal_service_testing")

                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
