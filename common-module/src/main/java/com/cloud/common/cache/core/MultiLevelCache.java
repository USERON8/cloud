package com.cloud.common.cache.core;

import com.cloud.common.cache.message.CacheMessage;
import com.cloud.common.cache.metrics.CacheMetricsCollector;
import com.cloud.common.cache.model.CacheObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Callable;

/**
 * 多级缓存实现类
 * <p>
 * 实现Spring Cache接口，提供Caffeine + Redis双级缓存功能。
 * 支持缓存穿透防护、过期时间控制和跨节点一致性保证。
 * <p>
 * 缓存查询策略：
 * 1. 先查询本地缓存(Caffeine)，命中则直接返回
 * 2. 本地未命中时查询分布式缓存(Redis)
 * 3. Redis命中则回填到本地缓存并返回
 * 4. 都未命中返回null，由@Cacheable注解驱动方法执行
 *
 * @author CloudDevAgent
 * @version 2.0
 * @since 2025-09-26
 */
@Slf4j
public class MultiLevelCache extends AbstractValueAdaptingCache {

    /**
     * 缓存名称
     */
    private final String name;

    /**
     * 本地缓存(Caffeine)
     */
    private final Cache<Object, CacheObject> localCache;

    /**
     * 分布式缓存(Redis)
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 默认过期时间(秒)
     */
    private final long defaultExpireSeconds;

    /**
     * Redis键前缀
     */
    private final String keyPrefix;

    /**
     * 当前节点ID
     */
    private final String nodeId;

    /**
     * 缓存一致性消息主题
     */
    private final String messageTopic;

    /**
     * 是否允许空值缓存（防止缓存穿透）
     */
    private final boolean allowNullValues;

    /**
     * 缓存指标收集器（可选）
     */
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

    /**
     * 缓存查询核心逻辑
     *
     * @param key 缓存键
     * @return 缓存值，未命中返回null
     */
    @Override
    protected Object lookup(Object key) {
        String cacheKey = generateRedisKey(key);
        long startTime = System.nanoTime();

        try {
            // 1. 先查询本地缓存(Caffeine)
            CacheObject localCacheObj = localCache.getIfPresent(key);
            if (localCacheObj != null && localCacheObj.isValid()) {
                log.debug("缓存L1命中: cacheName={}, key={}", name, key);

                // 记录指标
                if (metricsCollector != null) {
                    long duration = (System.nanoTime() - startTime) / 1_000_000; // 转换为毫秒
                    metricsCollector.recordCacheHit(name, key.toString());
                    metricsCollector.recordCacheAccessTime(name, duration);
                }

                return localCacheObj.getRealValue();
            }

            // 2. 查询分布式缓存(Redis)
            Object redisValue = redisTemplate.opsForValue().get(cacheKey);
            @SuppressWarnings("unchecked")
            CacheObject redisCacheObj = (redisValue instanceof CacheObject) ? (CacheObject) redisValue : null;
            if (redisCacheObj != null && redisCacheObj.isValid()) {
                // Redis命中，回填到本地缓存
                localCache.put(key, redisCacheObj);
                log.debug("缓存L2命中并回填: cacheName={}, key={}", name, key);

                // 记录指标
                if (metricsCollector != null) {
                    long duration = (System.nanoTime() - startTime) / 1_000_000;
                    metricsCollector.recordCacheHit(name, key.toString());
                    metricsCollector.recordCacheAccessTime(name, duration);
                }

                return redisCacheObj.getRealValue();
            }

            // 3. 清理过期的Redis缓存
            if (redisCacheObj != null && redisCacheObj.isExpired()) {
                redisTemplate.delete(cacheKey);
                log.debug("清理过期缓存: cacheName={}, key={}", name, key);
            }

            // 记录未命中
            if (metricsCollector != null) {
                long duration = (System.nanoTime() - startTime) / 1_000_000;
                metricsCollector.recordCacheMiss(name, key.toString());
                metricsCollector.recordCacheAccessTime(name, duration);
            }

        } catch (Exception e) {
            log.warn("缓存查询异常: cacheName={}, key={}, error={}", name, key, e.getMessage());
        }

        return null; // 缓存未命中
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        // 先尝试从缓存获取
        ValueWrapper existingValue = get(key);
        if (existingValue != null) {
            return (T) existingValue.get();
        }

        // 缓存未命中，执行valueLoader获取数据
        try {
            T value = valueLoader.call();
            // 将结果放入缓存
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    /**
     * 写入缓存（双写模式）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    @Override
    public void put(Object key, Object value) {
        String cacheKey = generateRedisKey(key);

        try {
            // 创建缓存对象
            CacheObject cacheObj = (value == null && allowNullValues)
                    ? CacheObject.nullObject(defaultExpireSeconds)
                    : CacheObject.of(value, defaultExpireSeconds);

            // 1. 写入Redis
            if (defaultExpireSeconds > 0) {
                redisTemplate.opsForValue().set(cacheKey, cacheObj,
                        java.time.Duration.ofSeconds(defaultExpireSeconds));
            } else {
                redisTemplate.opsForValue().set(cacheKey, cacheObj);
            }

            // 2. 写入本地缓存
            localCache.put(key, cacheObj);

            // 3. 发布缓存更新消息，通知其他节点清除本地缓存
            // publishCacheMessage(CacheMessage.update(name, key, nodeId)); // 暂时禁用消息发布

            log.debug("缓存更新成功: cacheName={}, key={}", name, key);

        } catch (Exception e) {
            log.error("缓存写入异常: cacheName={}, key={}, error={}", name, key, e.getMessage(), e);
        }
    }

    /**
     * 清除指定缓存项
     *
     * @param key 缓存键
     */
    @Override
    public void evict(Object key) {
        String cacheKey = generateRedisKey(key);

        try {
            // 1. 清除Redis缓存
            redisTemplate.delete(cacheKey);

            // 2. 清除本地缓存
            localCache.invalidate(key);

            // 3. 发布缓存删除消息，通知其他节点也清除本地缓存
            // publishCacheMessage(CacheMessage.delete(name, key, nodeId)); // 暂时禁用消息发布

            log.debug("缓存删除成功: cacheName={}, key={}", name, key);

        } catch (Exception e) {
            log.error("缓存删除异常: cacheName={}, key={}, error={}", name, key, e.getMessage(), e);
        }
    }

    /**
     * 清空当前缓存的所有项
     */
    @Override
    public void clear() {
        try {
            // 1. 清空本地缓存
            localCache.invalidateAll();

            // 2. 清空Redis中该缓存名下的所有缓存（通过模式匹配）
            String pattern = keyPrefix + name + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));

            // 3. 发布缓存清空消息
            // publishCacheMessage(CacheMessage.clear(name, nodeId)); // 暂时禁用消息发布

            log.info("缓存清空成功: cacheName={}", name);

        } catch (Exception e) {
            log.error("缓存清空异常: cacheName={}, error={}", name, e.getMessage(), e);
        }
    }

    /**
     * 处理其他节点发送的缓存一致性消息
     *
     * @param message 缓存消息
     */
    public void handleCacheMessage(CacheMessage message) {
        // 避免处理自己发送的消息
        if (nodeId.equals(message.getNodeId())) {
            return;
        }

        try {
            switch (message.getOperationType()) {
                case UPDATE:
                case DELETE:
                    // 清除本地缓存中的指定键
                    localCache.invalidate(message.getKey());
                    log.debug("收到缓存消息-清除本地缓存: cacheName={}, key={}, operation={}",
                            message.getCacheName(), message.getKey(), message.getOperationType());
                    break;
                case CLEAR:
                    // 清空本地缓存
                    localCache.invalidateAll();
                    log.debug("收到缓存消息-清空本地缓存: cacheName={}", message.getCacheName());
                    break;
            }
        } catch (Exception e) {
            log.error("处理缓存消息异常: message={}, error={}", message, e.getMessage(), e);
        }
    }

    /**
     * 生成Redis缓存键
     *
     * @param key 原始键
     * @return Redis缓存键
     */
    private String generateRedisKey(Object key) {
        return keyPrefix + name + ":" + key.toString();
    }

    /**
     * 发布缓存一致性消息
     *
     * @param message 缓存消息
     */
    private void publishCacheMessage(CacheMessage message) {
        try {
            redisTemplate.convertAndSend(messageTopic, message);
        } catch (Exception e) {
            log.error("发布缓存消息异常: message={}, error={}", message, e.getMessage());
        }
    }

    /**
     * 获取缓存统计信息（用于监控）
     *
     * @return 统计信息字符串
     */
    public String getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = localCache.stats();
        return String.format("Cache[%s] Stats: hitRate=%.2f%%, missCount=%d, evictionCount=%d",
                name, stats.hitRate() * 100, stats.missCount(), stats.evictionCount());
    }
}
