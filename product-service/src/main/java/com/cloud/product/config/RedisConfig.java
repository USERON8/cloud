package com.cloud.product.config;

import com.cloud.common.config.RedisConfigFactory;
import com.cloud.common.config.base.BaseRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 商品服务Redis配置
 * 支持多级缓存架构，优化商品相关数据的存储
 * 使用缓存专用配置，不需要事务支持
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class RedisConfig extends BaseRedisConfig {

    /**
     * 商品服务专用的RedisTemplate配置
     * 使用缓存专用配置，优化查询性能
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化商品服务Redis配置");
        return RedisConfigFactory.createCacheRedisTemplate(redisConnectionFactory);
    }

    @Override
    protected String getServicePrefix() {
        return "product";
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
            case "productSearch":
                return 900L;  // 15分钟（搜索结果）
            case "productRecommend":
                return 1200L; // 20分钟（推荐结果）
            default:
                return 3600L; // 默认1小时
        }
    }

    /**
     * 商品服务不需要事务支持（纯缓存场景）
     */
    @Override
    protected boolean shouldEnableTransactionSupport() {
        return false;
    }
}
