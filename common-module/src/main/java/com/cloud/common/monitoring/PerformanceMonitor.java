package com.cloud.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控组件
 * 提供应用性能指标收集、监控和报警功能
 *
 * @author what's up
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "performance.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceMonitor implements MeterBinder, HealthIndicator {

    // 报警阈值
    private static final double ERROR_RATE_THRESHOLD = 0.05; // 5% 错误率阈值
    private static final long RESPONSE_TIME_THRESHOLD = 2000; // 2秒响应时间阈值
    private static final double MEMORY_USAGE_THRESHOLD = 0.85; // 85% 内存使用率阈值
    private final RedisTemplate<String, Object> redisTemplate;
    // 性能统计
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong errorRequests = new AtomicLong(0);
    private final Map<String, PerformanceMetrics> endpointMetrics = new ConcurrentHashMap<>();
    // 监控任务调度器
    private final ScheduledExecutorService monitoringExecutor = Executors.newScheduledThreadPool(2);
    @Autowired(required = false)
    private DataSource dataSource;  // 可选的数据源依赖
    private MeterRegistry meterRegistry;
    // 性能指标
    private Counter requestCounter;
    private Counter errorCounter;
    private Timer responseTimer;
    private Gauge activeConnectionsGauge;
    private Gauge memoryUsageGauge;
    private Gauge redisConnectionsGauge;

    public PerformanceMonitor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        this.meterRegistry = registry;

        // 注册基础指标
        this.requestCounter = Counter.builder("http.requests.total")
                .description("HTTP请求总数")
                .register(registry);

        this.errorCounter = Counter.builder("http.requests.errors")
                .description("HTTP错误请求数")
                .register(registry);

        this.responseTimer = Timer.builder("http.response.time")
                .description("HTTP响应时间")
                .register(registry);

        this.activeConnectionsGauge = Gauge.builder("db.connections.active", this, monitor -> (double) monitor.getActiveConnections())
                .description("活跃数据库连接数")
                .register(registry);

        this.memoryUsageGauge = Gauge.builder("jvm.memory.usage.ratio", this, monitor -> monitor.getMemoryUsageRatio())
                .description("JVM内存使用率")
                .register(registry);

        this.redisConnectionsGauge = Gauge.builder("redis.connections.active", this, monitor -> (double) monitor.getRedisConnections())
                .description("活跃Redis连接数")
                .register(registry);

        // 启动监控任务
        startMonitoringTasks();

        log.info("性能监控指标已注册到MeterRegistry");
    }

    /**
     * 记录HTTP请求指标
     *
     * @param endpoint     端点
     * @param responseTime 响应时间
     * @param isError      是否为错误请求
     */
    public void recordRequest(String endpoint, long responseTime, boolean isError) {
        // 更新全局计数器
        requestCounter.increment();
        responseTimer.record(responseTime, TimeUnit.MILLISECONDS);
        totalRequests.incrementAndGet();

        if (isError) {
            errorCounter.increment();
            errorRequests.incrementAndGet();
        }

        // 更新端点级别指标
        PerformanceMetrics metrics = endpointMetrics.computeIfAbsent(endpoint, k -> new PerformanceMetrics());
        metrics.recordRequest(responseTime);

        if (isError) {
            metrics.recordError();
        }

        // 检查是否需要报警
        checkAlerts(endpoint, metrics, responseTime);
    }

    /**
     * 记录数据库连接使用
     *
     * @param increment 是否为增加连接
     */
    public void recordDatabaseConnection(boolean increment) {
        if (increment) {
            activeConnections.incrementAndGet();
        } else {
            activeConnections.decrementAndGet();
        }
    }

    /**
     * 获取当前性能快照
     *
     * @return 性能快照
     */
    public Map<String, Object> getPerformanceSnapshot() {
        Map<String, Object> snapshot = new ConcurrentHashMap<>();

        // 基础指标
        snapshot.put("totalRequests", totalRequests.get());
        snapshot.put("errorRequests", errorRequests.get());
        snapshot.put("errorRate", getGlobalErrorRate());
        snapshot.put("activeConnections", activeConnections.get());
        snapshot.put("memoryUsage", getMemoryUsageRatio());
        snapshot.put("redisConnections", getRedisConnections());

        // 端点指标
        Map<String, Map<String, Object>> endpointStats = new ConcurrentHashMap<>();
        endpointMetrics.forEach((endpoint, metrics) -> {
            Map<String, Object> stats = new ConcurrentHashMap<>();
            stats.put("requestCount", metrics.getRequestCount());
            stats.put("errorCount", metrics.getErrorCount());
            stats.put("errorRate", metrics.getErrorRate());
            stats.put("avgResponseTime", metrics.getAverageResponseTime());
            stats.put("maxResponseTime", metrics.getMaxResponseTime());
            stats.put("minResponseTime", metrics.getMinResponseTime());
            stats.put("lastRequestTime", metrics.getLastRequestTime());
            endpointStats.put(endpoint, stats);
        });
        snapshot.put("endpoints", endpointStats);

        return snapshot;
    }

    /**
     * 获取系统健康状态
     */
    @Override
    public Health health() {
        try {
            Health.Builder builder = new Health.Builder();

            // 检查数据库连接
            boolean dbHealthy = checkDatabaseHealth();

            // 检查Redis连接
            boolean redisHealthy = checkRedisHealth();

            // 检查内存使用率
            double memoryUsage = getMemoryUsageRatio();
            boolean memoryHealthy = memoryUsage < MEMORY_USAGE_THRESHOLD;

            // 检查错误率
            double errorRate = getGlobalErrorRate();
            boolean errorRateHealthy = errorRate < ERROR_RATE_THRESHOLD;

            if (dbHealthy && redisHealthy && memoryHealthy && errorRateHealthy) {
                builder.up();
            } else {
                builder.down();
            }

            // 添加详细信息
            builder.withDetail("database", dbHealthy ? "UP" : "DOWN")
                    .withDetail("redis", redisHealthy ? "UP" : "DOWN")
                    .withDetail("memoryUsage", String.format("%.2f%%", memoryUsage * 100))
                    .withDetail("errorRate", String.format("%.2f%%", errorRate * 100))
                    .withDetail("activeConnections", activeConnections.get())
                    .withDetail("totalRequests", totalRequests.get());

            return builder.build();

        } catch (Exception e) {
            log.error("健康检查异常", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * 启动监控任务
     */
    private void startMonitoringTasks() {
        // 每分钟记录性能指标
        monitoringExecutor.scheduleWithFixedDelay(() -> {
            try {
                recordSystemMetrics();
            } catch (Exception e) {
                log.error("记录系统指标异常", e);
            }
        }, 1, 1, TimeUnit.MINUTES);

        // 每5分钟清理过期指标
        monitoringExecutor.scheduleWithFixedDelay(() -> {
            try {
                cleanupExpiredMetrics();
            } catch (Exception e) {
                log.error("清理过期指标异常", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * 记录系统指标
     */
    private void recordSystemMetrics() {
        try {
            // 记录JVM内存使用情况
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsageRatio = (double) usedMemory / totalMemory;

            if (meterRegistry != null) {
                Gauge.builder("jvm.memory.used", () -> (double) usedMemory)
                        .description("JVM已使用内存")
                        .baseUnit("bytes")
                        .register(meterRegistry);

                Gauge.builder("jvm.memory.total", () -> (double) totalMemory)
                        .description("JVM总内存")
                        .baseUnit("bytes")
                        .register(meterRegistry);
            }

            log.debug("系统指标记录完成 - 内存使用率: {:.2f}%, 活跃连接: {}",
                    memoryUsageRatio * 100, activeConnections.get());

        } catch (Exception e) {
            log.error("记录系统指标异常", e);
        }
    }

    /**
     * 清理过期指标
     */
    private void cleanupExpiredMetrics() {
        Instant cutoffTime = Instant.now().minus(Duration.ofHours(1));

        endpointMetrics.entrySet().removeIf(entry -> {
            PerformanceMetrics metrics = entry.getValue();
            return metrics.getLastRequestTime() != null &&
                    metrics.getLastRequestTime().isBefore(cutoffTime);
        });

        log.debug("清理过期指标完成，当前端点数量: {}", endpointMetrics.size());
    }

    /**
     * 检查报警条件
     */
    private void checkAlerts(String endpoint, PerformanceMetrics metrics, long responseTime) {
        try {
            // 响应时间报警
            if (responseTime > RESPONSE_TIME_THRESHOLD) {
                triggerAlert("SLOW_RESPONSE",
                        String.format("端点 %s 响应时间过长: %dms", endpoint, responseTime));
            }

            // 错误率报警
            if (metrics.getRequestCount() >= 10 && metrics.getErrorRate() > ERROR_RATE_THRESHOLD) {
                triggerAlert("HIGH_ERROR_RATE",
                        String.format("端点 %s 错误率过高: %.2f%%", endpoint, metrics.getErrorRate() * 100));
            }

            // 内存使用率报警
            double memoryUsage = getMemoryUsageRatio();
            if (memoryUsage > MEMORY_USAGE_THRESHOLD) {
                triggerAlert("HIGH_MEMORY_USAGE",
                        String.format("内存使用率过高: %.2f%%", memoryUsage * 100));
            }

        } catch (Exception e) {
            log.error("报警检查异常", e);
        }
    }

    /**
     * 触发报警
     */
    private void triggerAlert(String alertType, String message) {
        // 这里可以集成实际的报警系统，如邮件、短信、钉钉等
        log.warn("性能报警 - {}: {}", alertType, message);

        // 记录报警到Redis（可选）
        try {
            String alertKey = "performance:alerts:" + Instant.now().toString().substring(0, 10);
            Map<String, Object> alert = new ConcurrentHashMap<>();
            alert.put("type", alertType);
            alert.put("message", message);
            alert.put("timestamp", Instant.now().toString());

            redisTemplate.opsForList().leftPush(alertKey, alert);
            redisTemplate.expire(alertKey, Duration.ofDays(7));

        } catch (Exception e) {
            log.error("记录报警信息失败", e);
        }
    }

    /**
     * 检查数据库健康状态
     */
    private boolean checkDatabaseHealth() {
        if (dataSource == null) {
            // 没有数据源配置时，跳过数据库健康检查
            return true;
        }

        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5秒超时
        } catch (SQLException e) {
            log.error("数据库健康检查失败", e);
            return false;
        }
    }

    /**
     * 检查Redis健康状态
     */
    private boolean checkRedisHealth() {
        try {
            redisTemplate.opsForValue().get("health_check");
            return true;
        } catch (Exception e) {
            log.error("Redis健康检查失败", e);
            return false;
        }
    }

    /**
     * 获取活跃连接数
     */
    private long getActiveConnections() {
        return activeConnections.get();
    }

    /**
     * 获取内存使用率
     */
    private double getMemoryUsageRatio() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return (double) usedMemory / totalMemory;
    }

    /**
     * 获取Redis连接数（简化实现）
     */
    private long getRedisConnections() {
        // 这里应该获取实际的Redis连接池信息
        // 简化实现，返回固定值
        return 5;
    }

    /**
     * 获取全局错误率
     */
    private double getGlobalErrorRate() {
        long total = totalRequests.get();
        long errors = errorRequests.get();
        return total > 0 ? (double) errors / total : 0.0;
    }

    /**
     * 获取端点性能指标
     *
     * @param endpoint 端点
     * @return 性能指标
     */
    public PerformanceMetrics getEndpointMetrics(String endpoint) {
        return endpointMetrics.get(endpoint);
    }

    /**
     * 重置性能指标
     */
    public void resetMetrics() {
        totalRequests.set(0);
        errorRequests.set(0);
        activeConnections.set(0);
        endpointMetrics.clear();
        log.info("性能指标已重置");
    }

    /**
     * 性能指标数据
     */
    public static class PerformanceMetrics {
        private long requestCount;
        private long errorCount;
        private long totalResponseTime;
        private long maxResponseTime;
        private long minResponseTime = Long.MAX_VALUE;
        private Instant lastRequestTime;

        public void recordRequest(long responseTime) {
            requestCount++;
            totalResponseTime += responseTime;
            maxResponseTime = Math.max(maxResponseTime, responseTime);
            minResponseTime = Math.min(minResponseTime, responseTime);
            lastRequestTime = Instant.now();
        }

        public void recordError() {
            errorCount++;
        }

        public double getErrorRate() {
            return requestCount > 0 ? (double) errorCount / requestCount : 0.0;
        }

        public double getAverageResponseTime() {
            return requestCount > 0 ? (double) totalResponseTime / requestCount : 0.0;
        }

        // Getters
        public long getRequestCount() {
            return requestCount;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public long getMaxResponseTime() {
            return maxResponseTime;
        }

        public long getMinResponseTime() {
            return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime;
        }

        public Instant getLastRequestTime() {
            return lastRequestTime;
        }
    }
}
