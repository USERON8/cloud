package com.cloud.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
public class JwtBlacklistTokenValidator implements OAuth2TokenValidator<Jwt> {

  private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

  private final RedisTemplate<String, Object> redisTemplate;

  public JwtBlacklistTokenValidator(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    if (jwt == null || jwt.getTokenValue() == null) {
      return OAuth2TokenValidatorResult.success();
    }

    try {
      String blacklistKey = BLACKLIST_KEY_PREFIX + jwt.getTokenValue();
      boolean blacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
      if (blacklisted) {
        log.warn("JWT token blacklisted: sub={}, jti={}", jwt.getSubject(), jwt.getId());
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("blacklisted", "Token is blacklisted", null));
      }
      return OAuth2TokenValidatorResult.success();
    } catch (Exception ex) {
      log.error("JWT blacklist validation failed", ex);
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error("blacklist_unavailable", "JWT blacklist validation unavailable", null));
    }
  }

  // Token value is used directly as blacklist key suffix to match auth:blacklist:{token} design.
}
