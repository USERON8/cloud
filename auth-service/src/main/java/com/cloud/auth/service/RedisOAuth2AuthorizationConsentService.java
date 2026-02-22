package com.cloud.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.util.Assert;

import java.time.Duration;
















@Slf4j
@RequiredArgsConstructor
public class RedisOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private static final String CONSENT_KEY_PREFIX = "oauth2:consent:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(30); 

    private final RedisTemplate<String, Object> redisTemplate;

    




    @Override
    public void save(@NonNull OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
        Assert.hasText(authorizationConsent.getRegisteredClientId(), "registeredClientId cannot be empty");
        Assert.hasText(authorizationConsent.getPrincipalName(), "principalName cannot be empty");

        String key = buildConsentKey(
                authorizationConsent.getRegisteredClientId(),
                authorizationConsent.getPrincipalName()
        );

        try {
            
            redisTemplate.opsForValue().set(key, authorizationConsent, DEFAULT_TTL);

            log.debug("OAuth2 authorization consent saved: clientId={}, principalName={}, authorities={}",
                    authorizationConsent.getRegisteredClientId(),
                    authorizationConsent.getPrincipalName(),
                    authorizationConsent.getAuthorities());

        } catch (Exception e) {
            log.error("Failed to save OAuth2 authorization consent to Redis: clientId={}, principalName={}",
                    authorizationConsent.getRegisteredClientId(),
                    authorizationConsent.getPrincipalName(), e);
            throw new RuntimeException("Failed to save OAuth2 authorization consent", e);
        }
    }

    




    @Override
    public void remove(@NonNull OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
        Assert.hasText(authorizationConsent.getRegisteredClientId(), "registeredClientId cannot be empty");
        Assert.hasText(authorizationConsent.getPrincipalName(), "principalName cannot be empty");

        String key = buildConsentKey(
                authorizationConsent.getRegisteredClientId(),
                authorizationConsent.getPrincipalName()
        );

        try {
            Boolean deleted = redisTemplate.delete(key);

            if (deleted) {
                log.debug("OAuth2 authorization consent removed: clientId={}, principalName={}",
                        authorizationConsent.getRegisteredClientId(),
                        authorizationConsent.getPrincipalName());
            } else {
                log.warn("OAuth2 authorization consent not found for removal: clientId={}, principalName={}",
                        authorizationConsent.getRegisteredClientId(),
                        authorizationConsent.getPrincipalName());
            }

        } catch (Exception e) {
            log.error("Failed to remove OAuth2 authorization consent from Redis: clientId={}, principalName={}",
                    authorizationConsent.getRegisteredClientId(),
                    authorizationConsent.getPrincipalName(), e);
            throw new RuntimeException("Failed to remove OAuth2 authorization consent", e);
        }
    }

    






    @Override
    @Nullable
    public OAuth2AuthorizationConsent findById(@NonNull String registeredClientId, @NonNull String principalName) {
        Assert.hasText(registeredClientId, "registeredClientId cannot be empty");
        Assert.hasText(principalName, "principalName cannot be empty");

        String key = buildConsentKey(registeredClientId, principalName);

        try {
            Object consent = redisTemplate.opsForValue().get(key);

            if (consent instanceof OAuth2AuthorizationConsent authorizationConsent) {
                log.debug("OAuth2 authorization consent found: clientId={}, principalName={}, authorities={}",
                        registeredClientId, principalName, authorizationConsent.getAuthorities());
                return authorizationConsent;
            }

            log.debug("OAuth2 authorization consent not found: clientId={}, principalName={}",
                    registeredClientId, principalName);
            return null;

        } catch (Exception e) {
            log.error("Failed to find OAuth2 authorization consent from Redis: clientId={}, principalName={}",
                    registeredClientId, principalName, e);
            throw new RuntimeException("Failed to find OAuth2 authorization consent", e);
        }
    }

    






    private String buildConsentKey(String registeredClientId, String principalName) {
        return CONSENT_KEY_PREFIX + registeredClientId + ":" + principalName;
    }

    





    public int cleanupExpiredConsents() {
        try {
            
            
            return 0;
        } catch (Exception e) {
            log.error("Failed to cleanup expired OAuth2 authorization consents", e);
            return 0;
        }
    }

    




    public boolean isHealthy() {
        try {
            
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                connection.ping();
                return null;
            });
            return true;
        } catch (Exception e) {
            log.error("Redis OAuth2 authorization consent service health check failed", e);
            return false;
        }
    }

    




    public long getConsentCount() {
        try {
            Long count = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Long>) connection -> {
                try {
                    java.util.Set<byte[]> keys = connection.keys((CONSENT_KEY_PREFIX + "*").getBytes());
                    return keys != null ? (long) keys.size() : 0L;
                } catch (Exception e) {
                    log.warn("Failed to count consent keys", e);
                    return 0L;
                }
            });
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Failed to get consent count from Redis", e);
            return -1;
        }
    }
}
