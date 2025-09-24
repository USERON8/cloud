package com.cloud.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;

/**
 * 本地缓存基础配置类
 * 仅提供工具方法，不再自动创建Bean
 * 各服务根据需要选择是否启用本地缓存
 *
 * @author cloud
 * @date 2024-01-20
 */
@Slf4j
@ConditionalOnClass(Caffeine.class)
public abstract class BaseLocalCacheConfig {

    /**
     * 创建基础的CacheManager
     * 子类可以调用此方法创建CacheManager
     *
     * @return CacheManager
     */
    protected CacheManager createCacheManager() {
        log.info("创建基础CacheManager");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(createDefaultCaffeineBuilder());
        return cacheManager;
    }

    /**
     * 创建默认的Caffeine构建器
     *
     * @return Caffeine构建器
     */
    protected Caffeine<Object, Object> createDefaultCaffeineBuilder() {
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

    /**
     * 构建简化的 Caffeine 缓存配置
     *
     * @param maximumSize             最大缓存数
     * @param expireAfterWriteMinutes 写入后过期时间（分钟）
     * @return Caffeine 构建器
     */
    protected Caffeine<Object, Object> buildSimpleCaffeineSpec(long maximumSize, long expireAfterWriteMinutes) {
        return Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(Duration.ofMinutes(expireAfterWriteMinutes))
                .recordStats();
    }

    /**
     * 获取服务名称
     * 子类应该重写此方法返回具体的服务名称
     *
     * @return 服务名称
     */
    protected abstract String getServiceName();

    /**
     * 获取缓存名称列表
     * 子类可以重写此方法返回预定义的缓存名称
     *
     * @return 缓存名称数组
     */
    protected String[] getCacheNames() {
        return new String[0];
    }
}
