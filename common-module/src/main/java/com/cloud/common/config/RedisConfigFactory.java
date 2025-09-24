package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置工厂类
 * 提供各种预定义的Redis配置模板，子服务可以根据需要选择使用
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
public class RedisConfigFactory {

    /**
     * 创建基础RedisTemplate配置
     * 适用于一般的缓存场景
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate
     */
    public static RedisTemplate<String, Object> createBasicRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("创建基础RedisTemplate配置");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 基础序列化配置
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        template.setDefaultSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建高性能RedisTemplate配置
     * 适用于高并发场景，优化序列化性能
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate
     */
    public static RedisTemplate<String, Object> createHighPerformanceRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("创建高性能RedisTemplate配置");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 高性能序列化配置
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);

        // 启用事务支持（高并发场景可能需要）
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建缓存专用RedisTemplate配置
     * 适用于纯缓存场景，不需要事务支持
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate
     */
    public static RedisTemplate<String, Object> createCacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("创建缓存专用RedisTemplate配置");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 缓存优化序列化配置
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        template.setDefaultSerializer(RedisSerializer.json());

        // 缓存场景不需要事务支持
        template.setEnableTransactionSupport(false);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建会话专用RedisTemplate配置
     * 适用于会话存储场景
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate
     */
    public static RedisTemplate<String, Object> createSessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("创建会话专用RedisTemplate配置");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 会话序列化配置
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        template.setDefaultSerializer(RedisSerializer.json());

        // 会话场景需要事务支持
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建消息队列专用RedisTemplate配置
     * 适用于Redis作为消息队列的场景
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate
     */
    public static RedisTemplate<String, Object> createMessageRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("创建消息队列专用RedisTemplate配置");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 消息队列序列化配置
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        template.setDefaultSerializer(RedisSerializer.json());

        // 消息队列需要事务支持
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建自定义RedisTemplate配置
     *
     * @param builder 配置构建器
     * @return RedisTemplate
     */
    public static RedisTemplate<String, Object> createCustomRedisTemplate(RedisTemplateBuilder builder) {
        log.info("创建自定义RedisTemplate配置");
        return builder.build();
    }

    /**
     * 创建StringRedisTemplate
     *
     * @param connectionFactory Redis连接工厂
     * @return StringRedisTemplate
     */
    public static StringRedisTemplate createStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("创建StringRedisTemplate配置");
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate配置构建器
     */
    public static class RedisTemplateBuilder {
        private RedisConnectionFactory connectionFactory;
        private RedisSerializer<?> keySerializer = new StringRedisSerializer();
        private RedisSerializer<?> hashKeySerializer = new StringRedisSerializer();
        private RedisSerializer<?> valueSerializer = RedisSerializer.json();
        private RedisSerializer<?> hashValueSerializer = RedisSerializer.json();
        private RedisSerializer<?> defaultSerializer = RedisSerializer.json();
        private boolean enableTransactionSupport = false;

        public RedisTemplateBuilder connectionFactory(RedisConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        public RedisTemplateBuilder keySerializer(RedisSerializer<?> keySerializer) {
            this.keySerializer = keySerializer;
            return this;
        }

        public RedisTemplateBuilder hashKeySerializer(RedisSerializer<?> hashKeySerializer) {
            this.hashKeySerializer = hashKeySerializer;
            return this;
        }

        public RedisTemplateBuilder valueSerializer(RedisSerializer<?> valueSerializer) {
            this.valueSerializer = valueSerializer;
            return this;
        }

        public RedisTemplateBuilder hashValueSerializer(RedisSerializer<?> hashValueSerializer) {
            this.hashValueSerializer = hashValueSerializer;
            return this;
        }

        public RedisTemplateBuilder defaultSerializer(RedisSerializer<?> defaultSerializer) {
            this.defaultSerializer = defaultSerializer;
            return this;
        }

        public RedisTemplateBuilder enableTransactionSupport(boolean enableTransactionSupport) {
            this.enableTransactionSupport = enableTransactionSupport;
            return this;
        }

        public RedisTemplate<String, Object> build() {
            if (connectionFactory == null) {
                throw new IllegalArgumentException("RedisConnectionFactory cannot be null");
            }

            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(keySerializer);
            template.setHashKeySerializer(hashKeySerializer);
            template.setValueSerializer(valueSerializer);
            template.setHashValueSerializer(hashValueSerializer);
            template.setDefaultSerializer(defaultSerializer);
            template.setEnableTransactionSupport(enableTransactionSupport);
            template.afterPropertiesSet();

            return template;
        }
    }
}
