package com.cloud.user.config;

import com.cloud.common.cache.CacheDataAnalyzer;
import com.cloud.common.cache.CachePerformanceMetrics;
import com.cloud.common.cache.HybridCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * User 服务混合缓存配置
 * 明确装配混合缓存所需的 Bean，避免包扫描范围导致的遗漏。
 */
@Slf4j
@Configuration
public class UserHybridCacheConfig {

    @Bean
    public CacheDataAnalyzer cacheDataAnalyzer() {
        return new CacheDataAnalyzer();
    }

    @Bean
    public CachePerformanceMetrics cachePerformanceMetrics() {
        return new CachePerformanceMetrics();
    }

    @Bean
    public HybridCacheManager hybridCacheManager(RedisTemplate<String, Object> redisTemplate,
                                                 CacheDataAnalyzer analyzer,
                                                 CachePerformanceMetrics metrics) {
        log.info("✅ 初始化 HybridCacheManager (user-service)");
        return new HybridCacheManager(redisTemplate, analyzer, metrics);
    }
}

