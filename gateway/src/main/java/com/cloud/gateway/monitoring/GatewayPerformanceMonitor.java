package com.cloud.gateway.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网关性能监控器
 * 收集和记录网关层面的性能指标
 * 
 * @author what's up
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "gateway.monitoring.performance.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayPerformanceMonitor {

    private final MeterRegistry meterRegistry;
    
    // 请求统计
    private final ConcurrentHashMap<String, RequestStats> pathStats = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong errorRequests = new AtomicLong();
    
    // Micrometer指标
    private final Timer.Builder requestTimerBuilder;
    
    public GatewayPerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestTimerBuilder = Timer.builder("gateway.request.duration")
                .description("网关请求处理时间");
                
        // 注册自定义指标
        meterRegistry.gauge("gateway.request.total", totalRequests, AtomicLong::get);
        meterRegistry.gauge("gateway.request.error", errorRequests, AtomicLong::get);
    }
    
    /**
     * 记录请求性能指标
     */
    public void recordRequest(String path, long responseTimeMs, boolean isError) {
        try {
            // 更新总体统计
            totalRequests.incrementAndGet();
            if (isError) {
                errorRequests.incrementAndGet();
            }
            
            // 更新路径统计
            pathStats.computeIfAbsent(normalizePathForMetrics(path), k -> new RequestStats())
                    .recordRequest(responseTimeMs, isError);
            
            // 记录Micrometer指标
            requestTimerBuilder
                    .tag("path", normalizePathForMetrics(path))
                    .tag("status", isError ? "error" : "success")
                    .register(meterRegistry)
                    .record(Duration.ofMillis(responseTimeMs));
            
            // 性能警报检查
            if (responseTimeMs > 5000) { // 超过5秒的慢请求
                log.warn("检测到慢请求: path={}, responseTime={}ms, error={}", 
                        path, responseTimeMs, isError);
            }
            
        } catch (Exception e) {
            log.error("记录性能指标异常: path={}", path, e);
        }
    }
    
    /**
     * 获取路径性能统计
     */
    public RequestStats getPathStats(String path) {
        return pathStats.get(normalizePathForMetrics(path));
    }
    
    /**
     * 获取总体统计信息
     */
    public OverallStats getOverallStats() {
        long total = totalRequests.get();
        long errors = errorRequests.get();
        double errorRate = total > 0 ? (double) errors / total * 100 : 0;
        
        // 计算平均响应时间
        double avgResponseTime = pathStats.values().stream()
                .mapToDouble(RequestStats::getAverageResponseTime)
                .filter(time -> time > 0)
                .average()
                .orElse(0.0);
        
        return new OverallStats(total, errors, errorRate, avgResponseTime, pathStats.size());
    }
    
    /**
     * 清理过期统计数据
     */
    public void cleanup() {
        long threshold = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        
        pathStats.entrySet().removeIf(entry -> {
            RequestStats stats = entry.getValue();
            return stats.lastUpdateTime < threshold && stats.requestCount < 10;
        });
        
        log.debug("清理性能统计数据，当前路径数: {}", pathStats.size());
    }
    
    /**
     * 重置统计数据
     */
    public void reset() {
        totalRequests.set(0);
        errorRequests.set(0);
        pathStats.clear();
        log.info("重置网关性能统计数据");
    }
    
    /**
     * 规范化路径用于指标收集
     */
    private String normalizePathForMetrics(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        
        // 移除查询参数
        int queryIndex = path.indexOf('?');
        if (queryIndex > 0) {
            path = path.substring(0, queryIndex);
        }
        
        // 替换路径参数为占位符（避免指标爆炸）
        path = path.replaceAll("/\\d+", "/{id}")
                   .replaceAll("/[a-f0-9-]{36}", "/{uuid}")
                   .replaceAll("/[a-f0-9]{32}", "/{hash}");
        
        // 限制路径长度
        if (path.length() > 100) {
            path = path.substring(0, 100) + "...";
        }
        
        return path;
    }
    
    /**
     * 请求统计信息
     */
    public static class RequestStats {
        private volatile long requestCount = 0;
        private volatile long errorCount = 0;
        private volatile long totalResponseTime = 0;
        private volatile long minResponseTime = Long.MAX_VALUE;
        private volatile long maxResponseTime = 0;
        private volatile long lastUpdateTime = System.currentTimeMillis();
        
        public synchronized void recordRequest(long responseTime, boolean isError) {
            requestCount++;
            totalResponseTime += responseTime;
            lastUpdateTime = System.currentTimeMillis();
            
            if (responseTime < minResponseTime) {
                minResponseTime = responseTime;
            }
            if (responseTime > maxResponseTime) {
                maxResponseTime = responseTime;
            }
            
            if (isError) {
                errorCount++;
            }
        }
        
        public long getRequestCount() { return requestCount; }
        public long getErrorCount() { return errorCount; }
        public double getErrorRate() { 
            return requestCount > 0 ? (double) errorCount / requestCount * 100 : 0; 
        }
        public double getAverageResponseTime() { 
            return requestCount > 0 ? (double) totalResponseTime / requestCount : 0; 
        }
        public long getMinResponseTime() { 
            return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime; 
        }
        public long getMaxResponseTime() { return maxResponseTime; }
        public long getLastUpdateTime() { return lastUpdateTime; }
        
        @Override
        public String toString() {
            return String.format("RequestStats{count=%d, errors=%d, errorRate=%.2f%%, avgTime=%.2fms, minTime=%dms, maxTime=%dms}",
                    requestCount, errorCount, getErrorRate(), getAverageResponseTime(), 
                    getMinResponseTime(), maxResponseTime);
        }
    }
    
    /**
     * 总体统计信息
     */
    public static class OverallStats {
        private final long totalRequests;
        private final long totalErrors;
        private final double errorRate;
        private final double averageResponseTime;
        private final int uniquePaths;
        
        public OverallStats(long totalRequests, long totalErrors, double errorRate, 
                           double averageResponseTime, int uniquePaths) {
            this.totalRequests = totalRequests;
            this.totalErrors = totalErrors;
            this.errorRate = errorRate;
            this.averageResponseTime = averageResponseTime;
            this.uniquePaths = uniquePaths;
        }
        
        public long getTotalRequests() { return totalRequests; }
        public long getTotalErrors() { return totalErrors; }
        public double getErrorRate() { return errorRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public int getUniquePaths() { return uniquePaths; }
        
        @Override
        public String toString() {
            return String.format("OverallStats{totalRequests=%d, totalErrors=%d, errorRate=%.2f%%, avgResponseTime=%.2fms, uniquePaths=%d}",
                    totalRequests, totalErrors, errorRate, averageResponseTime, uniquePaths);
        }
    }
}
