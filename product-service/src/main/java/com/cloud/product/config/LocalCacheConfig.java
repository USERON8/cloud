package com.cloud.product.config;

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
 * 商品服务本地缓存配置类
 * 提供基于Caffeine的本地缓存配置，用于实现多级缓存中的L1缓存层
 * L1: 本地缓存 (Caffeine) + L2: 分布式缓存 (Redis)
 * <p>
 * 商品服务特点：高频查询，商品信息相对稳定
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableCaching
@EnableAspectJAutoProxy // 启用AOP支持，用于多级缓存注解处理
public class LocalCacheConfig extends BaseLocalCacheConfig {

    /**
     * 商品服务专用的本地缓存管理器（L1缓存）
     * 用于多级缓存架构中的第一级缓存
     * 针对商品服务进行优化配置
     *
     * @return CacheManager 本地缓存管理器
     */
    @Bean
    @Primary
    public CacheManager localCacheManager() {
        log.info("初始化商品服务本地缓存管理器");
        return MultiLevelCacheConfigFactory.createProductServiceCacheManager();
    }

    @Override
    protected String getServiceName() {
        return "product-service";
    }

    @Override
    protected String[] getCacheNames() {
        return new String[]{
                "productCache",         // 商品基础信息缓存
                "productListCache",     // 商品列表查询缓存
                "productStatsCache",    // 商品统计信息缓存
                "shopCache",            // 店铺信息缓存
                "shopListCache",        // 店铺列表查询缓存
                "categoryCache",        // 商品分类缓存
                "categoryTreeCache"     // 分类树形结构缓存
        };
    }
}
