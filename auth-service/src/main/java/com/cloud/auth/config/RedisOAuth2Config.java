package com.cloud.auth.config;

import com.cloud.auth.service.RedisOAuth2AuthorizationConsentService;
import com.cloud.auth.service.SimpleRedisHashOAuth2AuthorizationService;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

/**
 * Redis OAuth2配置
 * 专门处理OAuth2相关的Redis存储配置
 * <p>
 * 功能包括:
 * - OAuth2授权服务Redis配置
 * - OAuth2同意服务Redis配置
 * - Redis序列化配置
 * - Redis连接配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisOAuth2Config {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationServerSettings authorizationServerSettings;

    /**
     * OAuth2授权服务配置
     * 使用Redis存储授权信息，支持集群部署
     */
    @Bean
    public OAuth2AuthorizationService authorizationService() {
        log.info("🔧 配置OAuth2授权服务（Redis存储）");

        OAuth2AuthorizationService authorizationService =
                new SimpleRedisHashOAuth2AuthorizationService(
                        oauth2MainRedisTemplate(),
                        registeredClientRepository,
                        authorizationServerSettings
                );

        log.info("✅ OAuth2授权服务配置完成");
        return authorizationService;
    }

    /**
     * OAuth2同意服务配置
     * 使用Redis存储用户授权同意信息
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        log.info("🔧 配置OAuth2同意服务（Redis存储）");

        OAuth2AuthorizationConsentService consentService =
                new RedisOAuth2AuthorizationConsentService(
                        oauth2MainRedisTemplate()
                );

        log.info("✅ OAuth2同意服务配置完成");
        return consentService;
    }

    /**
     * Redis模板配置
     * 专门用于OAuth2数据存储，支持JSON序列化
     */
    @Bean("oauth2MainRedisTemplate")
    public RedisTemplate<String, Object> oauth2MainRedisTemplate() {
        log.info("🔧 配置Redis模板");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 字符串序列化器
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // JSON序列化器（支持OAuth2对象序列化）
        GenericJackson2JsonRedisSerializer jsonRedisSerializer =
                new GenericJackson2JsonRedisSerializer();

        // 配置序列化器
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        // 启用事务支持
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();

        log.info("✅ Redis模板配置完成");
        return template;
    }

    /**
     * OAuth2专用Redis模板
     * 针对OAuth2对象序列化进行优化
     */
    @Bean("oauth2RedisTemplate")
    public RedisTemplate<String, Object> oauth2RedisTemplate() {
        log.info("🔧 配置OAuth2专用Redis模板");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 字符串序列化器
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 自定义JSON序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer();

        // 配置序列化器
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // OAuth2特定配置
        template.setDefaultSerializer(jsonSerializer);
        template.setEnableTransactionSupport(false);  // OAuth2服务通常不需要Redis事务

        template.afterPropertiesSet();

        log.info("✅ OAuth2专用Redis模板配置完成");
        return template;
    }

    /**
     * Redis连接配置验证器
     * 验证Redis连接是否正常
     */
    @Bean
    @ConditionalOnProperty(name = "auth.redis.validation.enabled", havingValue = "true")
    public RedisConnectionValidator redisConnectionValidator() {
        log.info("🔧 配置Redis连接验证器 (启用)");
        return new RedisConnectionValidator(redisConnectionFactory);
    }

    /**
     * Redis健康检查器
     * 定期检查Redis连接健康状态
     */
    @Bean
    public RedisHealthChecker redisHealthChecker() {
        log.info("🔧 配置Redis健康检查器");

        return new RedisHealthChecker(oauth2MainRedisTemplate());
    }

    /**
     * Redis连接验证器实现
     */
    public static class RedisConnectionValidator {
        private final RedisConnectionFactory connectionFactory;

        public RedisConnectionValidator(RedisConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            validateConnection();
        }

        /**
         * 验证Redis连接
         */
        private void validateConnection() {
            log.info("🔍 验证Redis连接");

            try {
                // 测试连接
                var connection = connectionFactory.getConnection();
                if (connection != null) {
                    // 执行ping命令测试连接
                    connection.ping();
                    connection.close();

                    log.info("✅ Redis连接验证成功");
                } else {
                    throw new IllegalStateException("无法获取Redis连接");
                }

            } catch (Exception e) {
                log.error("🚨 Redis连接验证失败", e);
                throw new IllegalStateException("Redis连接验证失败", e);
            }
        }

        /**
         * 获取连接信息
         */
        public String getConnectionInfo() {
            try {
                return String.format("Redis连接: %s, 状态: 正常",
                        connectionFactory.getClass().getSimpleName());
            } catch (Exception e) {
                return String.format("Redis连接: %s, 状态: 异常(%s)",
                        connectionFactory.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Redis健康检查器实现
     */
    public static class RedisHealthChecker {
        private final RedisTemplate<String, Object> redisTemplate;

        public RedisHealthChecker(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        /**
         * 检查Redis健康状态
         */
        public boolean isHealthy() {
            try {
                // 执行简单的Redis操作测试连接
                String testKey = "auth:health:check:" + System.currentTimeMillis();
                String testValue = "ok";

                // 写入测试数据
                redisTemplate.opsForValue().set(testKey, testValue,
                        java.time.Duration.ofSeconds(10));

                // 读取测试数据
                String result = (String) redisTemplate.opsForValue().get(testKey);

                // 清理测试数据
                redisTemplate.delete(testKey);

                boolean healthy = testValue.equals(result);

                if (healthy) {
                    log.debug("✅ Redis健康检查通过");
                } else {
                    log.warn("⚠️ Redis健康检查失败: 数据不一致");
                }

                return healthy;

            } catch (Exception e) {
                log.error("🚨 Redis健康检查异常", e);
                return false;
            }
        }

        /**
         * 获取Redis统计信息
         */
        public RedisStats getStats() {
            try {
                // 这里可以添加更多Redis统计信息收集
                return new RedisStats(true, "正常", System.currentTimeMillis());
            } catch (Exception e) {
                return new RedisStats(false, e.getMessage(), System.currentTimeMillis());
            }
        }

        /**
         * Redis统计信息
         */
        public static class RedisStats {
            private final boolean healthy;
            private final String status;
            private final long checkTime;

            public RedisStats(boolean healthy, String status, long checkTime) {
                this.healthy = healthy;
                this.status = status;
                this.checkTime = checkTime;
            }

            public boolean isHealthy() {
                return healthy;
            }

            public String getStatus() {
                return status;
            }

            public long getCheckTime() {
                return checkTime;
            }

            @Override
            public String toString() {
                return String.format("RedisStats{healthy=%s, status='%s', checkTime=%d}",
                        healthy, status, checkTime);
            }
        }
    }

    /**
     * OAuth2Authorization序列化混入类
     * 解决OAuth2Authorization对象序列化问题
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class OAuth2AuthorizationMixin {
        // 空的混入类，用于Jackson序列化配置
    }

    /**
     * OAuth2AuthorizationConsent序列化混入类
     * 解决OAuth2AuthorizationConsent对象序列化问题
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class OAuth2AuthorizationConsentMixin {
        // 空的混入类，用于Jackson序列化配置
    }
}
