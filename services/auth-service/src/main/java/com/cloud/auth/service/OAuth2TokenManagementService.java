package com.cloud.auth.service;

import cn.hutool.core.util.StrUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2TokenManagementService {

  private static final String PRINCIPAL_PREFIX = "oauth2:principal:";
  private final OAuth2AuthorizationService authorizationService;
  private final RegisteredClientRepository registeredClientRepository;
  private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
  private final TokenBlacklistService tokenBlacklistService;

  @Qualifier("oauth2MainRedisTemplate")
  private final RedisTemplate<String, Object> redisTemplate;

  @Value("${AUTH_ISSUER_URI:http://127.0.0.1:8081}")
  private String issuerUri;

  public OAuth2Authorization generateTokensForClient(String clientId, Set<String> scopes) {
    RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
    if (registeredClient == null) {
      throw new IllegalArgumentException("Client not found: " + clientId);
    }
    if (!registeredClient
        .getAuthorizationGrantTypes()
        .contains(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
      throw new IllegalArgumentException("Client does not support client_credentials grant");
    }

    if (scopes == null || scopes.isEmpty()) {
      scopes = registeredClient.getScopes();
    } else {
      scopes =
          scopes.stream()
              .filter(registeredClient.getScopes()::contains)
              .collect(Collectors.toSet());
    }

    OAuth2ClientAuthenticationToken clientAuthentication =
        new OAuth2ClientAuthenticationToken(
            registeredClient,
            ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
            registeredClient.getClientSecret());

    OAuth2Authorization.Builder authorizationBuilder =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .id(UUID.randomUUID().toString())
            .principalName(clientId)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .authorizedScopes(scopes);

    AuthorizationServerContext authorizationServerContext = resolveAuthorizationServerContext();

    OAuth2TokenContext tokenContext =
        DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(clientAuthentication)
            .authorizationGrant(clientAuthentication)
            .authorizationServerContext(authorizationServerContext)
            .authorization(authorizationBuilder.build())
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .authorizedScopes(scopes)
            .build();

    OAuth2Token accessToken = tokenGenerator.generate(tokenContext);
    OAuth2AccessToken oauth2AccessToken = toAccessToken(accessToken, scopes);

    OAuth2Authorization authorization = authorizationBuilder.accessToken(oauth2AccessToken).build();
    authorizationService.save(authorization);
    return authorization;
  }

  private OAuth2AccessToken toAccessToken(OAuth2Token token, Set<String> scopes) {
    if (token instanceof OAuth2AccessToken accessToken) {
      return accessToken;
    }
    if (token instanceof Jwt jwt) {
      return new OAuth2AccessToken(
          OAuth2AccessToken.TokenType.BEARER,
          jwt.getTokenValue(),
          jwt.getIssuedAt(),
          jwt.getExpiresAt(),
          scopes);
    }
    throw new IllegalStateException("Generated token is not an OAuth2 access token");
  }

  public void revokeToken(String tokenValue) {
    OAuth2Authorization authorization = findByToken(tokenValue);
    if (authorization != null) {
      revokeAuthorization(authorization, "manual_revocation");
    }
  }

  public boolean revokeAuthorizationById(String authorizationId, String reason) {
    if (StrUtil.isBlank(authorizationId)) {
      return false;
    }
    OAuth2Authorization authorization = authorizationService.findById(authorizationId.trim());
    if (authorization == null) {
      return false;
    }
    revokeAuthorization(authorization, reason);
    return true;
  }

  public OAuth2Authorization findByToken(String tokenValue) {
    if (StrUtil.isBlank(tokenValue)) {
      return null;
    }
    return authorizationService.findByToken(tokenValue, null);
  }

  public boolean isTokenValid(String tokenValue) {
    OAuth2Authorization authorization = findByToken(tokenValue);
    if (authorization == null) {
      return false;
    }
    if (tokenBlacklistService.isBlacklisted(tokenValue)) {
      return false;
    }

    OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
    if (accessToken != null && accessToken.getToken() != null) {
      Instant expiresAt = accessToken.getToken().getExpiresAt();
      if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
        return false;
      }
    }
    return true;
  }

  public boolean logout(String accessToken, String refreshToken) {
    boolean revoked = false;

    if (StrUtil.isNotBlank(accessToken)) {
      OAuth2Authorization authorization = findByToken(accessToken);
      if (authorization != null) {
        revokeAuthorization(authorization, "logout");
        revoked = true;
      }
    }

    if (StrUtil.isNotBlank(refreshToken)) {
      OAuth2Authorization authorization = findByToken(refreshToken);
      if (authorization != null) {
        revokeAuthorization(authorization, "logout");
        revoked = true;
      }
    }

    return revoked;
  }

  public long resolveTokenTtlSeconds(
      OAuth2Authorization authorization, String tokenValue, long fallbackTtlSeconds) {
    if (authorization == null || StrUtil.isBlank(tokenValue)) {
      return Math.max(fallbackTtlSeconds, 60L);
    }
    String normalizedTokenValue = tokenValue.trim();
    if (matchesTokenValue(authorization.getAccessToken(), normalizedTokenValue)) {
      return computeTtlSeconds(
          authorization.getAccessToken().getToken().getExpiresAt(),
          resolveRegisteredClientTtlSeconds(authorization, false, fallbackTtlSeconds));
    }
    if (matchesTokenValue(authorization.getRefreshToken(), normalizedTokenValue)) {
      return computeTtlSeconds(
          authorization.getRefreshToken().getToken().getExpiresAt(),
          resolveRegisteredClientTtlSeconds(authorization, true, fallbackTtlSeconds));
    }
    OAuth2Authorization.Token<
            org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode>
        authorizationCode =
            authorization.getToken(
                org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
                    .class);
    if (matchesTokenValue(authorizationCode, normalizedTokenValue)) {
      return computeTtlSeconds(
          authorizationCode.getToken().getExpiresAt(),
          resolveAuthorizationCodeTtlSeconds(authorization, fallbackTtlSeconds));
    }
    return Math.max(fallbackTtlSeconds, 60L);
  }

  public int logoutAllSessions(String username) {
    if (StrUtil.isBlank(username)) {
      return 0;
    }

    String principalKey = PRINCIPAL_PREFIX + username.trim();
    Set<Object> authorizationIds = redisTemplate.opsForSet().members(principalKey);
    if (authorizationIds == null || authorizationIds.isEmpty()) {
      return 0;
    }

    int revokedCount = 0;
    for (Object authorizationId : authorizationIds) {
      if (authorizationId == null) {
        continue;
      }
      OAuth2Authorization authorization =
          authorizationService.findById(String.valueOf(authorizationId));
      if (authorization == null) {
        redisTemplate.opsForSet().remove(principalKey, authorizationId);
        continue;
      }
      if (!username.equals(authorization.getPrincipalName())) {
        continue;
      }
      revokeAuthorization(authorization, "logout_all_sessions");
      revokedCount++;
    }
    return revokedCount;
  }

  private void revokeAuthorization(OAuth2Authorization authorization, String reason) {
    if (authorization.getAccessToken() != null
        && authorization.getAccessToken().getToken() != null) {
      String accessTokenValue = authorization.getAccessToken().getToken().getTokenValue();
      long ttl = resolveTokenTtlSeconds(authorization, accessTokenValue, 3600);
      tokenBlacklistService.addToBlacklist(
          accessTokenValue, authorization.getPrincipalName(), ttl, reason);
    }

    if (authorization.getRefreshToken() != null
        && authorization.getRefreshToken().getToken() != null) {
      String refreshTokenValue = authorization.getRefreshToken().getToken().getTokenValue();
      long ttl = resolveTokenTtlSeconds(authorization, refreshTokenValue, 2592000);
      tokenBlacklistService.addToBlacklist(
          refreshTokenValue, authorization.getPrincipalName(), ttl, reason);
    }

    authorizationService.remove(authorization);
  }

  private long computeTtlSeconds(Instant expiresAt, long defaultTtl) {
    if (expiresAt == null) {
      return Math.max(defaultTtl, 60L);
    }
    long seconds = Duration.between(Instant.now(), expiresAt).getSeconds();
    return Math.max(seconds, 60);
  }

  private boolean matchesTokenValue(
      OAuth2Authorization.Token<? extends OAuth2Token> token, String tokenValue) {
    return token != null
        && token.getToken() != null
        && StrUtil.equals(token.getToken().getTokenValue(), tokenValue);
  }

  private long resolveRegisteredClientTtlSeconds(
      OAuth2Authorization authorization, boolean refreshToken, long fallbackTtlSeconds) {
    if (authorization == null || StrUtil.isBlank(authorization.getRegisteredClientId())) {
      return Math.max(fallbackTtlSeconds, 60L);
    }
    RegisteredClient registeredClient =
        registeredClientRepository.findById(authorization.getRegisteredClientId());
    if (registeredClient == null || registeredClient.getTokenSettings() == null) {
      return Math.max(fallbackTtlSeconds, 60L);
    }
    Duration ttl =
        refreshToken
            ? registeredClient.getTokenSettings().getRefreshTokenTimeToLive()
            : registeredClient.getTokenSettings().getAccessTokenTimeToLive();
    if (ttl == null) {
      return Math.max(fallbackTtlSeconds, 60L);
    }
    return Math.max(ttl.toSeconds(), 60L);
  }

  private long resolveAuthorizationCodeTtlSeconds(
      OAuth2Authorization authorization, long fallbackTtlSeconds) {
    if (authorization == null || StrUtil.isBlank(authorization.getRegisteredClientId())) {
      return Math.max(fallbackTtlSeconds, 60L);
    }
    RegisteredClient registeredClient =
        registeredClientRepository.findById(authorization.getRegisteredClientId());
    if (registeredClient == null || registeredClient.getTokenSettings() == null) {
      return Math.max(fallbackTtlSeconds, 60L);
    }
    Duration ttl = registeredClient.getTokenSettings().getAuthorizationCodeTimeToLive();
    if (ttl == null) {
      return Math.max(fallbackTtlSeconds, 60L);
    }
    return Math.max(ttl.toSeconds(), 60L);
  }

  private AuthorizationServerContext resolveAuthorizationServerContext() {
    AuthorizationServerContext context = AuthorizationServerContextHolder.getContext();
    if (context != null) {
      return context;
    }
    AuthorizationServerSettings settings =
        AuthorizationServerSettings.builder().issuer(issuerUri).build();
    return new AuthorizationServerContext() {
      @Override
      public String getIssuer() {
        return settings.getIssuer();
      }

      @Override
      public AuthorizationServerSettings getAuthorizationServerSettings() {
        return settings;
      }
    };
  }
}
