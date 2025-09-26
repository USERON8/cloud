package com.cloud.common.cache;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存性能度量与报告
 * - 记录不同 Key 在 STRING/HASH 两种存储下的耗时与成功率
 * - 用于 HybridCacheManager 自动模式选择
 */
@Slf4j
@Component
public class CachePerformanceMetrics {

    private final Map<String, Stats> statsByKey = new ConcurrentHashMap<>();

    public void recordSetOperation(String key, HybridCacheManager.CacheStorageType type, long durationMs, boolean success) {
        if (type == null) type = HybridCacheManager.CacheStorageType.STRING;
        statsByKey.computeIfAbsent(key, k -> new Stats()).record(type, durationMs, success, true);
    }

    public void recordGetOperation(String key, HybridCacheManager.CacheStorageType type, long durationMs, boolean success) {
        if (type == null) type = HybridCacheManager.CacheStorageType.STRING;
        statsByKey.computeIfAbsent(key, k -> new Stats()).record(type, durationMs, success, false);
    }

    /**
     * 根据历史统计为某个 key 给出更优的存储类型（简单策略）
     */
    public HybridCacheManager.CacheStorageType getBestPerformingType(String key) {
        Stats s = statsByKey.get(key);
        if (s == null) return HybridCacheManager.CacheStorageType.STRING;
        double stringScore = s.score(HybridCacheManager.CacheStorageType.STRING);
        double hashScore = s.score(HybridCacheManager.CacheStorageType.HASH);
        return hashScore > stringScore ? HybridCacheManager.CacheStorageType.HASH : HybridCacheManager.CacheStorageType.STRING;
    }

    /**
     * 生成性能报告
     */
    public CachePerformanceReport generateReport() {
        CachePerformanceReport report = new CachePerformanceReport();
        for (Map.Entry<String, Stats> e : statsByKey.entrySet()) {
            report.getEntries().add(reportEntry(e.getKey(), e.getValue()));
        }
        report.setGeneratedAt(Instant.now().toString());
        return report;
    }

    private CachePerformanceReport.Entry reportEntry(String key, Stats s) {
        CachePerformanceReport.Entry entry = new CachePerformanceReport.Entry();
        entry.setKey(key);
        entry.setStringAvgMs(s.avgMs(HybridCacheManager.CacheStorageType.STRING));
        entry.setStringSuccessRate(s.successRate(HybridCacheManager.CacheStorageType.STRING));
        entry.setHashAvgMs(s.avgMs(HybridCacheManager.CacheStorageType.HASH));
        entry.setHashSuccessRate(s.successRate(HybridCacheManager.CacheStorageType.HASH));
        entry.setRecommended(getBestPerformingType(key).name());
        return entry;
    }

    /** 内部统计结构 */
    private static class Stats {
        private final Map<HybridCacheManager.CacheStorageType, Bucket> buckets = new EnumMap<>(HybridCacheManager.CacheStorageType.class);
        Stats() {
            buckets.put(HybridCacheManager.CacheStorageType.STRING, new Bucket());
            buckets.put(HybridCacheManager.CacheStorageType.HASH, new Bucket());
        }
        void record(HybridCacheManager.CacheStorageType type, long durationMs, boolean success, boolean write) {
            buckets.get(type).record(durationMs, success, write);
        }
        double avgMs(HybridCacheManager.CacheStorageType type) { return buckets.get(type).avgMs(); }
        double successRate(HybridCacheManager.CacheStorageType type) { return buckets.get(type).successRate(); }
        double score(HybridCacheManager.CacheStorageType type) {
            Bucket b = buckets.get(type);
            // 简单评分：成功率 * 1000 / (1 + 平均耗时)
            return b.successRate() * 1000.0 / (1.0 + b.avgMs());
        }
        static class Bucket {
            long count;
            long success;
            long totalMs;
            long writeCount;
            void record(long ms, boolean ok, boolean write) {
                count++; totalMs += Math.max(0, ms); if (ok) success++; if (write) writeCount++; }
            double avgMs() { return count == 0 ? 0.0 : (double) totalMs / count; }
            double successRate() { return count == 0 ? 0.0 : (double) success / count; }
        }
    }

    /** 报告模型 */
    @Data
    public static class CachePerformanceReport {
        private String generatedAt;
        private List<Entry> entries = new ArrayList<>();
        @Data
        public static class Entry {
            private String key;
            private double stringAvgMs;
            private double stringSuccessRate;
            private double hashAvgMs;
            private double hashSuccessRate;
            private String recommended;
        }
    }
}

