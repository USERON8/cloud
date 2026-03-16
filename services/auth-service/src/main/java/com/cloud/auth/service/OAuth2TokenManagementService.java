package com.cloud.auth.service;

import cn.hutool.core.util.StrUtil;
import com.cloud.auth.util.RedisKeyHelper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenManagementService {

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

  public int logoutAllSessions(String username) {
    if (StrUtil.isBlank(username)) {
      return 0;
    }

    Set<String> authKeys = RedisKeyHelper.scanKeys(redisTemplate, "oauth2:token:*");
    if (authKeys == null || authKeys.isEmpty()) {
      return 0;
    }

    List<String> authKeyList = authKeys.stream().toList();
    List<Object> authorizations = redisTemplate.opsForValue().multiGet(authKeyList);
    int revokedCount = 0;
    if (authorizations != null) {
      for (Object obj : authorizations) {
        if (!(obj instanceof OAuth2Authorization authorization)) {
          continue;
        }
        if (!username.equals(authorization.getPrincipalName())) {
          continue;
        }
        revokeAuthorization(authorization, "logout_all_sessions");
        revokedCount++;
      }
    }
    return revokedCount;
  }

  private void revokeAuthorization(OAuth2Authorization authorization, String reason) {
    try {
      if (authorization.getAccessToken() != null
          && authorization.getAccessToken().getToken() != null) {
        String accessTokenValue = authorization.getAccessToken().getToken().getTokenValue();
        long ttl =
            computeTtlSeconds(authorization.getAccessToken().getToken().getExpiresAt(), 3600);
        tokenBlacklistService.addToBlacklist(
            accessTokenValue, authorization.getPrincipalName(), ttl, reason);
      }

      if (authorization.getRefreshToken() != null
          && authorization.getRefreshToken().getToken() != null) {
        String refreshTokenValue = authorization.getRefreshToken().getToken().getTokenValue();
        long ttl =
            computeTtlSeconds(authorization.getRefreshToken().getToken().getExpiresAt(), 2592000);
        tokenBlacklistService.addToBlacklist(
            refreshTokenValue, authorization.getPrincipalName(), ttl, reason);
      }

      authorizationService.remove(authorization);
    } catch (Exception e) {
      log.warn("Failed to revoke authorization id={}: {}", authorization.getId(), e.getMessage());
    }
  }

  private long computeTtlSeconds(Instant expiresAt, long defaultTtl) {
    if (expiresAt == null) {
      return defaultTtl;
    }
    long seconds = Duration.between(Instant.now(), expiresAt).getSeconds();
    return Math.max(seconds, 60);
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
