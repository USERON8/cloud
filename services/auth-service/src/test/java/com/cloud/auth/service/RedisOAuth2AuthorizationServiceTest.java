package com.cloud.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

@ExtendWith(MockitoExtension.class)
class RedisOAuth2AuthorizationServiceTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private ValueOperations<String, Object> valueOperations;

  private RedisOAuth2AuthorizationService authorizationService;

  @BeforeEach
  void setUp() {
    authorizationService =
        new RedisOAuth2AuthorizationService(
            redisTemplate,
            new org.springframework.security.oauth2.server.authorization.client
                .InMemoryRegisteredClientRepository(
                RegisteredClient.withId("registered-client-id")
                    .clientId("client-service")
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .build()),
            AuthorizationServerSettings.builder().issuer("http://127.0.0.1:8081").build());
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void findByTokenShouldResolveRefreshTokenWhenTokenTypeIsNull() {
    Instant issuedAt = Instant.parse("2026-03-21T00:00:00Z");
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(
                RegisteredClient.withId("registered-client-id")
                    .clientId("client-service")
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .build())
            .id("authorization-id")
            .principalName("service-user")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .accessToken(
                new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "access-token",
                    issuedAt,
                    issuedAt.plusSeconds(3600)))
            .refreshToken(
                new OAuth2RefreshToken("refresh-token", issuedAt, issuedAt.plusSeconds(7200)))
            .build();

    when(valueOperations.get("oauth2:refresh:refresh-token")).thenReturn("authorization-id");
    when(valueOperations.get("oauth2:token:authorization-id")).thenReturn(authorization);
    when(redisTemplate.keys("oauth2:token:*")).thenReturn(Set.of("oauth2:token:authorization-id"));
    when(valueOperations.get("oauth2:token:authorization-id")).thenReturn(authorization);

    OAuth2Authorization result = authorizationService.findByToken("refresh-token", null);

    assertThat(result).isSameAs(authorization);
  }
}
