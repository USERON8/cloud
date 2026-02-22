package com.cloud.common.cache.core;

import com.cloud.common.cache.metrics.CacheMetricsCollector;
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











@Slf4j
public class MultiLevelCacheManager implements CacheManager {

    


    private final ConcurrentMap<String, MultiLevelCache> cacheMap = new ConcurrentHashMap<>();

    


    private final RedisTemplate<String, Object> redisTemplate;

    


    private final MultiLevelCacheConfig cacheConfig;

    


    private final String nodeId;

    


    private CacheMetricsCollector metricsCollector;

    public MultiLevelCacheManager(RedisTemplate<String, Object> redisTemplate,
                                  MultiLevelCacheConfig cacheConfig,
                                  String nodeId,
                                  CacheMetricsCollector metricsCollector) {
        this.redisTemplate = redisTemplate;
        this.cacheConfig = cacheConfig;
        this.nodeId = nodeId;
        this.metricsCollector = metricsCollector;

        

    }

    


    public MultiLevelCacheManager(RedisTemplate<String, Object> redisTemplate,
                                  MultiLevelCacheConfig cacheConfig,
                                  String nodeId) {
        this.redisTemplate = redisTemplate;
        this.cacheConfig = cacheConfig;
        this.nodeId = nodeId;
        this.metricsCollector = null; 

        

    }

    


    public void setMetricsCollector(CacheMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        
    }

    @Override
    public org.springframework.cache.Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, this::createCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    





    private MultiLevelCache createCache(String name) {
        
        Cache<Object, CacheObject> localCache = createCaffeineCache();

        
        MultiLevelCache cache = new MultiLevelCache(
                name,
                localCache,
                redisTemplate,
                cacheConfig.getDefaultExpireSeconds(),
                cacheConfig.getKeyPrefix(),
                nodeId,
                cacheConfig.getMessageTopic(),
                cacheConfig.isAllowNullValues(),
                metricsCollector  
        );

        


        return cache;
    }

    




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

    





    public String getCacheStats(String cacheName) {
        MultiLevelCache cache = cacheMap.get(cacheName);
        return cache != null ? cache.getStats() : null;
    }

    




    public ConcurrentMap<String, String> getAllCacheStats() {
        ConcurrentMap<String, String> stats = new ConcurrentHashMap<>();
        cacheMap.forEach((name, cache) -> stats.put(name, cache.getStats()));
        return stats;
    }

    




    public void clearCache(String cacheName) {
        MultiLevelCache cache = cacheMap.get(cacheName);
        if (cache != null) {
            cache.clear();
            
        }
    }

    


    public void clearAllCaches() {
        cacheMap.values().forEach(MultiLevelCache::clear);
        
    }

    




    public void removeCache(String cacheName) {
        MultiLevelCache cache = cacheMap.remove(cacheName);
        if (cache != null) {
            cache.clear();
            
        }
    }

    




    public MultiLevelCacheConfig getCacheConfig() {
        return cacheConfig;
    }

    




    public String getNodeId() {
        return nodeId;
    }

    





    public void handleCacheMessage(String cacheName, com.cloud.common.cache.message.CacheMessage message) {
        MultiLevelCache cache = cacheMap.get(cacheName);
        if (cache != null) {
            cache.handleCacheMessage(message);
        }
    }

    




    public ConcurrentMap<String, MultiLevelCache> getCacheMap() {
        return new ConcurrentHashMap<>(cacheMap);
    }

    


    public static class MultiLevelCacheConfig {
        


        private long defaultExpireSeconds = 1800; 

        


        private String keyPrefix = "cache:";

        


        private String messageTopic = "cache:message";

        


        private boolean allowNullValues = true;

        


        private CaffeineConfig caffeineConfig = new CaffeineConfig();

        
        public long getDefaultExpireSeconds() {
            return defaultExpireSeconds;
        }

        public void setDefaultExpireSeconds(long defaultExpireSeconds) {
            this.defaultExpireSeconds = defaultExpireSeconds;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getMessageTopic() {
            return messageTopic;
        }

        public void setMessageTopic(String messageTopic) {
            this.messageTopic = messageTopic;
        }

        public boolean isAllowNullValues() {
            return allowNullValues;
        }

        public void setAllowNullValues(boolean allowNullValues) {
            this.allowNullValues = allowNullValues;
        }

        public CaffeineConfig getCaffeineConfig() {
            return caffeineConfig;
        }

        public void setCaffeineConfig(CaffeineConfig caffeineConfig) {
            this.caffeineConfig = caffeineConfig;
        }
    }

    


    public static class CaffeineConfig {
        


        private long maximumSize = 1000L;

        


        private int expireAfterWriteMinutes = 30;

        


        private int expireAfterAccessMinutes = 10;

        


        private boolean recordStats = true;

        
        public long getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
        }

        public int getExpireAfterWriteMinutes() {
            return expireAfterWriteMinutes;
        }

        public void setExpireAfterWriteMinutes(int expireAfterWriteMinutes) {
            this.expireAfterWriteMinutes = expireAfterWriteMinutes;
        }

        public int getExpireAfterAccessMinutes() {
            return expireAfterAccessMinutes;
        }

        public void setExpireAfterAccessMinutes(int expireAfterAccessMinutes) {
            this.expireAfterAccessMinutes = expireAfterAccessMinutes;
        }

        public boolean isRecordStats() {
            return recordStats;
        }

        public void setRecordStats(boolean recordStats) {
            this.recordStats = recordStats;
        }
    }
}
