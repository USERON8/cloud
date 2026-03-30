package com.cloud.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

class ResourceServerConfigTest {

  private ReactiveStringRedisTemplate reactiveStringRedisTemplate;
  private ResourceServerConfig config;

  @BeforeEach
  void setUp() {
    reactiveStringRedisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
    Environment environment = Mockito.mock(Environment.class);
    Mockito.when(environment.getActiveProfiles()).thenReturn(new String[0]);
    config = new ResourceServerConfig(reactiveStringRedisTemplate, environment);
    ReflectionTestUtils.setField(config, "blacklistFailClosed", true);
  }

  @Test
  void validateBlacklistShouldRejectTokenWhenRedisIsUnavailableInFailClosedMode() {
    Jwt jwt = buildJwt("token-a");
    Mockito.when(reactiveStringRedisTemplate.hasKey("auth:blacklist:token-a"))
        .thenReturn(Mono.error(new IllegalStateException("redis down")));

    assertThatThrownBy(() -> config.validateBlacklist(jwt, "token-a").block())
        .isInstanceOf(BadJwtException.class)
        .hasMessageContaining("blacklist validation unavailable");
  }

  @Test
  void validateBlacklistShouldRejectTokenFromLocalCacheWhenRedisFails() {
    Jwt jwt = buildJwt("token-b");
    Mockito.when(reactiveStringRedisTemplate.hasKey("auth:blacklist:token-b"))
        .thenReturn(Mono.just(true), Mono.error(new IllegalStateException("redis down")));

    assertThatThrownBy(() -> config.validateBlacklist(jwt, "token-b").block())
        .isInstanceOf(BadJwtException.class)
        .hasMessageContaining("Token is blacklisted");

    assertThatThrownBy(() -> config.validateBlacklist(jwt, "token-b").block())
        .isInstanceOf(BadJwtException.class)
        .hasMessageContaining("Token is blacklisted");
  }

  @Test
  void validateBlacklistShouldAllowTokenWhenFailClosedDisabled() {
    Jwt jwt = buildJwt("token-c");
    ReflectionTestUtils.setField(config, "blacklistFailClosed", false);
    Mockito.when(reactiveStringRedisTemplate.hasKey("auth:blacklist:token-c"))
        .thenReturn(Mono.error(new IllegalStateException("redis down")));

    Jwt result = config.validateBlacklist(jwt, "token-c").block();

    assertThat(result).isSameAs(jwt);
  }

  private Jwt buildJwt(String tokenValue) {
    Instant now = Instant.now();
    return Jwt.withTokenValue(tokenValue)
        .header("alg", "none")
        .subject("user-1")
        .claim("jti", tokenValue + "-jti")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
