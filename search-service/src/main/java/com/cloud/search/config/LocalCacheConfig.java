package com.cloud.search.config;

import com.cloud.common.config.BaseLocalCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 搜索服务本地缓存配置类
 * 提供基于Caffeine的本地缓存配置，用于实现多级缓存中的L1缓存层
 * L1: 本地缓存 (Caffeine) + L2: 分布式缓存 (Redis)
 * <p>
 * 支持传统的Spring Cache注解(@Cacheable等)和自定义的多级缓存注解(@MultiLevelCacheable等)
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy // 启用AOP支持，用于多级缓存注解处理
public class LocalCacheConfig extends BaseLocalCacheConfig {

    /**
     * 重写父类的本地缓存管理器（L1缓存）
     * 用于多级缓存架构中的第一级缓存
     * 针对搜索服务进行优化配置
     *
     * @return CacheManager 本地缓存管理器
     */
    @Override
    @Primary
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 使用父类提供的工具方法创建Caffeine配置
        // 针对搜索服务的缓存特点：频繁访问，但结果可能变化较快
        cacheManager.setCaffeine(buildSearchCache());

        // 预定义缓存名称，提高性能
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "searchCache",          // 搜索结果缓存
                "productSearchCache",   // 商品搜索缓存
                "orderSearchCache",     // 订单搜索缓存
                "suggestionCache",      // 搜索建议缓存
                "aggregationCache",     // 聚合统计缓存
                "filterCache",          // 筛选条件缓存
                "hotKeywordCache"       // 热门关键词缓存
        ));

        log.info("搜索服务本地缓存管理器初始化完成");
        return cacheManager;
    }

    /**
     * 创建搜索专用缓存配置
     * 适用于搜索结果等数据
     */
    private com.github.benmanes.caffeine.cache.Caffeine<Object, Object> buildSearchCache() {
        return buildCaffeineSpec(
                150,    // 适中的初始容量，搜索关键词有限
                1500L,  // 适中的最大容量，支持多种搜索条件
                20L,    // 适中的写入过期时间（20分钟）
                10L,    // 较短的访问过期时间（10分钟），搜索结果变化频繁
                TimeUnit.MINUTES
        );
    }
}
