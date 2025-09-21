package com.cloud.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 智能缓存管理器
 * 提供缓存预热、穿透防护、智能更新等高级缓存策略
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartCacheManager {

    // 缓存穿透防护 - 空值缓存
    private static final String NULL_VALUE = "CACHE_NULL_VALUE";
    private static final Duration NULL_VALUE_TTL = Duration.ofMinutes(5);
    private final RedisTemplate<String, Object> redisTemplate;
    // 缓存预热任务调度器
    private final ScheduledExecutorService preheatingExecutor = Executors.newScheduledThreadPool(2);

    // 热点数据访问统计
    private final Map<String, CacheAccessStats> accessStats = new ConcurrentHashMap<>();

    // 预热任务注册表
    private final Map<String, CachePreheatingTask> preheatingTasks = new ConcurrentHashMap<>();

    /**
     * 智能获取缓存数据
     * 包含穿透防护、统计记录、自动预热等功能
     *
     * @param key             缓存键
     * @param dataLoader      数据加载器
     * @param ttl             缓存时间
     * @param enableNullCache 是否启用空值缓存
     * @return 缓存数据
     */
    public <T> T smartGet(String key, Supplier<T> dataLoader, Duration ttl, boolean enableNullCache) {
        // 记录访问统计
        CacheAccessStats stats = accessStats.computeIfAbsent(key, k -> new CacheAccessStats());

        try {
            // 尝试从Redis获取数据
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                // 缓存命中
                stats.recordHit();

                // 检查是否是空值缓存
                if (NULL_VALUE.equals(cached)) {
                    return null;
                }

                @SuppressWarnings("unchecked")
                T result = (T) cached;

                // 智能预热判断
                checkAndSchedulePreheating(key, dataLoader, ttl);

                return result;
            }

            // 缓存未命中
            stats.recordMiss();

            // 使用分布式锁防止缓存击穿
            return getWithLock(key, dataLoader, ttl, enableNullCache);

        } catch (Exception e) {
            log.error("智能缓存获取失败, key: {}", key, e);
            // 降级到直接调用数据加载器
            return dataLoader.get();
        }
    }

    /**
     * 使用分布式锁获取数据，防止缓存击穿
     */
    private <T> T getWithLock(String key, Supplier<T> dataLoader, Duration ttl, boolean enableNullCache) {
        String lockKey = "lock:" + key;
        String lockValue = UUID.randomUUID().toString();

        try {
            // 尝试获取分布式锁
            Boolean lockAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));

            if (Boolean.TRUE.equals(lockAcquired)) {
                try {
                    // 获得锁，加载数据
                    T data = dataLoader.get();

                    if (data != null) {
                        // 数据不为空，正常缓存
                        redisTemplate.opsForValue().set(key, data, ttl);
                        log.debug("缓存数据更新: key={}, ttl={}", key, ttl);
                    } else if (enableNullCache) {
                        // 数据为空且启用空值缓存
                        redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_VALUE_TTL);
                        log.debug("空值缓存设置: key={}", key);
                    }

                    return data;

                } finally {
                    // 释放锁
                    releaseLock(lockKey, lockValue);
                }
            } else {
                // 未获得锁，等待一段时间后重试
                Thread.sleep(100);

                // 再次尝试从缓存获取
                Object cached = redisTemplate.opsForValue().get(key);
                if (cached != null && !NULL_VALUE.equals(cached)) {
                    @SuppressWarnings("unchecked")
                    T result = (T) cached;
                    return result;
                }

                // 如果还是没有，直接调用数据加载器（降级策略）
                return dataLoader.get();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断, key: {}", key);
            return dataLoader.get();
        } catch (Exception e) {
            log.error("分布式锁处理异常, key: {}", key, e);
            return dataLoader.get();
        }
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(String lockKey, String lockValue) {
        try {
            // 使用Lua脚本确保原子性释放锁
            String luaScript =
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "    return redis.call('del', KEYS[1]) " +
                            "else " +
                            "    return 0 " +
                            "end";

            redisTemplate.execute((RedisCallback<Long>) connection ->
                    connection.eval(luaScript.getBytes(), ReturnType.INTEGER, 1, lockKey.getBytes(), lockValue.getBytes()));

        } catch (Exception e) {
            log.error("释放分布式锁失败, lockKey: {}", lockKey, e);
        }
    }

    /**
     * 检查并调度缓存预热
     */
    private <T> void checkAndSchedulePreheating(String key, Supplier<T> dataLoader, Duration ttl) {
        CacheAccessStats stats = accessStats.get(key);
        if (stats == null) return;

        // 判断是否为热点数据（访问次数 > 10 且命中率 > 80%）
        boolean isHotData = stats.getAccessCount() > 10 && stats.getHitRate() > 0.8;

        if (isHotData && !preheatingTasks.containsKey(key)) {
            // 注册预热任务
            registerPreheatingTask(key, () -> dataLoader.get(), Duration.ofMinutes(5), ttl);
        }
    }

    /**
     * 注册缓存预热任务
     *
     * @param cacheKey   缓存键
     * @param dataLoader 数据加载器
     * @param interval   预热间隔
     * @param ttl        缓存时间
     */
    public void registerPreheatingTask(String cacheKey, Supplier<Object> dataLoader,
                                       Duration interval, Duration ttl) {

        CachePreheatingTask task = new CachePreheatingTask(cacheKey, dataLoader, interval, ttl);
        preheatingTasks.put(cacheKey, task);

        // 调度预热任务
        preheatingExecutor.scheduleWithFixedDelay(() -> {
            try {
                Object data = dataLoader.get();
                if (data != null) {
                    redisTemplate.opsForValue().set(cacheKey, data, ttl);
                    log.debug("缓存预热完成: key={}", cacheKey);
                }
            } catch (Exception e) {
                log.error("缓存预热失败: key={}", cacheKey, e);
            }
        }, 0, interval.getSeconds(), TimeUnit.SECONDS);

        log.info("注册缓存预热任务: key={}, interval={}", cacheKey, interval);
    }

    /**
     * 批量预热缓存
     *
     * @param preheatingMap 预热数据映射
     */
    public void batchPreheating(Map<String, Object> preheatingMap, Duration ttl) {
        CompletableFuture.runAsync(() -> {
            try {
                redisTemplate.opsForValue().multiSet(preheatingMap);

                // 设置过期时间
                preheatingMap.keySet().forEach(key ->
                        redisTemplate.expire(key, ttl));

                log.info("批量缓存预热完成，数量: {}", preheatingMap.size());

            } catch (Exception e) {
                log.error("批量缓存预热失败", e);
            }
        });
    }

    /**
     * 智能删除缓存
     * 支持模式匹配删除 - 使用SCAN命令避免阻塞
     *
     * @param pattern 缓存键模式
     */
    public void smartEvict(String pattern) {
        try {
            Set<String> keys = scanKeys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);

                // 清理访问统计
                keys.forEach(accessStats::remove);
                keys.forEach(preheatingTasks::remove);

                log.info("智能删除缓存完成，模式: {}, 删除数量: {}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("智能删除缓存失败，模式: {}", pattern, e);
        }
    }

    /**
     * 使用SCAN命令获取匹配的key，避免阻塞Redis
     *
     * @param pattern 匹配模式
     * @return 匹配的key集合
     */
    private Set<String> scanKeys(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keySet = new HashSet<>();
            try (Cursor<byte[]> cursor = connection.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(pattern)
                            .count(1000)
                            .build())) {

                while (cursor.hasNext()) {
                    keySet.add(new String(cursor.next()));
                }
            } catch (Exception e) {
                log.error("SCAN操作失败", e);
            }
            return keySet;
        });
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    public Map<String, CacheAccessStats> getCacheStats() {
        return new HashMap<>(accessStats);
    }

    /**
     * 获取热点缓存Top N
     *
     * @param topN 排名前N
     * @return 热点缓存列表
     */
    public List<Map.Entry<String, CacheAccessStats>> getHotCache(int topN) {
        return accessStats.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().getAccessCount(), e1.getValue().getAccessCount()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * 清理过期统计数据
     */
    public void cleanupExpiredStats() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        accessStats.entrySet().removeIf(entry -> {
            LocalDateTime lastAccess = entry.getValue().getLastAccessTime();
            return lastAccess != null && lastAccess.isBefore(cutoffTime);
        });

        log.info("清理过期缓存统计数据完成");
    }

    /**
     * 缓存访问统计信息
     */
    public static class CacheAccessStats {
        private long accessCount;
        private LocalDateTime lastAccessTime;
        private long hitCount;
        private long missCount;

        public void recordHit() {
            accessCount++;
            hitCount++;
            lastAccessTime = LocalDateTime.now();
        }

        public void recordMiss() {
            accessCount++;
            missCount++;
            lastAccessTime = LocalDateTime.now();
        }

        public double getHitRate() {
            return accessCount > 0 ? (double) hitCount / accessCount : 0.0;
        }

        // Getters and Setters
        public long getAccessCount() {
            return accessCount;
        }

        public LocalDateTime getLastAccessTime() {
            return lastAccessTime;
        }

        public long getHitCount() {
            return hitCount;
        }

        public long getMissCount() {
            return missCount;
        }
    }

    /**
     * 缓存预热任务
     */
    public static class CachePreheatingTask {
        private final String cacheKey;
        private final Supplier<Object> dataLoader;
        private final Duration interval;
        private final Duration ttl;

        public CachePreheatingTask(String cacheKey, Supplier<Object> dataLoader,
                                   Duration interval, Duration ttl) {
            this.cacheKey = cacheKey;
            this.dataLoader = dataLoader;
            this.interval = interval;
            this.ttl = ttl;
        }

        // Getters
        public String getCacheKey() {
            return cacheKey;
        }

        public Supplier<Object> getDataLoader() {
            return dataLoader;
        }

        public Duration getInterval() {
            return interval;
        }

        public Duration getTtl() {
            return ttl;
        }
    }
}
