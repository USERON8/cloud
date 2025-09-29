package com.cloud.common.cache.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.metrics.cache.CacheMetricsRegistrar;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存监控指标收集器
 * 
 * 主要功能：
 * - 收集缓存命中率、大小等核心指标
 * - 支持多级缓存指标统计
 * - 集成 Spring Boot Actuator
 * - 提供缓存热点数据分析
 *
 * @author CloudDevAgent
 * @version 1.0
 * @since 2025-09-27
 */
@Component
@Slf4j
public class CacheMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    private final CacheMetricsRegistrar cacheMetricsRegistrar;
    
    // 缓存指标存储
    private final ConcurrentHashMap<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();
    
    // 自定义计数器和计时器
    private Counter cacheHitCounter;
    private Counter cacheMissCounter;
    private Timer cacheAccessTimer;
    private Counter cacheEvictionCounter;
    
    public CacheMetricsCollector(MeterRegistry meterRegistry, 
                                CacheManager cacheManager,
                                CacheMetricsRegistrar cacheMetricsRegistrar) {
        this.meterRegistry = meterRegistry;
        this.cacheManager = cacheManager;
        this.cacheMetricsRegistrar = cacheMetricsRegistrar;
    }
    
    @PostConstruct
    public void initMetrics() {
        log.info("初始化缓存监控指标...");
        
        // 初始化自定义指标
        initCustomMetrics();
        
        // 注册现有缓存的指标
        registerExistingCaches();
        
        // 注册缓存大小和统计指标
        registerCacheSizeMetrics();
        
        log.info("缓存监控指标初始化完成");
    }
    
    /**
     * 初始化自定义指标
     */
    private void initCustomMetrics() {
        cacheHitCounter = Counter.builder("cache.hit")
                .description("缓存命中次数")
                .register(meterRegistry);
                
        cacheMissCounter = Counter.builder("cache.miss")
                .description("缓存未命中次数")
                .register(meterRegistry);
                
        cacheAccessTimer = Timer.builder("cache.access")
                .description("缓存访问耗时")
                .register(meterRegistry);
                
        cacheEvictionCounter = Counter.builder("cache.eviction")
                .description("缓存驱逐次数")
                .register(meterRegistry);
    }
    
    /**
     * 注册现有缓存的指标
     */
    private void registerExistingCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                log.debug("注册缓存指标: {}", cacheName);
                cacheMetricsRegistrar.bindCacheToRegistry(cache);
                
                // 初始化缓存统计
                cacheStatsMap.put(cacheName, new CacheStats(cacheName));
            }
        });
    }
    
    /**
     * 注册缓存大小和统计指标
     */
    private void registerCacheSizeMetrics() {
        // 注册缓存命中率指标
        Gauge.builder("cache.hit_ratio", meterRegistry, registry -> calculateHitRatio())
                .description("缓存命中率")
                .register(meterRegistry);
        
        // 注册活跃缓存数量
        Gauge.builder("cache.active_count", meterRegistry, registry -> (double) cacheManager.getCacheNames().size())
                .description("活跃缓存数量")
                .register(meterRegistry);
        
        // 注册缓存总访问次数 
        Gauge.builder("cache.total_access", meterRegistry, registry -> (double) getTotalAccessCount())
                .description("缓存总访问次数")
                .register(meterRegistry);
    }
    
    /**
     * 记录缓存命中
     */
    public void recordCacheHit(String cacheName, String key) {
        cacheHitCounter.increment();
        CacheStats stats = cacheStatsMap.get(cacheName);
        if (stats != null) {
            stats.recordHit();
            stats.recordKeyAccess(key);
        }
        log.debug("缓存命中: cache={}, key={}", cacheName, key);
    }
    
    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss(String cacheName, String key) {
        cacheMissCounter.increment();
        CacheStats stats = cacheStatsMap.get(cacheName);
        if (stats != null) {
            stats.recordMiss();
            stats.recordKeyAccess(key);
        }
        log.debug("缓存未命中: cache={}, key={}", cacheName, key);
    }
    
    /**
     * 记录缓存访问耗时
     */
    public void recordCacheAccessTime(String cacheName, long durationMs) {
        cacheAccessTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        CacheStats stats = cacheStatsMap.get(cacheName);
        if (stats != null) {
            stats.recordAccessTime(durationMs);
        }
    }
    
    /**
     * 记录缓存驱逐
     */
    public void recordCacheEviction(String cacheName, String key) {
        cacheEvictionCounter.increment();
        CacheStats stats = cacheStatsMap.get(cacheName);
        if (stats != null) {
            stats.recordEviction();
        }
        log.debug("缓存驱逐: cache={}, key={}", cacheName, key);
    }
    
    /**
     * 计算总体命中率
     */
    private double calculateHitRatio() {
        long totalHits = cacheStatsMap.values().stream()
                .mapToLong(stats -> stats.getHitCount().get())
                .sum();
        long totalMisses = cacheStatsMap.values().stream()
                .mapToLong(stats -> stats.getMissCount().get())
                .sum();
        
        long totalAccess = totalHits + totalMisses;
        return totalAccess > 0 ? (double) totalHits / totalAccess : 0.0;
    }
    
    /**
     * 获取总访问次数
     */
    private long getTotalAccessCount() {
        return cacheStatsMap.values().stream()
                .mapToLong(stats -> stats.getHitCount().get() + stats.getMissCount().get())
                .sum();
    }
    
    /**
     * 获取指定缓存的统计信息
     */
    public CacheStats getCacheStats(String cacheName) {
        return cacheStatsMap.get(cacheName);
    }
    
    /**
     * 获取热点数据分析
     */
    public void analyzeHotspotData() {
        log.info("开始分析缓存热点数据...");
        
        cacheStatsMap.forEach((cacheName, stats) -> {
            log.info("缓存统计 - {}: 命中率={:.2f}%, 总访问={}次, 平均耗时={}ms",
                    cacheName,
                    stats.getHitRatio() * 100,
                    stats.getTotalAccess(),
                    stats.getAverageAccessTime());
            
            // 输出访问频率最高的 key
            stats.getTopAccessedKeys(5).forEach((key, count) ->
                    log.info("  热点数据: {} -> 访问{}次", key, count));
        });
    }
    
    /**
     * 缓存统计内部类
     */
    public static class CacheStats {
        private final String cacheName;
        private final AtomicLong hitCount = new AtomicLong(0);
        private final AtomicLong missCount = new AtomicLong(0);
        private final AtomicLong evictionCount = new AtomicLong(0);
        private final AtomicLong totalAccessTime = new AtomicLong(0);
        private final AtomicLong accessCount = new AtomicLong(0);
        
        // 用于统计热点数据
        private final ConcurrentHashMap<String, AtomicLong> keyAccessCount = new ConcurrentHashMap<>();
        
        public CacheStats(String cacheName) {
            this.cacheName = cacheName;
        }
        
        public void recordHit() {
            hitCount.incrementAndGet();
        }
        
        public void recordMiss() {
            missCount.incrementAndGet();
        }
        
        public void recordEviction() {
            evictionCount.incrementAndGet();
        }
        
        public void recordAccessTime(long timeMs) {
            totalAccessTime.addAndGet(timeMs);
            accessCount.incrementAndGet();
        }
        
        public void recordKeyAccess(String key) {
            keyAccessCount.computeIfAbsent(key, k -> new AtomicLong(0))
                    .incrementAndGet();
        }
        
        public double getHitRatio() {
            long total = hitCount.get() + missCount.get();
            return total > 0 ? (double) hitCount.get() / total : 0.0;
        }
        
        public long getTotalAccess() {
            return hitCount.get() + missCount.get();
        }
        
        public double getAverageAccessTime() {
            long count = accessCount.get();
            return count > 0 ? (double) totalAccessTime.get() / count : 0.0;
        }
        
        public java.util.Map<String, Long> getTopAccessedKeys(int limit) {
            return keyAccessCount.entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, AtomicLong>comparingByValue(
                            (a, b) -> Long.compare(b.get(), a.get())))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toMap(
                            java.util.Map.Entry::getKey,
                            entry -> entry.getValue().get(),
                            (a, b) -> a,
                            java.util.LinkedHashMap::new));
        }
        
        // Getters
        public String getCacheName() { return cacheName; }
        public AtomicLong getHitCount() { return hitCount; }
        public AtomicLong getMissCount() { return missCount; }
        public AtomicLong getEvictionCount() { return evictionCount; }
    }
    
    private double calculateHitRatio(Cache cache) {
        // 简化的命中率计算
        return 0.95; // 默认95%命中率
    }

    /**
     * 计算缓存命中次数
     * @return 命中次数
     */
    private double calculateHitCount() {
        return 0.0; // 默认实现，实际应该从缓存统计中获取
    }
    
    /**
     * 计算缓存未命中次数
     * @return 未命中次数
     */
    private double calculateMissCount() {
        return 0.0; // 默认实现，实际应该从缓存统计中获取
    }
    
    /**
     * 计算缓存命中率
     * @return 命中率(0.0-1.0)
     */

}