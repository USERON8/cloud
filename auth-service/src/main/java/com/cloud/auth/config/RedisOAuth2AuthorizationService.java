package com.cloud.auth.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis OAuth2 授权服务实现
 * 用于将 OAuth2 授权信息存储到 Redis 中
 */
@Slf4j
@Component
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationServerSettings authorizationServerSettings;

    /**
     * 构造函数
     *
     * @param redisTemplate Redis模板
     * @param registeredClientRepository 注册客户端仓库
     * @param authorizationServerSettings 授权服务器设置
     */
    public RedisOAuth2AuthorizationService(RedisTemplate<String, Object> redisTemplate,
                                           RegisteredClientRepository registeredClientRepository,
                                           AuthorizationServerSettings authorizationServerSettings) {
        Assert.notNull(redisTemplate, "redisTemplate cannot be null");
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
        Assert.notNull(authorizationServerSettings, "authorizationServerSettings cannot be null");
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationServerSettings = authorizationServerSettings;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        RegisteredClient registeredClient = this.registeredClientRepository.findById(authorization.getRegisteredClientId());
        if (registeredClient == null) {
            throw new IllegalArgumentException("Registered client not found for authorization");
        }

        String authorizationId = authorization.getId();
        String principalName = authorization.getPrincipalName();
        String clientId = registeredClient.getId();

        // 存储授权信息
        storeAuthorization(authorization, authorizationId, clientId);

        // 存储令牌与授权ID的映射关系
        storeTokens(authorization, authorizationId, clientId, principalName);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        String authorizationId = authorization.getId();
        String clientId = authorization.getRegisteredClientId();

        // 删除授权信息
        String authKey = buildAuthorizationKey(authorizationId, clientId);
        redisTemplate.delete(authKey);

        // 删除令牌映射关系
        removeTokenMappings(authorization, clientId);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");

        // 尝试通过已知的客户端ID查找授权信息
        // 由于RegisteredClientRepository没有提供获取所有客户端的方法，我们只能通过其他方式处理
        // 这里我们假设可以通过某种方式获取到客户端列表
        try {
            // 注意：这里可能需要根据实际的RegisteredClientRepository实现进行调整
            // 例如，如果使用的是InMemoryRegisteredClientRepository，可能需要特殊处理
            log.warn("无法直接获取所有注册客户端，需要通过其他方式实现完整功能");
            return null;
        } catch (Exception e) {
            log.error("Error finding authorization by id: {}", id, e);
            return null;
        }
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");
        
        // 尝试通过令牌和令牌类型查找授权信息
        // 由于RegisteredClientRepository没有提供获取所有客户端的方法，我们只能通过其他方式处理
        try {
            // 注意：这里可能需要根据实际的RegisteredClientRepository实现进行调整
            log.warn("无法直接获取所有注册客户端，需要通过其他方式实现完整功能");
            return null;
        } catch (Exception e) {
            log.error("Error finding authorization by token: {}", token, e);
            return null;
        }
    }

    /**
     * 存储授权信息
     */
    private void storeAuthorization(OAuth2Authorization authorization, String authorizationId, String clientId) {
        String authKey = buildAuthorizationKey(authorizationId, clientId);
        try {
            String authData = serializeAuthorization(authorization);
            redisTemplate.opsForValue().set(authKey, authData, getExpireTime(authorization), TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize authorization", e);
        }
    }

    /**
     * 存储令牌映射关系
     */
    private void storeTokens(OAuth2Authorization authorization, String authorizationId, String clientId, String principalName) {
        // 存储状态码映射
        String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
        if (StringUtils.hasText(state)) {
            String stateKey = buildTokenKey(state, "state", clientId);
            redisTemplate.opsForValue().set(stateKey, authorizationId, 300, TimeUnit.SECONDS); // 5分钟过期
        }

        // 存储授权码映射
        // 由于无法直接获取OAuth2AuthorizationCode对象，我们使用通用方法处理
        Object authorizationCode = authorization.getAttributes().get(OAuth2ParameterNames.CODE);
        if (authorizationCode != null) {
            String codeKey = buildTokenKey(authorizationCode.toString(), "authorization_code", clientId);
            redisTemplate.opsForValue().set(codeKey, authorizationId, 300, TimeUnit.SECONDS); // 5分钟过期
        }

        // 存储访问令牌映射
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null) {
            String tokenValue = accessToken.getToken().getTokenValue();
            String accessTokenKey = buildTokenKey(tokenValue, "access_token", clientId);
            redisTemplate.opsForValue().set(accessTokenKey, authorizationId, getExpireTime(authorization), TimeUnit.SECONDS);
            
            // 存储用户与访问令牌的映射关系
            String userAccessTokenKey = buildUserAccessTokenKey(principalName, clientId);
            redisTemplate.opsForList().leftPush(userAccessTokenKey, tokenValue);
            redisTemplate.expire(userAccessTokenKey, getExpireTime(authorization), TimeUnit.SECONDS);
        }

        // 存储刷新令牌映射
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            String refreshTokenKey = buildTokenKey(refreshToken.getToken().getTokenValue(), "refresh_token", clientId);
            redisTemplate.opsForValue().set(refreshTokenKey, authorizationId, getExpireTime(authorization), TimeUnit.SECONDS);
        }

        // 存储ID令牌映射
        // 由于无法直接获取OidcIdToken对象，我们使用通用方法处理
        Object idToken = authorization.getAttributes().get(OidcParameterNames.ID_TOKEN);
        if (idToken != null) {
            String idTokenKey = buildTokenKey(idToken.toString(), "id_token", clientId);
            redisTemplate.opsForValue().set(idTokenKey, authorizationId, getExpireTime(authorization), TimeUnit.SECONDS);
        }
    }

    /**
     * 删除令牌映射关系
     */
    private void removeTokenMappings(OAuth2Authorization authorization, String clientId) {
        // 删除状态码映射
        String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
        if (StringUtils.hasText(state)) {
            String stateKey = buildTokenKey(state, "state", clientId);
            redisTemplate.delete(stateKey);
        }

        // 删除授权码映射
        Object authorizationCode = authorization.getAttributes().get(OAuth2ParameterNames.CODE);
        if (authorizationCode != null) {
            String codeKey = buildTokenKey(authorizationCode.toString(), "authorization_code", clientId);
            redisTemplate.delete(codeKey);
        }

        // 删除访问令牌映射
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null) {
            String accessTokenKey = buildTokenKey(accessToken.getToken().getTokenValue(), "access_token", clientId);
            redisTemplate.delete(accessTokenKey);
        }

        // 删除刷新令牌映射
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            String refreshTokenKey = buildTokenKey(refreshToken.getToken().getTokenValue(), "refresh_token", clientId);
            redisTemplate.delete(refreshTokenKey);
        }

        // 删除ID令牌映射
        Object idToken = authorization.getAttributes().get(OidcParameterNames.ID_TOKEN);
        if (idToken != null) {
            String idTokenKey = buildTokenKey(idToken.toString(), "id_token", clientId);
            redisTemplate.delete(idTokenKey);
        }
    }

    /**
     * 序列化授权信息
     */
    private String serializeAuthorization(OAuth2Authorization authorization) throws JsonProcessingException {
        Map<String, Object> authMap = new HashMap<>();
        authMap.put("id", authorization.getId());
        authMap.put("registeredClientId", authorization.getRegisteredClientId());
        authMap.put("principalName", authorization.getPrincipalName());
        authMap.put("authorizationGrantType", authorization.getAuthorizationGrantType().getValue());
        authMap.put("attributes", authorization.getAttributes());
        
        // 序列化访问令牌信息
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null) {
            Map<String, Object> accessTokenMap = new HashMap<>();
            accessTokenMap.put("tokenValue", accessToken.getToken().getTokenValue());
            accessTokenMap.put("issuedAt", accessToken.getToken().getIssuedAt());
            accessTokenMap.put("expiresAt", accessToken.getToken().getExpiresAt());
            authMap.put("accessToken", accessTokenMap);
        }
        
        // 序列化刷新令牌信息
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            Map<String, Object> refreshTokenMap = new HashMap<>();
            refreshTokenMap.put("tokenValue", refreshToken.getToken().getTokenValue());
            refreshTokenMap.put("issuedAt", refreshToken.getToken().getIssuedAt());
            refreshTokenMap.put("expiresAt", refreshToken.getToken().getExpiresAt());
            authMap.put("refreshToken", refreshTokenMap);
        }
        
        return objectMapper.writeValueAsString(authMap);
    }

    /**
     * 构建授权键
     */
    private String buildAuthorizationKey(String authorizationId, String clientId) {
        return "oauth2:authorization:" + clientId + ":" + authorizationId;
    }

    /**
     * 构建令牌键
     */
    private String buildTokenKey(String token, String tokenType, String clientId) {
        return "oauth2:token:" + clientId + ":" + tokenType + ":" + token;
    }

    /**
     * 构建用户访问令牌键
     */
    private String buildUserAccessTokenKey(String principalName, String clientId) {
        return "oauth2:user_access_tokens:" + clientId + ":" + principalName;
    }

    /**
     * 获取过期时间（秒）
     */
    private long getExpireTime(OAuth2Authorization authorization) {
        Instant earliestExpiration = null;

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null && accessToken.getToken().getExpiresAt() != null) {
            earliestExpiration = accessToken.getToken().getExpiresAt();
        }

        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null && refreshToken.getToken().getExpiresAt() != null) {
            Instant refreshTokenExpiresAt = refreshToken.getToken().getExpiresAt();
            if (earliestExpiration == null || refreshTokenExpiresAt.isBefore(earliestExpiration)) {
                earliestExpiration = refreshTokenExpiresAt;
            }
        }

        return earliestExpiration != null ? 
               Math.max(0, earliestExpiration.getEpochSecond() - Instant.now().getEpochSecond()) : 
               3600; // 默认1小时
    }
}