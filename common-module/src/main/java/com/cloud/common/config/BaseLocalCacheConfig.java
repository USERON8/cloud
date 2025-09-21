package com.cloud.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 本地缓存基础配置类
 *
 * @author cloud
 * @date 2024-01-20
 */
@Configuration
@EnableCaching
@ConditionalOnClass(Caffeine.class)
public class BaseLocalCacheConfig {

    /**
     * 缓存管理器
     *
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * 配置缓存构建器
     *
     * @return Caffeine构建器
     */
    protected Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                // 设置最后一次写入或访问后经过固定时间过期
                .expireAfterWrite(Duration.ofMinutes(30))
                // 初始的缓存空间大小
                .initialCapacity(100)
                // 缓存的最大条数
                .maximumSize(1000)
                // 开启缓存统计
                .recordStats();
    }

    /**
     * 用户信息缓存
     *
     * @return Cache
     */
    @Bean
    public Cache<String, Object> userInfoCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(500)
                .recordStats()
                .build();
    }

    /**
     * 权限信息缓存
     *
     * @return Cache
     */
    @Bean
    public Cache<String, Object> permissionCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(60))
                .maximumSize(200)
                .recordStats()
                .build();
    }

    /**
     * 本地缓存管理器（可被子类重写）
     *
     * @return CacheManager
     */
    public CacheManager localCacheManager() {
        return cacheManager();
    }

    /**
     * 构建 Caffeine 缓存配置
     *
     * @param initialCapacity          初始容量
     * @param maximumSize              最大缓存数
     * @param expireAfterWriteMinutes  写入后过期时间（分钟）
     * @param expireAfterAccessMinutes 访问后过期时间（分钟）
     * @return Caffeine 构建器
     */
    protected Caffeine<Object, Object> buildCaffeineSpec(int initialCapacity, long maximumSize,
                                                         long expireAfterWriteMinutes, long expireAfterAccessMinutes) {
        return Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .maximumSize(maximumSize)
                .expireAfterWrite(Duration.ofMinutes(expireAfterWriteMinutes))
                .expireAfterAccess(Duration.ofMinutes(expireAfterAccessMinutes))
                .recordStats();
    }
}
