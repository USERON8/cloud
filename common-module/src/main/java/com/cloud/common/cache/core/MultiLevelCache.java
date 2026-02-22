package com.cloud.common.cache.core;

import com.cloud.common.cache.message.CacheMessage;
import com.cloud.common.cache.metrics.CacheMetricsCollector;
import com.cloud.common.cache.model.CacheObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Callable;

















@Slf4j
public class MultiLevelCache extends AbstractValueAdaptingCache {

    


    private final String name;

    


    private final Cache<Object, CacheObject> localCache;

    


    private final RedisTemplate<String, Object> redisTemplate;

    


    private final long defaultExpireSeconds;

    


    private final String keyPrefix;

    


    private final String nodeId;

    


    private final String messageTopic;

    


    private final boolean allowNullValues;

    


    private final CacheMetricsCollector metricsCollector;

    public MultiLevelCache(String name,
                           Cache<Object, CacheObject> localCache,
                           RedisTemplate<String, Object> redisTemplate,
                           long defaultExpireSeconds,
                           String keyPrefix,
                           String nodeId,
                           String messageTopic,
                           boolean allowNullValues,
                           CacheMetricsCollector metricsCollector) {
        super(allowNullValues);
        this.name = name;
        this.localCache = localCache;
        this.redisTemplate = redisTemplate;
        this.defaultExpireSeconds = defaultExpireSeconds;
        this.keyPrefix = keyPrefix;
        this.nodeId = nodeId;
        this.messageTopic = messageTopic;
        this.allowNullValues = allowNullValues;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this.localCache;
    }

    





    @Override
    protected Object lookup(Object key) {
        String cacheKey = generateRedisKey(key);
        long startTime = System.nanoTime();

        try {
            
            CacheObject localCacheObj = localCache.getIfPresent(key);
            if (localCacheObj != null && localCacheObj.isValid()) {
                log.debug("缂撳瓨L1鍛戒腑: cacheName={}, key={}", name, key);

                
                if (metricsCollector != null) {
                    long duration = (System.nanoTime() - startTime) / 1_000_000; 
                    metricsCollector.recordCacheHit(name, key.toString());
                    metricsCollector.recordCacheAccessTime(name, duration);
                }

                return localCacheObj.getRealValue();
            }

            
            Object redisValue = redisTemplate.opsForValue().get(cacheKey);
            @SuppressWarnings("unchecked")
            CacheObject redisCacheObj = (redisValue instanceof CacheObject) ? (CacheObject) redisValue : null;
            if (redisCacheObj != null && redisCacheObj.isValid()) {
                
                localCache.put(key, redisCacheObj);
                log.debug("缂撳瓨L2鍛戒腑骞跺洖濉? cacheName={}, key={}", name, key);

                
                if (metricsCollector != null) {
                    long duration = (System.nanoTime() - startTime) / 1_000_000;
                    metricsCollector.recordCacheHit(name, key.toString());
                    metricsCollector.recordCacheAccessTime(name, duration);
                }

                return redisCacheObj.getRealValue();
            }

            
            if (redisCacheObj != null && redisCacheObj.isExpired()) {
                redisTemplate.delete(cacheKey);
                log.debug("娓呯悊杩囨湡缂撳瓨: cacheName={}, key={}", name, key);
            }

            
            if (metricsCollector != null) {
                long duration = (System.nanoTime() - startTime) / 1_000_000;
                metricsCollector.recordCacheMiss(name, key.toString());
                metricsCollector.recordCacheAccessTime(name, duration);
            }

        } catch (Exception e) {
            log.warn("缂撳瓨鏌ヨ寮傚父: cacheName={}, key={}, error={}", name, key, e.getMessage());
        }

        return null; 
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        
        ValueWrapper existingValue = get(key);
        if (existingValue != null) {
            return (T) existingValue.get();
        }

        
        try {
            T value = valueLoader.call();
            
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    





    @Override
    public void put(Object key, Object value) {
        String cacheKey = generateRedisKey(key);

        try {
            
            CacheObject cacheObj = (value == null && allowNullValues)
                    ? CacheObject.nullObject(defaultExpireSeconds)
                    : CacheObject.of(value, defaultExpireSeconds);

            
            if (defaultExpireSeconds > 0) {
                redisTemplate.opsForValue().set(cacheKey, cacheObj,
                        java.time.Duration.ofSeconds(defaultExpireSeconds));
            } else {
                redisTemplate.opsForValue().set(cacheKey, cacheObj);
            }

            
            localCache.put(key, cacheObj);

            
            

            log.debug("缂撳瓨鏇存柊鎴愬姛: cacheName={}, key={}", name, key);

        } catch (Exception e) {
            log.error("缂撳瓨鍐欏叆寮傚父: cacheName={}, key={}, error={}", name, key, e.getMessage(), e);
        }
    }

    




    @Override
    public void evict(Object key) {
        String cacheKey = generateRedisKey(key);

        try {
            
            redisTemplate.delete(cacheKey);

            
            localCache.invalidate(key);

            
            

            log.debug("缂撳瓨鍒犻櫎鎴愬姛: cacheName={}, key={}", name, key);

        } catch (Exception e) {
            log.error("缂撳瓨鍒犻櫎寮傚父: cacheName={}, key={}, error={}", name, key, e.getMessage(), e);
        }
    }

    


    @Override
    public void clear() {
        try {
            
            localCache.invalidateAll();

            
            String pattern = keyPrefix + name + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));

            
            

            

        } catch (Exception e) {
            log.error("缂撳瓨娓呯┖寮傚父: cacheName={}, error={}", name, e.getMessage(), e);
        }
    }

    




    public void handleCacheMessage(CacheMessage message) {
        
        if (nodeId.equals(message.getNodeId())) {
            return;
        }

        try {
            switch (message.getOperationType()) {
                case UPDATE:
                case DELETE:
                    
                    localCache.invalidate(message.getKey());
                    log.debug("鏀跺埌缂撳瓨娑堟伅-娓呴櫎鏈湴缂撳瓨: cacheName={}, key={}, operation={}",
                            message.getCacheName(), message.getKey(), message.getOperationType());
                    break;
                case CLEAR:
                    
                    localCache.invalidateAll();
                    log.debug("鏀跺埌缂撳瓨娑堟伅-娓呯┖鏈湴缂撳瓨: cacheName={}", message.getCacheName());
                    break;
            }
        } catch (Exception e) {
            log.error("澶勭悊缂撳瓨娑堟伅寮傚父: message={}, error={}", message, e.getMessage(), e);
        }
    }

    





    private String generateRedisKey(Object key) {
        return keyPrefix + name + ":" + key.toString();
    }

    




    private void publishCacheMessage(CacheMessage message) {
        try {
            redisTemplate.convertAndSend(messageTopic, message);
        } catch (Exception e) {
            log.error("鍙戝竷缂撳瓨娑堟伅寮傚父: message={}, error={}", message, e.getMessage());
        }
    }

    




    public String getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = localCache.stats();
        return String.format("Cache[%s] Stats: hitRate=%.2f%%, missCount=%d, evictionCount=%d",
                name, stats.hitRate() * 100, stats.missCount(), stats.evictionCount());
    }
}
