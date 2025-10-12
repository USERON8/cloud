package com.cloud.common.threadpool;

import com.cloud.common.config.properties.DynamicAsyncProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 增强型线程池监控工具类
 * 提供线程池状态监控、性能统计、健康检查和指标收集功能
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.async.common.monitoring-enabled", havingValue = "true", matchIfMissing = true)
public class EnhancedThreadPoolMonitor extends ThreadPoolMonitor {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private DynamicAsyncProperties asyncProperties;

    // 缓存上次监控结果，用于变化检测
    private Map<String, ThreadPoolInfo> lastSnapshot = new HashMap<>();

    /**
     * 定时收集线程池指标
     * 注释掉Micrometer相关代码以解决编译问题
     */
    @Scheduled(fixedRateString = "${app.async.common.monitoring-interval-seconds:30}000")
    public void collectMetrics() {
        if (meterRegistry == null) {
            return;
        }

        Map<String, ThreadPoolInfo> currentPools = getAllThreadPoolInfo();

        // TODO: 修复Micrometer API后重新启用
        // Prometheus指标收集 - 使用正确的Micrometer API
        /*
        currentPools.forEach((name, info) -> {
            Gauge.builder("threadpool.active.threads")
                .tags("pool", name, "service", getServiceName(name))
                .register(meterRegistry, () -> info.getActiveThreadCount());

            Gauge.builder("threadpool.queue.size")
                .tags("pool", name, "service", getServiceName(name))
                .register(meterRegistry, () -> info.getQueueSize());

            Gauge.builder("threadpool.usage.rate")
                .tags("pool", name, "service", getServiceName(name))
                .register(meterRegistry, () -> info.getPoolUsageRate());

            Gauge.builder("threadpool.queue.usage.rate")
                .tags("pool", name, "service", getServiceName(name))
                .register(meterRegistry, () -> info.getQueueUsageRate());

            Gauge.builder("threadpool.completed.tasks")
                .tags("pool", name, "service", getServiceName(name))
                .register(meterRegistry, () -> (double) info.getCompletedTaskCount());
        });
        */

        // 检查状态变化并记录
        checkStatusChanges(currentPools);
        lastSnapshot = new HashMap<>(currentPools);
    }

    /**
     * 定时输出线程池状态日志
     */
    @Scheduled(fixedRateString = "${app.async.common.monitoring-interval-seconds:60}000")
    public void logThreadPoolStatus() {
        Map<String, ThreadPoolInfo> allThreadPools = getAllThreadPoolInfo();

        log.info("========== 线程池状态监控 ==========");
        allThreadPools.forEach((name, info) -> {
            String statusIcon = getStatusIcon(info.getStatus());
            log.info("{} 线程池: {} | 状态: {} | 活跃: {}/{} | 队列: {}/{} | 完成: {} | 使用率: {:.1f}%",
                    statusIcon,
                    name,
                    info.getStatus(),
                    info.getActiveThreadCount(),
                    info.getMaximumPoolSize(),
                    info.getQueueSize(),
                    info.getQueueCapacity(),
                    info.getCompletedTaskCount(),
                    info.getPoolUsageRate());
        });
        log.info("=====================================");
    }

    /**
     * 增强的健康检查
     */
    @Override
    public ThreadPoolHealthStatus checkThreadPoolHealth() {
        ThreadPoolHealthStatus healthStatus = super.checkThreadPoolHealth();

        // 添加告警阈值检查
        if (asyncProperties != null) {
            DynamicAsyncProperties.CommonConfig common = asyncProperties.getCommon();
            double usageThreshold = common.getAlertThresholdUsageRate();
            double queueThreshold = common.getAlertThresholdQueueRate();

            Map<String, ThreadPoolInfo> allThreadPools = getAllThreadPoolInfo();
            allThreadPools.forEach((name, info) -> {
                if (info.getPoolUsageRate() > usageThreshold) {
                    healthStatus.addWarning(name,
                        String.format("线程池使用率过高: %.1f%% > %.1f%%",
                            info.getPoolUsageRate(), usageThreshold));
                }
                if (info.getQueueUsageRate() > queueThreshold) {
                    healthStatus.addWarning(name,
                        String.format("队列使用率过高: %.1f%% > %.1f%%",
                            info.getQueueUsageRate(), queueThreshold));
                }
            });
        }

        return healthStatus;
    }

    /**
     * 获取线程池性能统计
     */
    public Map<String, ThreadPoolPerformanceStats> getPerformanceStats() {
        Map<String, ThreadPoolInfo> currentPools = getAllThreadPoolInfo();
        Map<String, ThreadPoolPerformanceStats> statsMap = new HashMap<>();

        currentPools.forEach((name, info) -> {
            ThreadPoolInfo lastInfo = lastSnapshot.get(name);
            ThreadPoolPerformanceStats stats = calculatePerformanceStats(info, lastInfo);
            statsMap.put(name, stats);
        });

        return statsMap;
    }

    /**
     * 检查线程池状态变化
     */
    private void checkStatusChanges(Map<String, ThreadPoolInfo> currentPools) {
        currentPools.forEach((name, currentInfo) -> {
            ThreadPoolInfo lastInfo = lastSnapshot.get(name);
            if (lastInfo != null) {
                // 检查状态变化
                if (!currentInfo.getStatus().equals(lastInfo.getStatus())) {
                    logStatusChange(name, lastInfo.getStatus(), currentInfo.getStatus());
                }

                // 检查使用率变化
                double usageDiff = Math.abs(currentInfo.getPoolUsageRate() - lastInfo.getPoolUsageRate());
                if (usageDiff > 20) { // 使用率变化超过20%
                    log.warn("⚠️ 线程池 {} 使用率大幅变化: {:.1f}% -> {:.1f}%",
                        name, lastInfo.getPoolUsageRate(), currentInfo.getPoolUsageRate());
                }
            }
        });
    }

    /**
     * 记录状态变化
     */
    private void logStatusChange(String poolName, String oldStatus, String newStatus) {
        String oldIcon = getStatusIcon(oldStatus);
        String newIcon = getStatusIcon(newStatus);

        if ("CRITICAL".equals(newStatus)) {
            log.error("🚨 线程池 {} 状态恶化: {} {} -> {} {}",
                poolName, oldIcon, oldStatus, newIcon, newStatus);
        } else if ("WARNING".equals(newStatus)) {
            log.warn("⚠️ 线程池 {} 状态警告: {} {} -> {} {}",
                poolName, oldIcon, oldStatus, newIcon, newStatus);
        } else if ("HEALTHY".equals(newStatus) && !"HEALTHY".equals(oldStatus)) {
            log.info("✅ 线程池 {} 状态恢复: {} {} -> {} {}",
                poolName, oldIcon, oldStatus, newIcon, newStatus);
        }
    }

    /**
     * 获取状态图标
     */
    private String getStatusIcon(String status) {
        return switch (status) {
            case "HEALTHY" -> "🟢";
            case "WARNING" -> "🟡";
            case "CRITICAL" -> "🔴";
            default -> "⚪";
        };
    }

    /**
     * 从线程池名称提取服务名
     */
    private String getServiceName(String poolName) {
        if (poolName.contains("user")) return "user-service";
        if (poolName.contains("order")) return "order-service";
        if (poolName.contains("payment")) return "payment-service";
        if (poolName.contains("product")) return "product-service";
        if (poolName.contains("stock")) return "stock-service";
        if (poolName.contains("search")) return "search-service";
        if (poolName.contains("auth")) return "auth-service";
        return "unknown";
    }

    /**
     * 计算性能统计
     */
    private ThreadPoolPerformanceStats calculatePerformanceStats(ThreadPoolInfo current, ThreadPoolInfo last) {
        ThreadPoolPerformanceStats stats = new ThreadPoolPerformanceStats();
        stats.setPoolName(current.getBeanName());
        stats.setCurrentPoolSize(current.getCurrentPoolSize());
        stats.setActiveThreadCount(current.getActiveThreadCount());
        stats.setQueueSize(current.getQueueSize());
        stats.setPoolUsageRate(current.getPoolUsageRate());
        stats.setQueueUsageRate(current.getQueueUsageRate());

        if (last != null) {
            long timeDiff = System.currentTimeMillis() - last.getTimestamp();
            long taskDiff = current.getCompletedTaskCount() - last.getCompletedTaskCount();

            if (timeDiff > 0) {
                double tasksPerSecond = (double) taskDiff / (timeDiff / 1000.0);
                stats.setTasksPerSecond(tasksPerSecond);
            }
        }

        stats.setTimestamp(System.currentTimeMillis());
        return stats;
    }

    /**
     * 线程池性能统计
     */
    public static class ThreadPoolPerformanceStats {
        private String poolName;
        private int currentPoolSize;
        private int activeThreadCount;
        private int queueSize;
        private double poolUsageRate;
        private double queueUsageRate;
        private double tasksPerSecond;
        private long timestamp;

        // getters and setters
        public String getPoolName() { return poolName; }
        public void setPoolName(String poolName) { this.poolName = poolName; }
        public int getCurrentPoolSize() { return currentPoolSize; }
        public void setCurrentPoolSize(int currentPoolSize) { this.currentPoolSize = currentPoolSize; }
        public int getActiveThreadCount() { return activeThreadCount; }
        public void setActiveThreadCount(int activeThreadCount) { this.activeThreadCount = activeThreadCount; }
        public int getQueueSize() { return queueSize; }
        public void setQueueSize(int queueSize) { this.queueSize = queueSize; }
        public double getPoolUsageRate() { return poolUsageRate; }
        public void setPoolUsageRate(double poolUsageRate) { this.poolUsageRate = poolUsageRate; }
        public double getQueueUsageRate() { return queueUsageRate; }
        public void setQueueUsageRate(double queueUsageRate) { this.queueUsageRate = queueUsageRate; }
        public double getTasksPerSecond() { return tasksPerSecond; }
        public void setTasksPerSecond(double tasksPerSecond) { this.tasksPerSecond = tasksPerSecond; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}