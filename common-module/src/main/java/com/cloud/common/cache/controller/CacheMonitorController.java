package com.cloud.common.cache.controller;

import com.cloud.common.cache.core.MultiLevelCacheManager;
import com.cloud.common.cache.metrics.CacheMetricsCollector;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * 缓存监控控制器
 * <p>
 * 提供缓存监控和管理接口:
 * - 查看缓存统计信息
 * - 查看热点数据
 * - 手动清理缓存
 * - 查看缓存配置
 *
 * @author CloudDevAgent
 * @since 2025-10-12
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/monitor")
@Tag(name = "缓存监控", description = "缓存统计和管理接口")
@ConditionalOnBean(CacheManager.class)
public class CacheMonitorController {

    private final CacheManager cacheManager;
    private final CacheMetricsCollector metricsCollector;

    public CacheMonitorController(CacheManager cacheManager, @Nullable CacheMetricsCollector metricsCollector) {
        this.cacheManager = cacheManager;
        this.metricsCollector = metricsCollector;
    }

    /**
     * 获取所有缓存名称
     */
    @GetMapping("/names")
    @Operation(summary = "获取所有缓存名称")
    public Result<Collection<String>> getCacheNames() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        return Result.success(cacheNames);
    }

    /**
     * 获取指定缓存的统计信息
     */
    @GetMapping("/stats/{cacheName}")
    @Operation(summary = "获取指定缓存的统计信息")
    public Result<Map<String, Object>> getCacheStats(@PathVariable String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                return Result.error("缓存 " + cacheName + " 不存在");
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("cacheName", cacheName);
            stats.put("cacheType", cache.getClass().getSimpleName());

            // 如果是多级缓存,获取详细统计
            if (cache instanceof com.cloud.common.cache.core.MultiLevelCache multiCache) {
                stats.put("stats", multiCache.getStats());
            }

            // 从MetricsCollector获取统计信息
            if (metricsCollector != null) {
                CacheMetricsCollector.CacheStats cacheStats =
                        metricsCollector.getCacheStats(cacheName);
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
            log.error("获取缓存统计失败: {}", cacheName, e);
            return Result.error("获取缓存统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有缓存的统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取所有缓存的统计信息")
    public Result<List<Map<String, Object>>> getAllCacheStats() {
        try {
            List<Map<String, Object>> statsList = new ArrayList<>();

            for (String cacheName : cacheManager.getCacheNames()) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("cacheName", cacheName);

                // 从MetricsCollector获取统计信息
                if (metricsCollector != null) {
                    CacheMetricsCollector.CacheStats cacheStats =
                            metricsCollector.getCacheStats(cacheName);
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
            log.error("获取所有缓存统计失败", e);
            return Result.error("获取所有缓存统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取热点数据分析
     */
    @GetMapping("/hotspot/{cacheName}")
    @Operation(summary = "获取指定缓存的热点数据")
    public Result<Map<String, Long>> getHotspotData(
            @PathVariable String cacheName,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            if (metricsCollector == null) {
                return Result.error("指标收集器未启用");
            }

            CacheMetricsCollector.CacheStats cacheStats =
                    metricsCollector.getCacheStats(cacheName);

            if (cacheStats == null) {
                return Result.error("缓存 " + cacheName + " 的统计信息不存在");
            }

            Map<String, Long> hotspotData = cacheStats.getTopAccessedKeys(limit);
            return Result.success(hotspotData);

        } catch (Exception e) {
            log.error("获取热点数据失败: {}", cacheName, e);
            return Result.error("获取热点数据失败: " + e.getMessage());
        }
    }

    /**
     * 清除指定缓存
     */
    @DeleteMapping("/clear/{cacheName}")
    @Operation(summary = "清除指定缓存")
    public Result<String> clearCache(@PathVariable String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                return Result.error("缓存 " + cacheName + " 不存在");
            }

            cache.clear();
            log.info("手动清除缓存: {}", cacheName);
            return Result.success("缓存 " + cacheName + " 已清除");

        } catch (Exception e) {
            log.error("清除缓存失败: {}", cacheName, e);
            return Result.error("清除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 清除所有缓存
     */
    @DeleteMapping("/clear-all")
    @Operation(summary = "清除所有缓存")
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

            log.info("手动清除所有缓存, 总数: {}", count);
            return Result.success("已清除 " + count + " 个缓存");

        } catch (Exception e) {
            log.error("清除所有缓存失败", e);
            return Result.error("清除所有缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存管理器信息
     */
    @GetMapping("/manager-info")
    @Operation(summary = "获取缓存管理器信息")
    public Result<Map<String, Object>> getCacheManagerInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("managerType", cacheManager.getClass().getSimpleName());
            info.put("cacheCount", cacheManager.getCacheNames().size());
            info.put("cacheNames", cacheManager.getCacheNames());

            // 如果是多级缓存管理器,获取额外信息
            if (cacheManager instanceof MultiLevelCacheManager multiManager) {
                info.put("nodeId", multiManager.getNodeId());
                info.put("cacheConfig", multiManager.getCacheConfig());
            }

            return Result.success(info);

        } catch (Exception e) {
            log.error("获取缓存管理器信息失败", e);
            return Result.error("获取缓存管理器信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统总体缓存指标
     */
    @GetMapping("/metrics/summary")
    @Operation(summary = "获取系统总体缓存指标")
    public Result<Map<String, Object>> getCacheMetricsSummary() {
        try {
            if (metricsCollector == null) {
                return Result.error("指标收集器未启用");
            }

            Map<String, Object> summary = new HashMap<>();

            long totalHits = 0;
            long totalMisses = 0;
            double totalAccessTime = 0;
            int cacheCount = 0;

            for (String cacheName : cacheManager.getCacheNames()) {
                CacheMetricsCollector.CacheStats stats =
                        metricsCollector.getCacheStats(cacheName);

                if (stats != null) {
                    totalHits += stats.getHitCount().get();
                    totalMisses += stats.getMissCount().get();
                    totalAccessTime += stats.getAverageAccessTime();
                    cacheCount++;
                }
            }

            long totalAccess = totalHits + totalMisses;
            double overallHitRatio = totalAccess > 0 ? (double) totalHits / totalAccess : 0.0;
            double avgAccessTime = cacheCount > 0 ? totalAccessTime / cacheCount : 0.0;

            summary.put("totalHits", totalHits);
            summary.put("totalMisses", totalMisses);
            summary.put("totalAccess", totalAccess);
            summary.put("overallHitRatio", String.format("%.2f%%", overallHitRatio * 100));
            summary.put("averageAccessTime", String.format("%.2fms", avgAccessTime));
            summary.put("cacheCount", cacheCount);

            return Result.success(summary);

        } catch (Exception e) {
            log.error("获取缓存指标汇总失败", e);
            return Result.error("获取缓存指标汇总失败: " + e.getMessage());
        }
    }
}
