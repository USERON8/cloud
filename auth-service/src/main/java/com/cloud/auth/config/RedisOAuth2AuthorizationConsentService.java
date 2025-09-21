package com.cloud.auth.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis OAuth2 授权同意服务实现
 * 用于将 OAuth2 授权同意信息存储到 Redis 中
 */
@Slf4j
@Service
public class RedisOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param redisTemplate Redis模板
     * @param registeredClientRepository 注册客户端仓库
     */
    public RedisOAuth2AuthorizationConsentService(RedisTemplate<String, Object> redisTemplate,
                                                  RegisteredClientRepository registeredClientRepository) {
        Assert.notNull(redisTemplate, "redisTemplate cannot be null");
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
        this.redisTemplate = redisTemplate;
        this.registeredClientRepository = registeredClientRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
        String key = buildConsentKey(authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
        try {
            String consentData = objectMapper.writeValueAsString(authorizationConsent);
            // 存储1小时
            redisTemplate.opsForValue().set(key, consentData, 1, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OAuth2AuthorizationConsent", e);
        }
    }

    @Override
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
        String key = buildConsentKey(authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
        redisTemplate.delete(key);
    }

    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        Assert.hasText(registeredClientId, "registeredClientId cannot be empty");
        Assert.hasText(principalName, "principalName cannot be empty");
        String key = buildConsentKey(registeredClientId, principalName);
        String consentData = (String) redisTemplate.opsForValue().get(key);
        if (consentData != null) {
            try {
                return objectMapper.readValue(consentData, new TypeReference<OAuth2AuthorizationConsent>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize OAuth2AuthorizationConsent", e);
            }
        }
        return null;
    }

    /**
     * 构建授权同意信息的Redis键
     *
     * @param registeredClientId 注册客户端ID
     * @param principalName 主体名称
     * @return Redis键
     */
    private String buildConsentKey(String registeredClientId, String principalName) {
        return "oauth2:consent:" + registeredClientId + ":" + principalName;
    }
}