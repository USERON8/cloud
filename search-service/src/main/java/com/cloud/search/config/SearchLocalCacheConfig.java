package com.cloud.search.config;

import com.cloud.common.config.BaseLocalCacheConfig;
import com.cloud.common.config.MultiLevelCacheConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

/**
 * 搜索服务本地缓存配置类
 * 提供基于Caffeine的本地缓存配置，用于实现多级缓存中的L1缓存层
 * L1: 本地缓存 (Caffeine) + L2: 分布式缓存 (Redis)
 * <p>
 * 搜索服务特点：搜索结果变化较快，但热门搜索相对稳定
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableCaching
@EnableAspectJAutoProxy // 启用AOP支持，用于多级缓存注解处理
public class SearchLocalCacheConfig extends BaseLocalCacheConfig {

    /**
     * 搜索服务专用的本地缓存管理器（L1缓存）
     * 用于多级缓存架构中的第一级缓存
     * 针对搜索服务进行优化配置
     *
     * @return CacheManager 本地缓存管理器
     */
    @Bean
    @Primary
    public CacheManager localCacheManager() {
        log.info("初始化搜索服务本地缓存管理器");
        return MultiLevelCacheConfigFactory.createSearchServiceCacheManager();
    }

    @Override
    protected String getServiceName() {
        return "search-service";
    }

    @Override
    protected String[] getCacheNames() {
        return new String[]{
                "productSearchCache",   // 商品搜索结果缓存
                "searchSuggestionCache", // 搜索建议缓存
                "hotSearchCache",       // 热门搜索缓存
                "searchStatsCache",     // 搜索统计缓存
                "filterCache",          // 搜索过滤器缓存
                "aggregationCache"      // 聚合查询结果缓存
        };
    }
}
