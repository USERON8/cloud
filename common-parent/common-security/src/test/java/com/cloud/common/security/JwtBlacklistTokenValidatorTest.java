package com.cloud.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistTokenValidatorTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  @Test
  void validateShouldRejectBlacklistedToken() {
    JwtBlacklistTokenValidator validator =
        new JwtBlacklistTokenValidator(redisTemplate, stringRedisTemplate);
    Jwt jwt = newJwt("token-1");
    when(redisTemplate.hasKey("auth:blacklist:token-1")).thenReturn(true);

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors()).singleElement().extracting("errorCode").isEqualTo("blacklisted");
  }

  @Test
  void validateShouldDegradeOpenWhenRedisIsUnavailable() {
    JwtBlacklistTokenValidator validator =
        new JwtBlacklistTokenValidator(redisTemplate, stringRedisTemplate);
    Jwt jwt = newJwt("token-2");
    when(redisTemplate.hasKey("auth:blacklist:token-2"))
        .thenThrow(new IllegalStateException("redis down"));

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isFalse();
  }

  @Test
  void validateShouldUseLocalBlacklistCacheWhenRedisFailsAfterATrueHit() {
    JwtBlacklistTokenValidator validator =
        new JwtBlacklistTokenValidator(redisTemplate, stringRedisTemplate);
    Jwt jwt = newJwt("token-3");
    when(redisTemplate.hasKey("auth:blacklist:token-3"))
        .thenReturn(true)
        .thenThrow(new IllegalStateException("redis down"));

    var firstResult = validator.validate(jwt);
    var secondResult = validator.validate(jwt);

    assertThat(firstResult.hasErrors()).isTrue();
    assertThat(secondResult.hasErrors()).isTrue();
    assertThat(secondResult.getErrors())
        .singleElement()
        .extracting("errorCode")
        .isEqualTo("blacklisted");
  }

  @Test
  void validateShouldAcceptTokenWhenAuthVersionMatches() {
    JwtBlacklistTokenValidator validator =
        new JwtBlacklistTokenValidator(redisTemplate, stringRedisTemplate);
    Jwt jwt = newJwt("token-4", 42L, 3L);
    when(redisTemplate.hasKey("auth:blacklist:token-4")).thenReturn(false);
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("auth:version:42")).thenReturn("3");

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isFalse();
  }

  @Test
  void validateShouldRejectTokenWhenAuthVersionIsStale() {
    JwtBlacklistTokenValidator validator =
        new JwtBlacklistTokenValidator(redisTemplate, stringRedisTemplate);
    Jwt jwt = newJwt("token-5", 42L, 2L);
    when(redisTemplate.hasKey("auth:blacklist:token-5")).thenReturn(false);
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("auth:version:42")).thenReturn("3");

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors())
        .singleElement()
        .extracting("errorCode")
        .isEqualTo("credentials_stale");
  }

  @Test
  void validateShouldRejectLegacyTokenAfterAuthVersionIsPublished() {
    JwtBlacklistTokenValidator validator =
        new JwtBlacklistTokenValidator(redisTemplate, stringRedisTemplate);
    Jwt jwt = newJwt("token-6", 42L, null);
    when(redisTemplate.hasKey("auth:blacklist:token-6")).thenReturn(false);
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("auth:version:42")).thenReturn("3");

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors())
        .singleElement()
        .extracting("errorCode")
        .isEqualTo("credentials_stale");
  }

  private Jwt newJwt(String tokenValue) {
    return newJwt(tokenValue, null, null);
  }

  private Jwt newJwt(String tokenValue, Long userId, Long authVersion) {
    Instant now = Instant.now();
    Jwt.Builder builder =
        Jwt.withTokenValue(tokenValue)
        .subject("user-1")
        .header("alg", "none")
        .claim("jti", "jwt-1")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300));
    if (userId != null) {
      builder.claim("user_id", userId);
    }
    if (authVersion != null) {
      builder.claim("auth_version", authVersion);
    }
    return builder.build();
  }
}
