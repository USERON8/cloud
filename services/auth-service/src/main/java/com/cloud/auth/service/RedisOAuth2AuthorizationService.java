package com.cloud.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;







@Slf4j
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    
    private static final String AUTHORIZATION_PREFIX = "oauth2:authorization:";
    
    private static final String TOKEN_PREFIX = "oauth2:token:";
    private final RedisTemplate<String, Object> redisTemplate;
    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationServerSettings authorizationServerSettings;

    public RedisOAuth2AuthorizationService(RedisTemplate<String, Object> redisTemplate,
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

        
        String authorizationKey = AUTHORIZATION_PREFIX + authorization.getId();
        redisTemplate.opsForValue().set(authorizationKey, authorization);

        
        
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        if (authorizationCode != null) {
            String codeKey = TOKEN_PREFIX + authorizationCode.getToken().getTokenValue();
            redisTemplate.opsForValue().set(codeKey, authorization.getId(),
                    getExpireSeconds(authorizationCode.getToken()), TimeUnit.SECONDS);
        }

        
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getAccessToken();
        if (accessToken != null) {
            String accessTokenKey = TOKEN_PREFIX + accessToken.getToken().getTokenValue();
            redisTemplate.opsForValue().set(accessTokenKey, authorization.getId(),
                    getExpireSeconds(accessToken.getToken()), TimeUnit.SECONDS);
        }

        
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                authorization.getRefreshToken();
        if (refreshToken != null) {
            String refreshTokenKey = TOKEN_PREFIX + refreshToken.getToken().getTokenValue();
            redisTemplate.opsForValue().set(refreshTokenKey, authorization.getId(),
                    getExpireSeconds(refreshToken.getToken()), TimeUnit.SECONDS);
        }

        
        OAuth2Authorization.Token<OidcIdToken> oidcIdToken =
                authorization.getToken(OidcIdToken.class);
        if (oidcIdToken != null) {
            String oidcIdTokenKey = TOKEN_PREFIX + oidcIdToken.getToken().getTokenValue();
            redisTemplate.opsForValue().set(oidcIdTokenKey, authorization.getId(),
                    getExpireSeconds(oidcIdToken.getToken()), TimeUnit.SECONDS);
        }

        log.debug("Saved authorization with id: {}", authorization.getId());
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        
        String authorizationKey = AUTHORIZATION_PREFIX + authorization.getId();
        redisTemplate.delete(authorizationKey);

        
        
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        if (authorizationCode != null) {
            String codeKey = TOKEN_PREFIX + authorizationCode.getToken().getTokenValue();
            redisTemplate.delete(codeKey);
        }

        
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getAccessToken();
        if (accessToken != null) {
            String accessTokenKey = TOKEN_PREFIX + accessToken.getToken().getTokenValue();
            redisTemplate.delete(accessTokenKey);
        }

        
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                authorization.getRefreshToken();
        if (refreshToken != null) {
            String refreshTokenKey = TOKEN_PREFIX + refreshToken.getToken().getTokenValue();
            redisTemplate.delete(refreshTokenKey);
        }

        
        OAuth2Authorization.Token<OidcIdToken> oidcIdToken =
                authorization.getToken(OidcIdToken.class);
        if (oidcIdToken != null) {
            String oidcIdTokenKey = TOKEN_PREFIX + oidcIdToken.getToken().getTokenValue();
            redisTemplate.delete(oidcIdTokenKey);
        }

        log.debug("Removed authorization with id: {}", authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        String key = AUTHORIZATION_PREFIX + id;
        Object obj = redisTemplate.opsForValue().get(key);
        return obj instanceof OAuth2Authorization ? (OAuth2Authorization) obj : null;
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");

        
        String tokenKey = TOKEN_PREFIX + token;

        
        Object obj = redisTemplate.opsForValue().get(tokenKey);
        String authorizationId = obj instanceof String ? (String) obj : null;
        if (authorizationId == null) {
            return null;
        }

        
        return findById(authorizationId);
    }

    





    private long getExpireSeconds(OAuth2Token token) {
        Instant expiresAt = token.getExpiresAt();
        if (expiresAt != null) {
            return ChronoUnit.SECONDS.between(Instant.now(), expiresAt);
        }
        
        return 7200L;
    }
}
