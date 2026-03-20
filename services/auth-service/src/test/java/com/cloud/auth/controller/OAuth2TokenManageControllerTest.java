package com.cloud.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.auth.service.OAuth2TokenManagementService;
import com.cloud.auth.service.TokenBlacklistService;
import com.cloud.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

@ExtendWith(MockitoExtension.class)
class OAuth2TokenManageControllerTest {

  @Mock
  private org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
      authorizationService;

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private TokenBlacklistService tokenBlacklistService;

  @Mock private OAuth2TokenManagementService tokenManagementService;

  private OAuth2TokenManageController controller;

  @BeforeEach
  void setUp() {
    controller =
        new OAuth2TokenManageController(
            authorizationService, redisTemplate, tokenBlacklistService, tokenManagementService);
  }

  @Test
  void revokeAuthorizationShouldDelegateToTokenManagementService() {
    when(tokenManagementService.revokeAuthorizationById("auth-id", "admin_revocation"))
        .thenReturn(true);

    var result = controller.revokeAuthorization("auth-id");

    assertThat(result.getCode()).isEqualTo(200);
    verify(tokenManagementService).revokeAuthorizationById("auth-id", "admin_revocation");
  }

  @Test
  void revokeAuthorizationShouldThrowWhenAuthorizationMissing() {
    when(tokenManagementService.revokeAuthorizationById("missing", "admin_revocation"))
        .thenReturn(false);

    assertThatThrownBy(() -> controller.revokeAuthorization("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void addToBlacklistShouldUseResolvedTokenTtl() {
    RegisteredClient registeredClient =
        RegisteredClient.withId("registered-client-id")
            .clientId("client-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .id("authorization-id")
            .principalName("tester")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .accessToken(
                new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "token-value",
                    java.time.Instant.now().minusSeconds(10),
                    java.time.Instant.now().plusSeconds(600)))
            .build();
    when(authorizationService.findByToken("token-value", null)).thenReturn(authorization);
    when(tokenManagementService.resolveTokenTtlSeconds(authorization, "token-value", 3600L))
        .thenReturn(600L);

    var result = controller.addToBlacklist("token-value", "admin_manual");

    assertThat(result.getCode()).isEqualTo(200);
    verify(tokenBlacklistService).addToBlacklist("token-value", "tester", 600L, "admin_manual");
  }
}
