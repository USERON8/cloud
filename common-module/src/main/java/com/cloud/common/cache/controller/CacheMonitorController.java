package com.cloud.common.cache.controller;

import com.cloud.common.cache.core.MultiLevelCache;
import com.cloud.common.cache.core.MultiLevelCacheManager;
import com.cloud.common.cache.metrics.CacheMetricsCollector;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cache/monitor")
@Tag(name = "Cache Monitor", description = "Cache monitoring endpoints")
@ConditionalOnBean(CacheManager.class)
public class CacheMonitorController {

    private final CacheManager cacheManager;
    private final CacheMetricsCollector metricsCollector;

    public CacheMonitorController(CacheManager cacheManager, @Nullable CacheMetricsCollector metricsCollector) {
        this.cacheManager = cacheManager;
        this.metricsCollector = metricsCollector;
    }

    @GetMapping("/names")
    @Operation(summary = "Get cache names")
    public Result<Collection<String>> getCacheNames() {
        return Result.success(cacheManager.getCacheNames());
    }

    @GetMapping("/stats/{cacheName}")
    @Operation(summary = "Get cache stats")
    public Result<Map<String, Object>> getCacheStats(@PathVariable String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                return Result.error("Cache not found: " + cacheName);
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("cacheName", cacheName);
            stats.put("cacheType", cache.getClass().getSimpleName());

            if (cache instanceof MultiLevelCache multiLevelCache) {
                stats.put("stats", multiLevelCache.getStats());
            }

            if (metricsCollector != null) {
                CacheMetricsCollector.CacheStats cacheStats = metricsCollector.getCacheStats(cacheName);
                if (cacheStats != null) {
                    stats.put("hitCount", cacheStats.getHitCount().get());
                    stats.put("missCount", cacheStats.getMissCount().get());
                    stats.put("evictionCount", cacheStats.getEvictionCount().get());
                    stats.put("totalAccess", cacheStats.getTotalAccess());
                    stats.put("hitRatio", String.format("%.2f%%", cacheStats.getHitRatio() * 100));
                    stats.put("averageAccessTime", String.format("%.2fms", cacheStats.getAverageAccessTime()));
                }
            }

            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get cache stats: {}", cacheName, e);
            return Result.error("Failed to get cache stats: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get all cache stats")
    public Result<List<Map<String, Object>>> getAllCacheStats() {
        try {
            List<Map<String, Object>> statsList = new ArrayList<>();

            for (String cacheName : cacheManager.getCacheNames()) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("cacheName", cacheName);

                if (metricsCollector != null) {
                    CacheMetricsCollector.CacheStats cacheStats = metricsCollector.getCacheStats(cacheName);
                    if (cacheStats != null) {
                        stats.put("hitCount", cacheStats.getHitCount().get());
                        stats.put("missCount", cacheStats.getMissCount().get());
                        stats.put("totalAccess", cacheStats.getTotalAccess());
                        stats.put("hitRatio", String.format("%.2f%%", cacheStats.getHitRatio() * 100));
                        stats.put("averageAccessTime", String.format("%.2fms", cacheStats.getAverageAccessTime()));
                    }
                }

                statsList.add(stats);
            }

            return Result.success(statsList);
        } catch (Exception e) {
            log.error("Failed to get all cache stats", e);
            return Result.error("Failed to get all cache stats: " + e.getMessage());
        }
    }

    @GetMapping("/hotspot/{cacheName}")
    @Operation(summary = "Get cache hotspot keys")
    public Result<Map<String, Long>> getHotspotData(@PathVariable String cacheName,
                                                    @RequestParam(defaultValue = "10") int limit) {
        try {
            if (metricsCollector == null) {
                return Result.error("Cache metrics collector is not available");
            }

            CacheMetricsCollector.CacheStats cacheStats = metricsCollector.getCacheStats(cacheName);
            if (cacheStats == null) {
                return Result.error("No cache stats found for: " + cacheName);
            }

            return Result.success(cacheStats.getTopAccessedKeys(limit));
        } catch (Exception e) {
            log.error("Failed to get hotspot data: {}", cacheName, e);
            return Result.error("Failed to get hotspot data: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear/{cacheName}")
    @Operation(summary = "Clear cache")
    public Result<String> clearCache(@PathVariable String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                return Result.error("Cache not found: " + cacheName);
            }

            cache.clear();
            return Result.success("Cache cleared: " + cacheName);
        } catch (Exception e) {
            log.error("Failed to clear cache: {}", cacheName, e);
            return Result.error("Failed to clear cache: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear-all")
    @Operation(summary = "Clear all caches")
    public Result<String> clearAllCaches() {
        try {
            int count = 0;
            for (String cacheName : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    count++;
                }
            }

            return Result.success("Cleared caches: " + count);
        } catch (Exception e) {
            log.error("Failed to clear all caches", e);
            return Result.error("Failed to clear all caches: " + e.getMessage());
        }
    }

    @GetMapping("/manager-info")
    @Operation(summary = "Get cache manager info")
    public Result<Map<String, Object>> getCacheManagerInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("managerType", cacheManager.getClass().getSimpleName());
            info.put("cacheCount", cacheManager.getCacheNames().size());
            info.put("cacheNames", cacheManager.getCacheNames());

            if (cacheManager instanceof MultiLevelCacheManager multiLevelCacheManager) {
                info.put("nodeId", multiLevelCacheManager.getNodeId());
                info.put("cacheConfig", multiLevelCacheManager.getCacheConfig());
            }

            return Result.success(info);
        } catch (Exception e) {
            log.error("Failed to get cache manager info", e);
            return Result.error("Failed to get cache manager info: " + e.getMessage());
        }
    }

    @GetMapping("/metrics/summary")
    @Operation(summary = "Get cache metrics summary")
    public Result<Map<String, Object>> getCacheMetricsSummary() {
        try {
            if (metricsCollector == null) {
                return Result.error("Cache metrics collector is not available");
            }

            long totalHits = 0L;
            long totalMisses = 0L;
            long totalEvictions = 0L;
            double totalAccessTime = 0D;
            int cacheCount = 0;

            for (String cacheName : cacheManager.getCacheNames()) {
                CacheMetricsCollector.CacheStats stats = metricsCollector.getCacheStats(cacheName);
                if (stats != null) {
                    totalHits += stats.getHitCount().get();
                    totalMisses += stats.getMissCount().get();
                    totalEvictions += stats.getEvictionCount().get();
                    totalAccessTime += stats.getAverageAccessTime();
                    cacheCount++;
                }
            }

            long totalAccess = totalHits + totalMisses;
            double hitRatio = totalAccess > 0 ? (double) totalHits / totalAccess : 0D;
            double avgAccessTime = cacheCount > 0 ? totalAccessTime / cacheCount : 0D;

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalHits", totalHits);
            summary.put("totalMisses", totalMisses);
            summary.put("totalEvictions", totalEvictions);
            summary.put("totalAccess", totalAccess);
            summary.put("overallHitRatio", String.format("%.2f%%", hitRatio * 100));
            summary.put("averageAccessTime", String.format("%.2fms", avgAccessTime));
            summary.put("cacheCount", cacheCount);

            return Result.success(summary);
        } catch (Exception e) {
            log.error("Failed to get cache metrics summary", e);
            return Result.error("Failed to get cache metrics summary: " + e.getMessage());
        }
    }
}
