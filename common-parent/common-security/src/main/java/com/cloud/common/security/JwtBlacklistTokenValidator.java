package com.cloud.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
public class JwtBlacklistTokenValidator implements OAuth2TokenValidator<Jwt> {

  private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
  private static final String AUTH_VERSION_KEY_PREFIX = "auth:version:";

  private final RedisTemplate<String, Object> redisTemplate;
  private final StringRedisTemplate stringRedisTemplate;
  private final LocalJwtBlacklistCache localBlacklistCache = new LocalJwtBlacklistCache();

  public JwtBlacklistTokenValidator(
      RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
    this.redisTemplate = redisTemplate;
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    if (jwt == null || jwt.getTokenValue() == null) {
      return OAuth2TokenValidatorResult.success();
    }

    String tokenValue = jwt.getTokenValue();
    localBlacklistCache.evictExpired(tokenValue);

    try {
      String blacklistKey = BLACKLIST_KEY_PREFIX + tokenValue;
      boolean blacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
      if (blacklisted) {
        localBlacklistCache.remember(tokenValue, jwt);
        log.warn("JWT token blacklisted: sub={}, jti={}", jwt.getSubject(), jwt.getId());
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("blacklisted", "Token is blacklisted", null));
      }
      OAuth2TokenValidatorResult versionResult = validateAuthVersion(jwt);
      if (versionResult.hasErrors()) {
        return versionResult;
      }
      localBlacklistCache.clear(tokenValue);
      return OAuth2TokenValidatorResult.success();
    } catch (Exception ex) {
      if (localBlacklistCache.contains(tokenValue)) {
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

  private OAuth2TokenValidatorResult validateAuthVersion(Jwt jwt) {
    String userId = claimAsString(jwt, "user_id");
    if (userId == null || userId.isBlank()) {
      userId = claimAsString(jwt, "userId");
    }
    if (userId == null || userId.isBlank()) {
      return OAuth2TokenValidatorResult.success();
    }

    String currentVersion = stringRedisTemplate.opsForValue().get(AUTH_VERSION_KEY_PREFIX + userId);
    if (currentVersion == null || currentVersion.isBlank()) {
      return OAuth2TokenValidatorResult.success();
    }
    String tokenVersion = claimAsString(jwt, "auth_version");
    if (!currentVersion.equals(tokenVersion)) {
      log.warn(
          "JWT credentials are stale: sub={}, userId={}, tokenVersion={}, currentVersion={}",
          jwt.getSubject(),
          userId,
          tokenVersion,
          currentVersion);
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error("credentials_stale", "Token credentials are stale", null));
    }
    return OAuth2TokenValidatorResult.success();
  }

  private String claimAsString(Jwt jwt, String claimName) {
    Object claim = jwt.getClaims().get(claimName);
    return claim == null ? null : claim.toString();
  }
}
