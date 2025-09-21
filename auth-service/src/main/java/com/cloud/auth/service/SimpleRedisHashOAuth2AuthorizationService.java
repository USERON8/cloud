package com.cloud.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
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
 * 基于Redis Hash的简化版OAuth2授权服务实现
 * 主要特点：
 * 1. 使用Redis Hash存储完整的OAuth2Authorization对象（JSON序列化）
 * 2. 为每个token创建独立的索引，支持通过token快速查找
 * 3. 自动管理过期时间，确保数据一致性
 * 4. 使用common-module中的RedisHashCacheService进行Hash操作
 *
 * @author what's up
 */
@Slf4j
public class SimpleRedisHashOAuth2AuthorizationService implements OAuth2AuthorizationService {

    // Redis Hash存储OAuth2Authorization完整对象的key前缀
    private static final String AUTHORIZATION_HASH_PREFIX = "oauth2:auth:";

    // Redis存储token索引的key前缀  
    private static final String TOKEN_INDEX_PREFIX = "oauth2:token:";

    // Hash中存储完整OAuth2Authorization JSON的字段名
    private static final String FIELD_AUTHORIZATION_DATA = "data";
    private static final String FIELD_CLIENT_ID = "clientId";
    private static final String FIELD_PRINCIPAL_NAME = "principalName";
    private static final String FIELD_CREATE_TIME = "createTime";

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Object> hashOperations;
    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationServerSettings authorizationServerSettings;
    private final ObjectMapper objectMapper;

    public SimpleRedisHashOAuth2AuthorizationService(RedisTemplate<String, Object> redisTemplate,
                                                     RegisteredClientRepository registeredClientRepository,
                                                     AuthorizationServerSettings authorizationServerSettings) {
        Assert.notNull(redisTemplate, "redisTemplate cannot be null");
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
        Assert.notNull(authorizationServerSettings, "authorizationServerSettings cannot be null");

        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationServerSettings = authorizationServerSettings;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        String authHashKey = AUTHORIZATION_HASH_PREFIX + authorization.getId();

        try {
            // 1. 序列化完整的OAuth2Authorization对象
            String authorizationJson = objectMapper.writeValueAsString(authorization);

            // 2. 构建Hash数据
            hashOperations.put(authHashKey, FIELD_AUTHORIZATION_DATA, authorizationJson);
            hashOperations.put(authHashKey, FIELD_CLIENT_ID, authorization.getRegisteredClientId());
            hashOperations.put(authHashKey, FIELD_PRINCIPAL_NAME, authorization.getPrincipalName());
            hashOperations.put(authHashKey, FIELD_CREATE_TIME, Instant.now().toString());

            // 3. 设置Hash过期时间
            long expireSeconds = calculateMaxExpireSeconds(authorization);
            if (expireSeconds > 0) {
                redisTemplate.expire(authHashKey, expireSeconds, TimeUnit.SECONDS);
            }

            // 4. 创建token索引
            createTokenIndexes(authorization);

            log.debug("Saved authorization with id: {} using Hash storage", authorization.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OAuth2Authorization for id: {}", authorization.getId(), e);
            throw new RuntimeException("Failed to serialize OAuth2Authorization", e);
        } catch (Exception e) {
            log.error("Failed to save authorization with id: {}", authorization.getId(), e);
            throw new RuntimeException("Failed to save OAuth2Authorization", e);
        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        String authHashKey = AUTHORIZATION_HASH_PREFIX + authorization.getId();

        try {
            // 1. 删除授权信息Hash
            redisTemplate.delete(authHashKey);

            // 2. 删除token索引
            removeTokenIndexes(authorization);

            log.debug("Removed authorization with id: {} from Hash storage", authorization.getId());

        } catch (Exception e) {
            log.error("Failed to remove authorization with id: {}", authorization.getId(), e);
        }
    }

    @Override
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");

        String authHashKey = AUTHORIZATION_HASH_PREFIX + id;

        try {
            Object authDataObj = hashOperations.get(authHashKey, FIELD_AUTHORIZATION_DATA);
            if (authDataObj instanceof String authorizationJson) {
                return objectMapper.readValue(authorizationJson, OAuth2Authorization.class);
            }
            return null;

        } catch (Exception e) {
            log.error("Failed to deserialize OAuth2Authorization for id: {}", id, e);
            return null;
        }
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");

        // 通过token索引找到authorizationId
        String tokenIndexKey = TOKEN_INDEX_PREFIX + token;
        Object authorizationIdObj = redisTemplate.opsForValue().get(tokenIndexKey);

        if (!(authorizationIdObj instanceof String authorizationId)) {
            return null;
        }

        return findById(authorizationId);
    }

    /**
     * 创建token索引
     */
    private void createTokenIndexes(OAuth2Authorization authorization) {
        String authorizationId = authorization.getId();

        // 创建各种token的索引
        createTokenIndex(authorization.getToken(OAuth2AuthorizationCode.class), authorizationId);
        createTokenIndex(authorization.getAccessToken(), authorizationId);
        createTokenIndex(authorization.getRefreshToken(), authorizationId);
        createTokenIndex(authorization.getToken(OidcIdToken.class), authorizationId);
    }

    /**
     * 创建单个token索引
     */
    private void createTokenIndex(OAuth2Authorization.Token<?> tokenHolder, String authorizationId) {
        if (tokenHolder != null && tokenHolder.getToken() != null) {
            String tokenValue = tokenHolder.getToken().getTokenValue();
            String tokenIndexKey = TOKEN_INDEX_PREFIX + tokenValue;

            long expireSeconds = getTokenExpireSeconds(tokenHolder.getToken());
            if (expireSeconds > 0) {
                redisTemplate.opsForValue().set(tokenIndexKey, authorizationId, expireSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(tokenIndexKey, authorizationId);
            }

            log.debug("Created token index: {} -> {}", tokenValue.substring(0, Math.min(10, tokenValue.length())) + "...", authorizationId);
        }
    }

    /**
     * 删除token索引
     */
    private void removeTokenIndexes(OAuth2Authorization authorization) {
        removeTokenIndex(authorization.getToken(OAuth2AuthorizationCode.class));
        removeTokenIndex(authorization.getAccessToken());
        removeTokenIndex(authorization.getRefreshToken());
        removeTokenIndex(authorization.getToken(OidcIdToken.class));
    }

    /**
     * 删除单个token索引
     */
    private void removeTokenIndex(OAuth2Authorization.Token<?> tokenHolder) {
        if (tokenHolder != null && tokenHolder.getToken() != null) {
            String tokenIndexKey = TOKEN_INDEX_PREFIX + tokenHolder.getToken().getTokenValue();
            redisTemplate.delete(tokenIndexKey);
        }
    }

    /**
     * 计算最大过期秒数（以最长的token过期时间为准）
     */
    private long calculateMaxExpireSeconds(OAuth2Authorization authorization) {
        long maxExpireSeconds = 7200L; // 默认2小时

        maxExpireSeconds = Math.max(maxExpireSeconds, getTokenExpireSeconds(authorization.getToken(OAuth2AuthorizationCode.class)));
        maxExpireSeconds = Math.max(maxExpireSeconds, getTokenExpireSeconds(authorization.getAccessToken()));
        maxExpireSeconds = Math.max(maxExpireSeconds, getTokenExpireSeconds(authorization.getRefreshToken()));
        maxExpireSeconds = Math.max(maxExpireSeconds, getTokenExpireSeconds(authorization.getToken(OidcIdToken.class)));

        return maxExpireSeconds;
    }

    /**
     * 获取token过期秒数
     */
    private long getTokenExpireSeconds(OAuth2Authorization.Token<?> tokenHolder) {
        if (tokenHolder != null && tokenHolder.getToken() != null) {
            return getTokenExpireSeconds(tokenHolder.getToken());
        }
        return 0;
    }

    /**
     * 计算token的过期秒数
     */
    private long getTokenExpireSeconds(OAuth2Token token) {
        if (token != null && token.getExpiresAt() != null) {
            long seconds = ChronoUnit.SECONDS.between(Instant.now(), token.getExpiresAt());
            return Math.max(seconds, 60); // 至少1分钟
        }
        return 7200L; // 默认2小时
    }

    /**
     * 获取存储统计信息
     */
    public TokenStorageStats getStorageStats() {
        // 这里可以添加一些统计信息的查询
        // 比如当前存储的authorization数量、token索引数量等
        return new TokenStorageStats();
    }

    /**
     * 清理过期的token索引（可选的维护方法）
     */
    public void cleanupExpiredTokens() {
        // 由于Redis的自动过期机制，大部分情况下不需要手动清理
        // 这个方法可以用于一些特殊的清理场景
        log.info("Token cleanup completed - relying on Redis TTL for automatic cleanup");
    }

    /**
     * token存储统计信息
     */
    public static class TokenStorageStats {
        private long authorizationCount;
        private long tokenIndexCount;
        private long memoryUsage;

        // getters and setters
        public long getAuthorizationCount() {
            return authorizationCount;
        }

        public void setAuthorizationCount(long authorizationCount) {
            this.authorizationCount = authorizationCount;
        }

        public long getTokenIndexCount() {
            return tokenIndexCount;
        }

        public void setTokenIndexCount(long tokenIndexCount) {
            this.tokenIndexCount = tokenIndexCount;
        }

        public long getMemoryUsage() {
            return memoryUsage;
        }

        public void setMemoryUsage(long memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
    }
}
