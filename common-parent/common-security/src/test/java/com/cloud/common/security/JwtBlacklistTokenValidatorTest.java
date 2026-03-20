package com.cloud.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistTokenValidatorTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Test
  void validateShouldRejectBlacklistedToken() {
    JwtBlacklistTokenValidator validator = new JwtBlacklistTokenValidator(redisTemplate);
    Jwt jwt = newJwt("token-1");
    when(redisTemplate.hasKey("auth:blacklist:token-1")).thenReturn(true);

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.getErrors()).singleElement().extracting("errorCode").isEqualTo("blacklisted");
  }

  @Test
  void validateShouldDegradeOpenWhenRedisIsUnavailable() {
    JwtBlacklistTokenValidator validator = new JwtBlacklistTokenValidator(redisTemplate);
    Jwt jwt = newJwt("token-2");
    when(redisTemplate.hasKey("auth:blacklist:token-2"))
        .thenThrow(new IllegalStateException("redis down"));

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isFalse();
  }

  @Test
  void validateShouldUseLocalBlacklistCacheWhenRedisFailsAfterATrueHit() {
    JwtBlacklistTokenValidator validator = new JwtBlacklistTokenValidator(redisTemplate);
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

  private Jwt newJwt(String tokenValue) {
    Instant now = Instant.now();
    return Jwt.withTokenValue(tokenValue)
        .subject("user-1")
        .header("alg", "none")
        .claim("jti", "jwt-1")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
