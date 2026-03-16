package com.cloud.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.util.Assert;

@Slf4j
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

  private static final String TOKEN_PREFIX = "oauth2:token:";
  private static final String REFRESH_PREFIX = "oauth2:refresh:";
  private static final String CODE_PREFIX = "oauth2:code:";
  private static final OAuth2TokenType AUTHORIZATION_CODE =
      new OAuth2TokenType(OAuth2ParameterNames.CODE);
  private final RedisTemplate<String, Object> redisTemplate;
  private final RegisteredClientRepository registeredClientRepository;
  private final AuthorizationServerSettings authorizationServerSettings;

  public RedisOAuth2AuthorizationService(
      RedisTemplate<String, Object> redisTemplate,
      RegisteredClientRepository registeredClientRepository,
      AuthorizationServerSettings authorizationServerSettings) {
    Assert.notNull(redisTemplate, "redisTemplate cannot be null");
    Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
    Assert.notNull(authorizationServerSettings, "authorizationServerSettings cannot be null");
    this.redisTemplate = redisTemplate;
    this.registeredClientRepository = registeredClientRepository;
    this.authorizationServerSettings = authorizationServerSettings;
  }

  @Override
  public void save(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization cannot be null");

    String authorizationKey = TOKEN_PREFIX + authorization.getId();
    long authorizationTtl = resolveAuthorizationTtlSeconds(authorization);
    if (authorizationTtl > 0) {
      redisTemplate
          .opsForValue()
          .set(authorizationKey, authorization, authorizationTtl, TimeUnit.SECONDS);
    } else {
      redisTemplate.opsForValue().set(authorizationKey, authorization);
    }

    OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
        authorization.getToken(OAuth2AuthorizationCode.class);
    if (authorizationCode != null) {
      String codeKey = CODE_PREFIX + authorizationCode.getToken().getTokenValue();
      redisTemplate
          .opsForValue()
          .set(
              codeKey,
              authorization.getId(),
              getExpireSeconds(authorizationCode.getToken()),
              TimeUnit.SECONDS);
    }

    OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
    if (refreshToken != null) {
      String refreshTokenKey = REFRESH_PREFIX + refreshToken.getToken().getTokenValue();
      redisTemplate
          .opsForValue()
          .set(
              refreshTokenKey,
              authorization.getId(),
              getExpireSeconds(refreshToken.getToken()),
              TimeUnit.SECONDS);
    }

    log.debug("Saved authorization with id: {}", authorization.getId());
  }

  @Override
  public void remove(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization cannot be null");

    String authorizationKey = TOKEN_PREFIX + authorization.getId();
    redisTemplate.delete(authorizationKey);

    OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
        authorization.getToken(OAuth2AuthorizationCode.class);
    if (authorizationCode != null) {
      String codeKey = CODE_PREFIX + authorizationCode.getToken().getTokenValue();
      redisTemplate.delete(codeKey);
    }

    OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
    if (refreshToken != null) {
      String refreshTokenKey = REFRESH_PREFIX + refreshToken.getToken().getTokenValue();
      redisTemplate.delete(refreshTokenKey);
    }

    log.debug("Removed authorization with id: {}", authorization.getId());
  }

  @Override
  public OAuth2Authorization findById(String id) {
    Assert.hasText(id, "id cannot be empty");
    String key = TOKEN_PREFIX + id;
    Object obj = redisTemplate.opsForValue().get(key);
    return obj instanceof OAuth2Authorization ? (OAuth2Authorization) obj : null;
  }

  @Override
  public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
    Assert.hasText(token, "token cannot be empty");

    if (tokenType != null) {
      if (AUTHORIZATION_CODE.equals(tokenType)) {
        return findByAuthorizationId(CODE_PREFIX + token);
      }
      if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
        return findByAuthorizationId(REFRESH_PREFIX + token);
      }
    }

    return findByAccessTokenValue(token, tokenType);
  }

  private OAuth2Authorization findByAuthorizationId(String tokenKey) {
    Object obj = redisTemplate.opsForValue().get(tokenKey);
    String authorizationId = obj instanceof String ? (String) obj : null;
    if (authorizationId == null) {
      return null;
    }
    return findById(authorizationId);
  }

  private OAuth2Authorization findByAccessTokenValue(String token, OAuth2TokenType tokenType) {
    var keys = redisTemplate.keys(TOKEN_PREFIX + "*");
    if (keys == null || keys.isEmpty()) {
      return null;
    }
    for (String key : keys) {
      Object obj = redisTemplate.opsForValue().get(key);
      if (!(obj instanceof OAuth2Authorization authorization)) {
        continue;
      }
      if (matchToken(authorization, token, tokenType)) {
        return authorization;
      }
    }
    return null;
  }

  private boolean matchToken(
      OAuth2Authorization authorization, String token, OAuth2TokenType tokenType) {
    if (authorization == null || token == null) {
      return false;
    }
    if (tokenType == null || OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
      OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
      if (accessToken != null && accessToken.getToken() != null) {
        return token.equals(accessToken.getToken().getTokenValue());
      }
    }
    if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
      OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
      return refreshToken != null
          && refreshToken.getToken() != null
          && token.equals(refreshToken.getToken().getTokenValue());
    }
    if (AUTHORIZATION_CODE.equals(tokenType)) {
      OAuth2Authorization.Token<OAuth2AuthorizationCode> code =
          authorization.getToken(OAuth2AuthorizationCode.class);
      return code != null
          && code.getToken() != null
          && token.equals(code.getToken().getTokenValue());
    }
    return false;
  }

  private long getExpireSeconds(OAuth2Token token) {
    Instant expiresAt = token.getExpiresAt();
    if (expiresAt != null) {
      return ChronoUnit.SECONDS.between(Instant.now(), expiresAt);
    }

    return 7200L;
  }

  private long resolveAuthorizationTtlSeconds(OAuth2Authorization authorization) {
    long maxSeconds = 0L;
    if (authorization == null) {
      return maxSeconds;
    }
    maxSeconds =
        Math.max(
            maxSeconds,
            getExpireSeconds(
                authorization.getAccessToken() != null
                    ? authorization.getAccessToken().getToken()
                    : null));
    maxSeconds =
        Math.max(
            maxSeconds,
            getExpireSeconds(
                authorization.getRefreshToken() != null
                    ? authorization.getRefreshToken().getToken()
                    : null));
    maxSeconds =
        Math.max(
            maxSeconds,
            getExpireSeconds(
                authorization.getToken(OAuth2AuthorizationCode.class) != null
                    ? authorization.getToken(OAuth2AuthorizationCode.class).getToken()
                    : null));
    return maxSeconds;
  }
}
