package com.cloud.search.config;

import com.cloud.common.config.RedisConfigFactory;
import com.cloud.common.config.base.BaseRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 搜索服务Redis配置
 * 支持多级缓存架构，优化搜索相关数据的存储
 * 使用缓存专用配置，不需要事务支持
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class SearchRedisConfig extends BaseRedisConfig {

    /**
     * 搜索服务专用的RedisTemplate配置
     * 使用缓存专用配置，优化搜索结果缓存性能
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        System.out.println("初始化搜索服务Redis配置");
        return RedisConfigFactory.createCacheRedisTemplate(redisConnectionFactory);
    }

    @Override
    protected String getServicePrefix() {
        return "search";
    }

    /**
     * 搜索服务缓存过期时间配置
     * 搜索结果变化较快，使用较短的缓存时间
     */
    @Override
    protected long getCacheExpireTime(String type) {
        switch (type) {
            case "productSearch":
                return 1800L; // 30分钟（商品搜索结果）
            case "searchSuggestion":
                return 3600L; // 1小时（搜索建议）
            case "hotSearch":
                return 7200L; // 2小时（热门搜索）
            case "searchStats":
                return 600L;  // 10分钟（搜索统计）
            case "filter":
                return 2700L; // 45分钟（搜索过滤器）
            case "aggregation":
                return 900L;  // 15分钟（聚合查询结果）
            case "searchHistory":
                return 86400L; // 24小时（搜索历史）
            default:
                return 1800L; // 默认30分钟
        }
    }

    /**
     * 搜索服务不需要事务支持（纯缓存场景）
     */
    @Override
    protected boolean shouldEnableTransactionSupport() {
        return false;
    }
}
