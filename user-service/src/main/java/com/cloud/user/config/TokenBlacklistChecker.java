package com.cloud.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * 令牌黑名单检查器（user-service版本）
 * 检查JWT令牌是否在auth-service维护的黑名单中
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistChecker implements OAuth2TokenValidator<Jwt> {

    private static final String BLACKLIST_KEY_PREFIX = "oauth2:blacklist:";
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt == null || jwt.getTokenValue() == null) {
            return OAuth2TokenValidatorResult.success();
        }

        try {
            String tokenId = extractTokenId(jwt);
            String blacklistKey = BLACKLIST_KEY_PREFIX + tokenId;

            boolean isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));

            if (isBlacklisted) {
                log.warn("🚫 检测到黑名单JWT令牌: subject={}, jti={}",
                        jwt.getSubject(), jwt.getClaimAsString("jti"));
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("blacklisted", "Token is blacklisted", null));
            }

            return OAuth2TokenValidatorResult.success();

        } catch (Exception e) {
            log.error("检查令牌黑名单状态时发生错误", e);
            // 发生错误时允许通过，避免影响正常业务
            return OAuth2TokenValidatorResult.success();
        }
    }

    /**
     * 从JWT中提取令牌ID
     */
    private String extractTokenId(Jwt jwt) {
        // 优先使用jti声明
        String jti = jwt.getClaimAsString("jti");
        if (jti != null && !jti.trim().isEmpty()) {
            return jti;
        }

        // 如果没有jti，使用令牌值的哈希
        return String.valueOf(jwt.getTokenValue().hashCode());
    }
}
