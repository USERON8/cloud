package com.cloud.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;











@Slf4j
public class SimpleRedisHashOAuth2AuthorizationService implements OAuth2AuthorizationService {

    
    private static final String AUTHORIZATION_HASH_PREFIX = "oauth2:auth:";

    
    private static final String TOKEN_INDEX_PREFIX = "oauth2:token:";

    
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
        this.objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        String authHashKey = AUTHORIZATION_HASH_PREFIX + authorization.getId();

        try {
            
            String authorizationJson = objectMapper.writeValueAsString(authorization);

            
            hashOperations.put(authHashKey, FIELD_AUTHORIZATION_DATA, authorizationJson);
            hashOperations.put(authHashKey, FIELD_CLIENT_ID, authorization.getRegisteredClientId());
            hashOperations.put(authHashKey, FIELD_PRINCIPAL_NAME, authorization.getPrincipalName());
            hashOperations.put(authHashKey, FIELD_CREATE_TIME, Instant.now().toString());

            
            long expireSeconds = calculateMaxExpireSeconds(authorization);
            if (expireSeconds > 0) {
                redisTemplate.expire(authHashKey, expireSeconds, TimeUnit.SECONDS);
            }

            
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
            
            redisTemplate.delete(authHashKey);

            
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

        
        String tokenIndexKey = TOKEN_INDEX_PREFIX + token;
        Object authorizationIdObj = redisTemplate.opsForValue().get(tokenIndexKey);

        if (!(authorizationIdObj instanceof String authorizationId)) {
            return null;
        }

        return findById(authorizationId);
    }

    


    private void createTokenIndexes(OAuth2Authorization authorization) {
        String authorizationId = authorization.getId();

        
        createTokenIndex(authorization.getToken(OAuth2AuthorizationCode.class), authorizationId);
        createTokenIndex(authorization.getAccessToken(), authorizationId);
        createTokenIndex(authorization.getRefreshToken(), authorizationId);
        createTokenIndex(authorization.getToken(OidcIdToken.class), authorizationId);
    }

    


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

    


    private void removeTokenIndexes(OAuth2Authorization authorization) {
        removeTokenIndex(authorization.getToken(OAuth2AuthorizationCode.class));
        removeTokenIndex(authorization.getAccessToken());
        removeTokenIndex(authorization.getRefreshToken());
        removeTokenIndex(authorization.getToken(OidcIdToken.class));
    }

    


    private void removeTokenIndex(OAuth2Authorization.Token<?> tokenHolder) {
        if (tokenHolder != null && tokenHolder.getToken() != null) {
            String tokenIndexKey = TOKEN_INDEX_PREFIX + tokenHolder.getToken().getTokenValue();
            redisTemplate.delete(tokenIndexKey);
        }
    }

    


    private long calculateMaxExpireSeconds(OAuth2Authorization authorization) {
        long maxExpireSeconds = 7200L; 

        maxExpireSeconds = Math.max(maxExpireSeconds, getTokenExpireSeconds(authorization.getToken(OAuth2AuthorizationCode.class)));
        maxExpireSeconds = Math.max(maxExpireSeconds, getTokenExpireSeconds(authorization.getAccessToken()));
        maxExpireSeconds = Math.max(maxExpireSeconds, getTokenExpireSeconds(authorization.getRefreshToken()));
        maxExpireSeconds = Math.max(maxExpireSeconds, getTokenExpireSeconds(authorization.getToken(OidcIdToken.class)));

        return maxExpireSeconds;
    }

    


    private long getTokenExpireSeconds(OAuth2Authorization.Token<?> tokenHolder) {
        if (tokenHolder != null && tokenHolder.getToken() != null) {
            return getTokenExpireSeconds(tokenHolder.getToken());
        }
        return 0;
    }

    


    private long getTokenExpireSeconds(OAuth2Token token) {
        if (token != null && token.getExpiresAt() != null) {
            long seconds = ChronoUnit.SECONDS.between(Instant.now(), token.getExpiresAt());
            return Math.max(seconds, 60); 
        }
        return 7200L; 
    }

    


    public TokenStorageStats getStorageStats() {
        
        
        return new TokenStorageStats();
    }

    


    public void cleanupExpiredTokens() {
        
        
        
    }

    


    public static class TokenStorageStats {
        private long authorizationCount;
        private long tokenIndexCount;
        private long memoryUsage;

        
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
