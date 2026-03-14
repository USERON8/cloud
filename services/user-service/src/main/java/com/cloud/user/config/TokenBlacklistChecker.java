package com.cloud.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistChecker implements OAuth2TokenValidator<Jwt> {

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
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
                log.warn("Blacklisted JWT detected: subject={}, jti={}", jwt.getSubject(), jwt.getClaimAsString("jti"));
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("blacklisted", "Token is blacklisted", null));
            }

            return OAuth2TokenValidatorResult.success();
        } catch (Exception e) {
            log.error("Failed to check JWT blacklist status", e);
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("blacklist_unavailable", "JWT blacklist validation unavailable", null)
            );
        }
    }

    private String extractTokenId(Jwt jwt) {
        String jti = jwt.getClaimAsString("jti");
        if (jti != null && !jti.trim().isEmpty()) {
            return jti;
        }
        return sha256Hex(jwt.getTokenValue());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
