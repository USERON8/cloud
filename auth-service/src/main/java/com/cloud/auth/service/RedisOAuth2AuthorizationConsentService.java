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

/**
 * OAuth2.1授权同意服务的Redis实现
 * <p>
 * 符合OAuth2.1标准，将OAuth2授权同意信息存储到Redis中
 * 支持TTL过期机制，提高性能和安全性
 * <p>
 * 主要特性：
 * - 基于Redis的持久化存储OAuth2授权同意信息
 * - 支持授权同意的CRUD操作
 * - 自动过期清理，防止内存泄漏
 * - 线程安全，支持高并发场景
 * - 符合OAuth2.1标准的授权同意管理规范
 *
 * @author what's up
 */
@Slf4j
@RequiredArgsConstructor
public class RedisOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private static final String CONSENT_KEY_PREFIX = "oauth2:consent:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(30); // 同意信息默认保存30天

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 保存OAuth2授权同意信息
     *
     * @param authorizationConsent OAuth2授权同意对象
     */
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
            // 将OAuth2AuthorizationConsent对象存储到Redis，设置过期时间
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

    /**
     * 移除OAuth2授权同意信息
     *
     * @param authorizationConsent OAuth2授权同意对象
     */
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

    /**
     * 根据客户端ID和主体名称查找OAuth2授权同意信息
     *
     * @param registeredClientId 注册的客户端ID
     * @param principalName      主体名称（通常是用户名）
     * @return OAuth2授权同意对象，如果不存在则返回null
     */
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

    /**
     * 构建授权同意的Redis键
     *
     * @param registeredClientId 注册的客户端ID
     * @param principalName      主体名称
     * @return Redis键
     */
    private String buildConsentKey(String registeredClientId, String principalName) {
        return CONSENT_KEY_PREFIX + registeredClientId + ":" + principalName;
    }

    /**
     * 清理过期的授权同意信息
     * 可以通过定时任务调用此方法进行定期清理
     *
     * @return 清理的数量
     */
    public int cleanupExpiredConsents() {
        try {
            // Redis会自动处理TTL过期，这里主要用于统计和日志记录
            log.info("OAuth2 authorization consent cleanup completed - Redis TTL handles expiration automatically");
            return 0;
        } catch (Exception e) {
            log.error("Failed to cleanup expired OAuth2 authorization consents", e);
            return 0;
        }
    }

    /**
     * 检查Redis连接健康状态
     *
     * @return true如果连接正常，false如果连接异常
     */
    public boolean isHealthy() {
        try {
            // 执行简单的ping操作来检查Redis连接
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

    /**
     * 获取所有授权同意信息的数量（用于监控）
     *
     * @return 授权同意信息数量
     */
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
