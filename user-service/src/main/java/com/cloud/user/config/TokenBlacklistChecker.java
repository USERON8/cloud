package com.cloud.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;







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

            boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);

            if (isBlacklisted) {
                log.warn("馃毇 妫€娴嬪埌榛戝悕鍗旿WT浠ょ墝: subject={}, jti={}",
                        jwt.getSubject(), jwt.getClaimAsString("jti"));
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("blacklisted", "Token is blacklisted", null));
            }

            return OAuth2TokenValidatorResult.success();

        } catch (Exception e) {
            log.error("妫€鏌ヤ护鐗岄粦鍚嶅崟鐘舵€佹椂鍙戠敓閿欒", e);
            
            return OAuth2TokenValidatorResult.success();
        }
    }

    


    private String extractTokenId(Jwt jwt) {
        
        String jti = jwt.getClaimAsString("jti");
        if (jti != null && !jti.trim().isEmpty()) {
            return jti;
        }

        
        return String.valueOf(jwt.getTokenValue().hashCode());
    }
}
