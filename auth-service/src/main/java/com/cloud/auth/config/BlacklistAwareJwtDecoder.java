package com.cloud.auth.config;

import com.cloud.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

/**
 * 支持黑名单检查的JWT解码器
 * 在标准JWT验证基础上增加黑名单检查功能
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistAwareJwtDecoder implements JwtDecoder {

    private final JwtDecoder delegate;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public Jwt decode(String token) throws JwtException {
        // 首先进行标准JWT解码和验证
        Jwt jwt = delegate.decode(token);

        // 检查令牌是否在黑名单中
        if (tokenBlacklistService.isBlacklisted(jwt)) {
            log.warn("检测到黑名单JWT令牌: subject={}, jti={}",
                    jwt.getSubject(), jwt.getClaimAsString("jti"));
            throw new JwtValidationException("JWT token has been revoked",
                    OAuth2TokenValidatorResult.failure(new OAuth2Error("blacklisted", "Token is blacklisted", null)).getErrors());
        }

        return jwt;
    }

    /**
     * 黑名单令牌验证器
     * 可以作为独立的验证器使用
     */
    @Component
    @RequiredArgsConstructor
    public static class BlacklistTokenValidator implements OAuth2TokenValidator<Jwt> {

        private final TokenBlacklistService tokenBlacklistService;

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                log.debug("JWT令牌验证失败：令牌在黑名单中, subject={}", jwt.getSubject());
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("blacklisted", "Token is blacklisted", null));
            }

            return OAuth2TokenValidatorResult.success();
        }
    }
}
