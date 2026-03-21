package com.cloud.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

@ExtendWith(MockitoExtension.class)
class OAuth2TokenManagementServiceTest {

  @Mock
  private org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
      authorizationService;

  @Mock private RegisteredClientRepository registeredClientRepository;

  @Mock
  private OAuth2TokenGenerator<org.springframework.security.oauth2.core.OAuth2Token> tokenGenerator;

  @Mock private TokenBlacklistService tokenBlacklistService;

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private SetOperations<String, Object> setOperations;

  private OAuth2TokenManagementService tokenManagementService;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    tokenManagementService =
        new OAuth2TokenManagementService(
            authorizationService,
            registeredClientRepository,
            tokenGenerator,
            tokenBlacklistService,
            redisTemplate);
  }

  @Test
  void revokeAuthorizationByIdShouldBlacklistAccessAndRefreshTokens() {
    Instant issuedAt = Instant.now().minusSeconds(30);
    Instant accessExpiresAt = Instant.now().plusSeconds(1800);
    Instant refreshExpiresAt = Instant.now().plusSeconds(7200);
    RegisteredClient registeredClient =
        RegisteredClient.withId("registered-client-id")
            .clientId("client-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofHours(1))
                    .refreshTokenTimeToLive(Duration.ofDays(7))
                    .build())
            .build();
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .id("authorization-id")
            .principalName("tester")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .accessToken(
                new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "access-token",
                    issuedAt,
                    accessExpiresAt,
                    Set.of("internal")))
            .refreshToken(new OAuth2RefreshToken("refresh-token", issuedAt, refreshExpiresAt))
            .build();

    when(authorizationService.findById("authorization-id")).thenReturn(authorization);
    when(registeredClientRepository.findById("registered-client-id")).thenReturn(registeredClient);

    boolean revoked =
        tokenManagementService.revokeAuthorizationById("authorization-id", "admin_revocation");

    assertThat(revoked).isTrue();
    ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
    verify(tokenBlacklistService)
        .addToBlacklist(
            org.mockito.ArgumentMatchers.eq("access-token"),
            org.mockito.ArgumentMatchers.eq("tester"),
            ttlCaptor.capture(),
            org.mockito.ArgumentMatchers.eq("admin_revocation"));
    long accessTtl = ttlCaptor.getValue();
    assertThat(accessTtl).isBetween(60L, 1800L);

    ArgumentCaptor<Long> refreshTtlCaptor = ArgumentCaptor.forClass(Long.class);
    verify(tokenBlacklistService)
        .addToBlacklist(
            org.mockito.ArgumentMatchers.eq("refresh-token"),
            org.mockito.ArgumentMatchers.eq("tester"),
            refreshTtlCaptor.capture(),
            org.mockito.ArgumentMatchers.eq("admin_revocation"));
    long refreshTtl = refreshTtlCaptor.getValue();
    assertThat(refreshTtl).isBetween(60L, 7200L);
    verify(authorizationService).remove(authorization);
  }

  @Test
  void resolveTokenTtlSecondsShouldUseRegisteredClientSettingsWhenTokenExpiryIsMissing() {
    Instant issuedAt = Instant.now().minusSeconds(10);
    RegisteredClient registeredClient =
        RegisteredClient.withId("registered-client-id")
            .clientId("client-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofHours(2))
                    .refreshTokenTimeToLive(Duration.ofDays(7))
                    .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                    .build())
            .build();
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .id("authorization-id")
            .principalName("tester")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .accessToken(
                new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "access-token",
                    issuedAt,
                    null,
                    Set.of("internal")))
            .build();

    when(registeredClientRepository.findById("registered-client-id")).thenReturn(registeredClient);

    long ttl = tokenManagementService.resolveTokenTtlSeconds(authorization, "access-token", 60L);

    assertThat(ttl).isEqualTo(Duration.ofHours(2).toSeconds());
  }

  @Test
  void revokeAuthorizationByIdShouldReturnFalseWhenAuthorizationMissing() {
    when(authorizationService.findById("missing")).thenReturn(null);

    boolean revoked = tokenManagementService.revokeAuthorizationById("missing", "admin_revocation");

    assertThat(revoked).isFalse();
    verify(tokenBlacklistService, never())
        .addToBlacklist(anyString(), anyString(), anyLong(), anyString());
  }

  @Test
  void revokeAuthorizationByIdShouldPropagateBlacklistFailure() {
    Instant issuedAt = Instant.now().minusSeconds(10);
    RegisteredClient registeredClient =
        RegisteredClient.withId("registered-client-id")
            .clientId("client-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenSettings(
                TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
            .build();
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .id("authorization-id")
            .principalName("tester")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .accessToken(
                new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "access-token",
                    issuedAt,
                    issuedAt.plusSeconds(1800),
                    Set.of("internal")))
            .build();

    when(authorizationService.findById("authorization-id")).thenReturn(authorization);
    when(registeredClientRepository.findById("registered-client-id")).thenReturn(registeredClient);
    org.mockito.Mockito.doThrow(new RuntimeException("blacklist failed"))
        .when(tokenBlacklistService)
        .addToBlacklist(eq("access-token"), eq("tester"), anyLong(), eq("admin_revocation"));

    assertThatThrownBy(
            () ->
                tokenManagementService.revokeAuthorizationById(
                    "authorization-id", "admin_revocation"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("blacklist failed");

    verify(authorizationService, never()).remove(authorization);
  }

  @Test
  void logoutAllSessionsShouldUsePrincipalIndex() {
    Instant issuedAt = Instant.now().minusSeconds(10);
    RegisteredClient registeredClient =
        RegisteredClient.withId("registered-client-id")
            .clientId("client-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenSettings(
                TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
            .build();
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .id("authorization-id")
            .principalName("tester")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .accessToken(
                new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "access-token",
                    issuedAt,
                    issuedAt.plusSeconds(1800),
                    Set.of("internal")))
            .build();

    when(setOperations.members("oauth2:principal:tester")).thenReturn(Set.of("authorization-id"));
    when(authorizationService.findById("authorization-id")).thenReturn(authorization);
    when(registeredClientRepository.findById("registered-client-id")).thenReturn(registeredClient);

    int revokedCount = tokenManagementService.logoutAllSessions("tester");

    assertThat(revokedCount).isEqualTo(1);
    verify(redisTemplate, never()).keys("oauth2:token:*");
    verify(authorizationService).remove(authorization);
  }
}
