package com.cloud.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
public class JwtBlacklistTokenValidator implements OAuth2TokenValidator<Jwt> {

  private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
  private static final Duration LOCAL_BLACKLIST_GRACE_PERIOD = Duration.ofMinutes(10);

  private final RedisTemplate<String, Object> redisTemplate;
  private final ConcurrentMap<String, Instant> localBlacklistCache = new ConcurrentHashMap<>();

  public JwtBlacklistTokenValidator(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    if (jwt == null || jwt.getTokenValue() == null) {
      return OAuth2TokenValidatorResult.success();
    }

    String tokenValue = jwt.getTokenValue();
    evictExpiredLocalEntry(tokenValue);

    try {
      String blacklistKey = BLACKLIST_KEY_PREFIX + tokenValue;
      boolean blacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
      if (blacklisted) {
        rememberBlacklistedToken(tokenValue, jwt);
        log.warn("JWT token blacklisted: sub={}, jti={}", jwt.getSubject(), jwt.getId());
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("blacklisted", "Token is blacklisted", null));
      }
      localBlacklistCache.remove(tokenValue);
      return OAuth2TokenValidatorResult.success();
    } catch (Exception ex) {
      if (isLocallyBlacklisted(tokenValue)) {
        log.warn(
            "JWT blacklist validation fell back to local cache: sub={}, jti={}",
            jwt.getSubject(),
            jwt.getId(),
            ex);
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("blacklisted", "Token is blacklisted", null));
      }
      log.error(
          "JWT blacklist validation failed, allow token temporarily: sub={}, jti={}",
          jwt.getSubject(),
          jwt.getId(),
          ex);
      return OAuth2TokenValidatorResult.success();
    }
  }

  // Token value is used directly as blacklist key suffix to match auth:blacklist:{token} design.

  private void rememberBlacklistedToken(String tokenValue, Jwt jwt) {
    Instant expiresAt = jwt.getExpiresAt();
    if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
      expiresAt = Instant.now().plus(LOCAL_BLACKLIST_GRACE_PERIOD);
    }
    localBlacklistCache.put(tokenValue, expiresAt);
  }

  private boolean isLocallyBlacklisted(String tokenValue) {
    Instant expiresAt = localBlacklistCache.get(tokenValue);
    if (expiresAt == null) {
      return false;
    }
    if (expiresAt.isBefore(Instant.now())) {
      localBlacklistCache.remove(tokenValue, expiresAt);
      return false;
    }
    return true;
  }

  private void evictExpiredLocalEntry(String tokenValue) {
    Instant expiresAt = localBlacklistCache.get(tokenValue);
    if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
      localBlacklistCache.remove(tokenValue, expiresAt);
    }
  }
}
