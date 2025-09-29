package com.cloud.common.cache.core;

import com.cloud.common.cache.model.CacheObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 多级缓存管理器
 * 
 * 实现Spring CacheManager接口，管理多个MultiLevelCache实例。
 * 支持动态创建缓存、配置化管理和缓存统计功能。
 * 
 * @author CloudDevAgent
 * @version 2.0
 * @since 2025-09-26
 */
@Slf4j
public class MultiLevelCacheManager implements CacheManager {

    /**
     * 缓存实例存储
     */
    private final ConcurrentMap<String, MultiLevelCache> cacheMap = new ConcurrentHashMap<>();

    /**
     * Redis操作模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存配置
     */
    private final MultiLevelCacheConfig cacheConfig;

    /**
     * 当前节点ID
     */
    private final String nodeId;

    /**
     * 多级缓存配置类
     */
    public static class MultiLevelCacheConfig {
        /**
         * 默认过期时间（秒）
         */
        private long defaultExpireSeconds = 1800; // 30分钟

        /**
         * Redis键前缀
         */
        private String keyPrefix = "cache:";

        /**
         * 缓存一致性消息主题
         */
        private String messageTopic = "cache:message";

        /**
         * 是否允许空值缓存
         */
        private boolean allowNullValues = true;

        /**
         * Caffeine本地缓存配置
         */
        private CaffeineConfig caffeineConfig = new CaffeineConfig();

        // Getters and Setters
        public long getDefaultExpireSeconds() { return defaultExpireSeconds; }
        public void setDefaultExpireSeconds(long defaultExpireSeconds) { this.defaultExpireSeconds = defaultExpireSeconds; }

        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

        public String getMessageTopic() { return messageTopic; }
        public void setMessageTopic(String messageTopic) { this.messageTopic = messageTopic; }

        public boolean isAllowNullValues() { return allowNullValues; }
        public void setAllowNullValues(boolean allowNullValues) { this.allowNullValues = allowNullValues; }

        public CaffeineConfig getCaffeineConfig() { return caffeineConfig; }
        public void setCaffeineConfig(CaffeineConfig caffeineConfig) { this.caffeineConfig = caffeineConfig; }
    }

    /**
     * Caffeine缓存配置类
     */
    public static class CaffeineConfig {
        /**
         * 最大缓存条目数
         */
        private long maximumSize = 1000L;

        /**
         * 写入后过期时间（分钟）
         */
        private int expireAfterWriteMinutes = 30;

        /**
         * 访问后过期时间（分钟）
         */
        private int expireAfterAccessMinutes = 10;

        /**
         * 是否启用统计
         */
        private boolean recordStats = true;

        // Getters and Setters
        public long getMaximumSize() { return maximumSize; }
        public void setMaximumSize(long maximumSize) { this.maximumSize = maximumSize; }

        public int getExpireAfterWriteMinutes() { return expireAfterWriteMinutes; }
        public void setExpireAfterWriteMinutes(int expireAfterWriteMinutes) { this.expireAfterWriteMinutes = expireAfterWriteMinutes; }

        public int getExpireAfterAccessMinutes() { return expireAfterAccessMinutes; }
        public void setExpireAfterAccessMinutes(int expireAfterAccessMinutes) { this.expireAfterAccessMinutes = expireAfterAccessMinutes; }

        public boolean isRecordStats() { return recordStats; }
        public void setRecordStats(boolean recordStats) { this.recordStats = recordStats; }
    }

    public MultiLevelCacheManager(RedisTemplate<String, Object> redisTemplate, 
                                 MultiLevelCacheConfig cacheConfig, 
                                 String nodeId) {
        this.redisTemplate = redisTemplate;
        this.cacheConfig = cacheConfig;
        this.nodeId = nodeId;
        
        log.info("多级缓存管理器初始化完成: nodeId={}, keyPrefix={}, messageTopic={}", 
                nodeId, cacheConfig.getKeyPrefix(), cacheConfig.getMessageTopic());
    }

    @Override
    public org.springframework.cache.Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, this::createCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    /**
     * 创建新的多级缓存实例
     * 
     * @param name 缓存名称
     * @return MultiLevelCache实例
     */
    private MultiLevelCache createCache(String name) {
        // 创建本地缓存(Caffeine)
        Cache<Object, CacheObject> localCache = createCaffeineCache();

        // 创建多级缓存实例
        MultiLevelCache cache = new MultiLevelCache(
                name,
                localCache,
                redisTemplate,
                cacheConfig.getDefaultExpireSeconds(),
                cacheConfig.getKeyPrefix(),
                nodeId,
                cacheConfig.getMessageTopic(),
                cacheConfig.isAllowNullValues()
        );

        log.info("创建多级缓存: name={}, expireSeconds={}, allowNullValues={}", 
                name, cacheConfig.getDefaultExpireSeconds(), cacheConfig.isAllowNullValues());

        return cache;
    }

    /**
     * 创建Caffeine本地缓存
     * 
     * @return Caffeine缓存实例
     */
    private Cache<Object, CacheObject> createCaffeineCache() {
        CaffeineConfig config = cacheConfig.getCaffeineConfig();
        
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(config.getMaximumSize())
                .expireAfterWrite(Duration.ofMinutes(config.getExpireAfterWriteMinutes()))
                .expireAfterAccess(Duration.ofMinutes(config.getExpireAfterAccessMinutes()));

        if (config.isRecordStats()) {
            builder.recordStats();
        }

        return builder.build();
    }

    /**
     * 获取指定缓存的统计信息
     * 
     * @param cacheName 缓存名称
     * @return 统计信息，如果缓存不存在返回null
     */
    public String getCacheStats(String cacheName) {
        MultiLevelCache cache = cacheMap.get(cacheName);
        return cache != null ? cache.getStats() : null;
    }

    /**
     * 获取所有缓存的统计信息
     * 
     * @return 统计信息映射
     */
    public ConcurrentMap<String, String> getAllCacheStats() {
        ConcurrentMap<String, String> stats = new ConcurrentHashMap<>();
        cacheMap.forEach((name, cache) -> stats.put(name, cache.getStats()));
        return stats;
    }

    /**
     * 清空指定缓存
     * 
     * @param cacheName 缓存名称
     */
    public void clearCache(String cacheName) {
        MultiLevelCache cache = cacheMap.get(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("清空缓存: cacheName={}", cacheName);
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCaches() {
        cacheMap.values().forEach(MultiLevelCache::clear);
        log.info("清空所有缓存: count={}", cacheMap.size());
    }

    /**
     * 移除指定缓存实例
     * 
     * @param cacheName 缓存名称
     */
    public void removeCache(String cacheName) {
        MultiLevelCache cache = cacheMap.remove(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("移除缓存实例: cacheName={}", cacheName);
        }
    }

    /**
     * 获取缓存管理器配置
     * 
     * @return 配置对象
     */
    public MultiLevelCacheConfig getCacheConfig() {
        return cacheConfig;
    }

    /**
     * 获取当前节点ID
     * 
     * @return 节点ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 处理缓存一致性消息（由消息监听器调用）
     * 
     * @param cacheName 缓存名称
     * @param message 缓存消息
     */
    public void handleCacheMessage(String cacheName, com.cloud.common.cache.message.CacheMessage message) {
        MultiLevelCache cache = cacheMap.get(cacheName);
        if (cache != null) {
            cache.handleCacheMessage(message);
        }
    }

    /**
     * 获取缓存实例映射（仅用于调试和监控）
     * 
     * @return 只读的缓存映射
     */
    public ConcurrentMap<String, MultiLevelCache> getCacheMap() {
        return new ConcurrentHashMap<>(cacheMap);
    }
}
