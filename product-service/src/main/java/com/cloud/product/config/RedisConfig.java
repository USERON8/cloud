package com.cloud.product.config;

import com.cloud.common.config.base.EnhancedRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 商品服务Redis配置
 * 支持多级缓存架构，优化商品相关数据的存储
 * 使用Hash类型存储商品高频访问字段
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class RedisConfig extends EnhancedRedisConfig {

    @Override
    protected String getCacheKeyPrefix() {
        return "product";
    }

    /**
     * 商品服务Redis模板配置
     */
    @Bean
    public RedisTemplate<String, Object> productRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return createRedisTemplate(redisConnectionFactory);
    }

    /**
     * 字符串Redis模板，用于简单缓存操作
     */
    @Bean
    public StringRedisTemplate productStringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return createStringRedisTemplate(redisConnectionFactory);
    }

    /**
     * Hash操作接口，用于存储商品详细信息
     */
    @Bean
    public HashOperations<String, String, Object> productHashOperations(
            RedisTemplate<String, Object> productRedisTemplate) {
        return getHashOperations(productRedisTemplate);
    }

    /**
     * 商品服务缓存过期时间配置
     */
    @Override
    protected long getCacheExpireTime(String type) {
        switch (type) {
            case "productInfo":
                return 2700L; // 45分钟
            case "productList":
                return 1800L; // 30分钟  
            case "shopInfo":
                return 3600L; // 1小时
            case "categoryInfo":
                return 7200L; // 2小时（分类变动较少）
            case "productStats":
                return 600L;  // 10分钟（统计信息更新频繁）
            default:
                return 3600L; // 默认1小时
        }
    }
}
