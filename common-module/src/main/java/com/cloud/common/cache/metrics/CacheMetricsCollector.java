package com.cloud.common.cache.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.metrics.cache.CacheMetricsRegistrar;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CacheMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    private final CacheMetricsRegistrar cacheMetricsRegistrar;
    private final ConcurrentHashMap<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();

    private Counter cacheHitCounter;
    private Counter cacheMissCounter;
    private Timer cacheAccessTimer;
    private Counter cacheEvictionCounter;

    public CacheMetricsCollector(MeterRegistry meterRegistry,
                                 @Lazy CacheManager cacheManager,
                                 CacheMetricsRegistrar cacheMetricsRegistrar) {
        this.meterRegistry = meterRegistry;
        this.cacheManager = cacheManager;
        this.cacheMetricsRegistrar = cacheMetricsRegistrar;
    }

    @PostConstruct
    public void initMetrics() {
        initCustomMetrics();
        registerExistingCaches();
        registerCacheGauges();
    }

    private void initCustomMetrics() {
        cacheHitCounter = Counter.builder("cache.hit").description("Cache hit count").register(meterRegistry);
        cacheMissCounter = Counter.builder("cache.miss").description("Cache miss count").register(meterRegistry);
        cacheAccessTimer = Timer.builder("cache.access").description("Cache access time").register(meterRegistry);
        cacheEvictionCounter = Counter.builder("cache.eviction").description("Cache eviction count").register(meterRegistry);
    }

    private void registerExistingCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cacheMetricsRegistrar.bindCacheToRegistry(cache);
                cacheStatsMap.putIfAbsent(cacheName, new CacheStats(cacheName));
            }
        });
    }

    private void registerCacheGauges() {
        Gauge.builder("cache.hit_ratio", this, CacheMetricsCollector::calculateHitRatio)
                .description("Global cache hit ratio")
                .register(meterRegistry);

        Gauge.builder("cache.active_count", this, c -> (double) c.cacheManager.getCacheNames().size())
                .description("Active cache count")
                .register(meterRegistry);

        Gauge.builder("cache.total_access", this, c -> (double) c.getTotalAccessCount())
                .description("Total cache access count")
                .register(meterRegistry);
    }

    public void recordCacheHit(String cacheName, String key) {
        cacheHitCounter.increment();
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, CacheStats::new);
        stats.recordHit();
        stats.recordKeyAccess(key);
    }

    public void recordCacheMiss(String cacheName, String key) {
        cacheMissCounter.increment();
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, CacheStats::new);
        stats.recordMiss();
        stats.recordKeyAccess(key);
    }

    public void recordCacheAccessTime(String cacheName, long durationMs) {
        cacheAccessTimer.record(durationMs, TimeUnit.MILLISECONDS);
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, CacheStats::new);
        stats.recordAccessTime(durationMs);
    }

    public void recordCacheEviction(String cacheName, String key) {
        cacheEvictionCounter.increment();
        CacheStats stats = cacheStatsMap.computeIfAbsent(cacheName, CacheStats::new);
        stats.recordEviction();
    }

    private double calculateHitRatio() {
        long totalHits = cacheStatsMap.values().stream().mapToLong(s -> s.getHitCount().get()).sum();
        long totalMisses = cacheStatsMap.values().stream().mapToLong(s -> s.getMissCount().get()).sum();
        long total = totalHits + totalMisses;
        return total > 0 ? (double) totalHits / total : 0D;
    }

    private long getTotalAccessCount() {
        return cacheStatsMap.values().stream().mapToLong(CacheStats::getTotalAccess).sum();
    }

    public CacheStats getCacheStats(String cacheName) {
        return cacheStatsMap.get(cacheName);
    }

    public void analyzeHotspotData() {
        cacheStatsMap.forEach((cacheName, stats) -> {
            Map<String, Long> top5 = stats.getTopAccessedKeys(5);
            if (!top5.isEmpty()) {
                log.debug("Hotspot data for cache {}: {}", cacheName, top5);
            }
        });
    }

    public static class CacheStats {

        private final String cacheName;
        private final AtomicLong hitCount = new AtomicLong(0);
        private final AtomicLong missCount = new AtomicLong(0);
        private final AtomicLong evictionCount = new AtomicLong(0);
        private final AtomicLong totalAccessTime = new AtomicLong(0);
        private final AtomicLong accessCount = new AtomicLong(0);
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
            if (key == null) {
                return;
            }
            keyAccessCount.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }

        public double getHitRatio() {
            long total = hitCount.get() + missCount.get();
            return total > 0 ? (double) hitCount.get() / total : 0D;
        }

        public long getTotalAccess() {
            return hitCount.get() + missCount.get();
        }

        public double getAverageAccessTime() {
            long count = accessCount.get();
            return count > 0 ? (double) totalAccessTime.get() / count : 0D;
        }

        public Map<String, Long> getTopAccessedKeys(int limit) {
            return keyAccessCount.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                    .limit(Math.max(limit, 0))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().get(),
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
        }

        public String getCacheName() {
            return cacheName;
        }

        public AtomicLong getHitCount() {
            return hitCount;
        }

        public AtomicLong getMissCount() {
            return missCount;
        }

        public AtomicLong getEvictionCount() {
            return evictionCount;
        }
    }
}
