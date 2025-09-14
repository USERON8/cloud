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

/**
 * 基于Redis的OAuth2授权服务实现
 * 用于在Redis中存储和检索OAuth2授权信息
 *
 * @author what's up
 */
@Slf4j
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    // Redis中存储OAuth2Authorization对象的key前缀
    private static final String AUTHORIZATION_PREFIX = "oauth2:authorization:";
    // Redis中存储token到authorizationId映射的key前缀
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

        // 保存完整的OAuth2Authorization对象
        String authorizationKey = AUTHORIZATION_PREFIX + authorization.getId();
        redisTemplate.opsForValue().set(authorizationKey, authorization);

        // 为不同类型的token建立索引，便于通过token值查找authorization
        // 授权码
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        if (authorizationCode != null) {
            String codeKey = TOKEN_PREFIX + authorizationCode.getToken().getTokenValue();
            redisTemplate.opsForValue().set(codeKey, authorization.getId(),
                    getExpireSeconds(authorizationCode.getToken()), TimeUnit.SECONDS);
        }

        // 访问令牌
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getAccessToken();
        if (accessToken != null) {
            String accessTokenKey = TOKEN_PREFIX + accessToken.getToken().getTokenValue();
            redisTemplate.opsForValue().set(accessTokenKey, authorization.getId(),
                    getExpireSeconds(accessToken.getToken()), TimeUnit.SECONDS);
        }

        // 刷新令牌
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                authorization.getRefreshToken();
        if (refreshToken != null) {
            String refreshTokenKey = TOKEN_PREFIX + refreshToken.getToken().getTokenValue();
            redisTemplate.opsForValue().set(refreshTokenKey, authorization.getId(),
                    getExpireSeconds(refreshToken.getToken()), TimeUnit.SECONDS);
        }

        // OIDC ID令牌 (修复版本)
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

        // 删除完整的OAuth2Authorization对象
        String authorizationKey = AUTHORIZATION_PREFIX + authorization.getId();
        redisTemplate.delete(authorizationKey);

        // 删除各种token的索引
        // 授权码索引
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        if (authorizationCode != null) {
            String codeKey = TOKEN_PREFIX + authorizationCode.getToken().getTokenValue();
            redisTemplate.delete(codeKey);
        }

        // 访问令牌索引
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getAccessToken();
        if (accessToken != null) {
            String accessTokenKey = TOKEN_PREFIX + accessToken.getToken().getTokenValue();
            redisTemplate.delete(accessTokenKey);
        }

        // 刷新令牌索引
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                authorization.getRefreshToken();
        if (refreshToken != null) {
            String refreshTokenKey = TOKEN_PREFIX + refreshToken.getToken().getTokenValue();
            redisTemplate.delete(refreshTokenKey);
        }

        // OIDC ID令牌索引 (修复版本)
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

        // 根据token类型查找对应的索引
        String tokenKey = TOKEN_PREFIX + token;

        // 通过索引获取authorizationId
        Object obj = redisTemplate.opsForValue().get(tokenKey);
        String authorizationId = obj instanceof String ? (String) obj : null;
        if (authorizationId == null) {
            return null;
        }

        // 根据authorizationId获取完整授权信息
        return findById(authorizationId);
    }

    /**
     * 计算token的过期秒数
     *
     * @param token OAuth2Token
     * @return 过期秒数
     */
    private long getExpireSeconds(OAuth2Token token) {
        Instant expiresAt = token.getExpiresAt();
        if (expiresAt != null) {
            return ChronoUnit.SECONDS.between(Instant.now(), expiresAt);
        }
        // 默认2小时
        return 7200L;
    }
}