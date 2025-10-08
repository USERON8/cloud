package com.cloud.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础Redis配置类
 * <p>
 * 提供所有服务共享的Redis基础能力：
 * 1. RedisTemplate配置（优化的序列化策略）
 * 2. 基于Redis的单级缓存管理器
 * 3. 统一的序列化配置（String类型存储JSON）
 * <p>
 * 设计原则：
 * - 始终生效，为所有服务提供Redis单缓存能力
 * - 可与多级缓存配置组合，支持灵活的缓存策略
 * - 通过@ConditionalOnMissingBean避免与复合缓存管理器冲突
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnClass({RedisConnectionFactory.class, RedisTemplate.class})
public class RedisConfig {

    /**
     * 统一的JSON序列化器
     * 使用优化的Jackson配置，确保序列化性能和兼容性
     */
    @Bean
    @ConditionalOnMissingBean
    public GenericJackson2JsonRedisSerializer jsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 序列化配置优化
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // 处理时区和日期格式
        objectMapper.findAndRegisterModules();

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /**
     * 主要的RedisTemplate配置
     * 统一使用String key + JSON value的序列化策略
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer jsonSerializer) {

        log.info("🔧 初始化统一RedisTemplate配置");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 统一序列化配置
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Key和HashKey使用String序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value和HashValue使用JSON序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);

        // 启用事务支持（用于复杂业务场景）
        template.setEnableTransactionSupport(false);

        template.afterPropertiesSet();

        log.info("✅ RedisTemplate配置完成");
        return template;
    }

    /**
     * StringRedisTemplate配置
     * 用于纯字符串操作，性能更优
     */
    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("🔧 初始化StringRedisTemplate配置");

        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();

        log.info("✅ StringRedisTemplate配置完成");
        return template;
    }

    /**
     * 基于Redis的缓存管理器
     * 支持不同业务场景的缓存策略
     * 使用@ConditionalOnMissingBean，允许被其他缓存管理器覆盖
     */
    @Bean("redisCacheManager")
    @ConditionalOnMissingBean(CacheManager.class) // 避免与其他缓存管理器冲突
    public CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer jsonSerializer) {

        log.info("🔧 初始化Redis缓存管理器");

        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))  // 默认1小时过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues()  // 不缓存null值，防止缓存穿透
                .computePrefixWith(cacheName -> "cache:" + cacheName + ":");  // 统一缓存前缀

        // 特定业务场景的缓存配置
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();

        // 用户相关缓存 - 30分钟过期
        configMap.put("user", createCacheConfig(jsonSerializer, Duration.ofMinutes(30)));
        configMap.put("userInfo", createCacheConfig(jsonSerializer, Duration.ofMinutes(30)));
        configMap.put("userProfile", createCacheConfig(jsonSerializer, Duration.ofMinutes(20)));

        // 商品相关缓存 - 45分钟过期
        configMap.put("product", createCacheConfig(jsonSerializer, Duration.ofMinutes(45)));
        configMap.put("productInfo", createCacheConfig(jsonSerializer, Duration.ofMinutes(45)));
        configMap.put("productList", createCacheConfig(jsonSerializer, Duration.ofMinutes(30)));

        // 订单相关缓存 - 15分钟过期（变化频繁）
        configMap.put("order", createCacheConfig(jsonSerializer, Duration.ofMinutes(15)));
        configMap.put("orderInfo", createCacheConfig(jsonSerializer, Duration.ofMinutes(15)));

        // 库存相关缓存 - 5分钟过期（高频变化）
        configMap.put("stock", createCacheConfig(jsonSerializer, Duration.ofMinutes(5)));
        configMap.put("stockInfo", createCacheConfig(jsonSerializer, Duration.ofMinutes(5)));

        // 支付相关缓存 - 10分钟过期
        configMap.put("payment", createCacheConfig(jsonSerializer, Duration.ofMinutes(10)));
        configMap.put("paymentInfo", createCacheConfig(jsonSerializer, Duration.ofMinutes(10)));

        // 搜索相关缓存 - 20分钟过期
        configMap.put("search", createCacheConfig(jsonSerializer, Duration.ofMinutes(20)));
        configMap.put("searchResult", createCacheConfig(jsonSerializer, Duration.ofMinutes(15)));

        // 权限相关缓存 - 1小时过期
        configMap.put("auth", createCacheConfig(jsonSerializer, Duration.ofHours(1)));
        configMap.put("permission", createCacheConfig(jsonSerializer, Duration.ofHours(1)));

        // 配置统计信息缓存 - 10分钟过期
        configMap.put("stats", createCacheConfig(jsonSerializer, Duration.ofMinutes(10)));
        configMap.put("metrics", createCacheConfig(jsonSerializer, Duration.ofMinutes(5)));

        // 热点数据缓存 - 2小时过期，添加随机过期时间防雪崩
        configMap.put("hotspot", createCacheConfig(jsonSerializer, Duration.ofHours(2).plusMinutes((long) (Math.random() * 30))));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .transactionAware()  // 支持事务
                .build();

        log.info("✅ Redis缓存管理器配置完成，支持{}个特定缓存配置", configMap.size());
        return cacheManager;
    }

    /**
     * 创建指定过期时间的缓存配置
     */
    private RedisCacheConfiguration createCacheConfig(GenericJackson2JsonRedisSerializer jsonSerializer, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();
    }
}
