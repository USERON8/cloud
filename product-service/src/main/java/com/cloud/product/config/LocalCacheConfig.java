package com.cloud.product.config;

import com.cloud.common.config.BaseLocalCacheConfig;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
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
@Configuration
@EnableAspectJAutoProxy // 启用AOP支持，用于多级缓存注解处理
public class LocalCacheConfig extends BaseLocalCacheConfig {

    /**
     * 重写父类的本地缓存管理器（L1缓存）
     * 用于多级缓存架构中的第一级缓存
     * 针对商品服务进行优化配置
     *
     * @return CacheManager 本地缓存管理器
     */
    @Override
    @Primary
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 使用父类提供的工具方法创建Caffeine配置
        // 针对商品服务的缓存特点：高频访问，商品信息相对稳定
        cacheManager.setCaffeine(buildCaffeineSpec(
                150,    // 初始容量
                1500L,  // 最大缓存条目数
                45L,    // 写入后45分钟过期
                20L     // 访问后20分钟过期
        ));

        // 预定义缓存名称，提高性能
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "productCache",         // 商品基础信息缓存
                "productListCache",     // 商品列表查询缓存
                "productStatsCache",    // 商品统计信息缓存
                "shopCache",            // 店铺信息缓存
                "shopListCache",        // 店铺列表查询缓存
                "categoryCache",        // 商品分类缓存
                "categoryTreeCache"     // 分类树形结构缓存
        ));

        return cacheManager;
    }
}
