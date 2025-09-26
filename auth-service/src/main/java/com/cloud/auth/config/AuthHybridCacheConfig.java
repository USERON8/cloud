package com.cloud.auth.config;

import com.cloud.common.cache.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 认证服务混合缓存配置
 * 为认证服务提供专用的缓存组件配置
 *
 * @author what's up
 * @since 1.0.0
 */
@Configuration
public class AuthHybridCacheConfig {

    /**
     * 缓存数据分析器
     * 用于分析和监控缓存的使用情况
     */
    @Bean
    public CacheDataAnalyzer cacheDataAnalyzer() {
        return new CacheDataAnalyzer();
    }

    /**
     * 缓存性能指标收集器
     * 用于收集缓存的性能指标和统计信息
     */
    @Bean
    public CachePerformanceMetrics cachePerformanceMetrics() {
        return new CachePerformanceMetrics();
    }

    /**
     * 混合缓存管理器
     * 提供本地缓存和分布式缓存的统一管理
     *
     * @param redisTemplate Redis模板
     * @param analyzer 缓存数据分析器
     * @param metrics 缓存性能指标收集器
     * @return HybridCacheManager实例
     */
    @Bean
    public HybridCacheManager hybridCacheManager(org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate,
                                                CacheDataAnalyzer analyzer, 
                                                CachePerformanceMetrics metrics) {
        return new HybridCacheManager(redisTemplate, analyzer, metrics);
    }
}
