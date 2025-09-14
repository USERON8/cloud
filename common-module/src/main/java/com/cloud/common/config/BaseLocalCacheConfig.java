package com.cloud.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置基类
 * 提供基于Caffeine的本地缓存配置模板
 * 用于实现多级缓存中的L1缓存层
 * 各服务可继承此类并根据需要重写缓存配置
 */
@Configuration
public abstract class BaseLocalCacheConfig {

    /**
     * 创建默认的Caffeine缓存管理器
     * 子类必须实现此方法来提供服务特定的缓存配置
     *
     * @return CacheManager 缓存管理器
     */
    @Bean
    public abstract CacheManager localCacheManager();

    /**
     * 为子类提供的工具方法：创建默认的Caffeine配置
     *
     * @param initialCapacity   初始容量
     * @param maximumSize       最大容量
     * @param expireAfterWrite  写入后过期时间
     * @param expireAfterAccess 访问后过期时间
     * @param timeUnit          时间单位
     * @return Caffeine 配置对象
     */
    protected Caffeine<Object, Object> buildCaffeineSpec(
            int initialCapacity, long maximumSize,
            long expireAfterWrite, long expireAfterAccess,
            TimeUnit timeUnit) {
        return Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWrite, timeUnit)
                .expireAfterAccess(expireAfterAccess, timeUnit)
                .recordStats();
    }
}