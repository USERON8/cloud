package com.cloud.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;







@Slf4j
@Component
@ConditionalOnProperty(name = "performance.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceMonitor implements MeterBinder, HealthIndicator {

    
    private static final double ERROR_RATE_THRESHOLD = 0.05; 
    private static final long RESPONSE_TIME_THRESHOLD = 2000; 
    private static final double MEMORY_USAGE_THRESHOLD = 0.85; 
    private final RedisTemplate<String, Object> redisTemplate;
    
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong errorRequests = new AtomicLong(0);
    private final Map<String, PerformanceMetrics> endpointMetrics = new ConcurrentHashMap<>();
    private final TaskScheduler monitoringScheduler;
    private volatile ScheduledFuture<?> metricsCollectionTask;
    private volatile ScheduledFuture<?> cleanupTask;
    @Autowired(required = false)
    private DataSource dataSource;  
    private MeterRegistry meterRegistry;
    
    private Counter requestCounter;
    private Counter errorCounter;
    private Timer responseTimer;
    private Gauge activeConnectionsGauge;
    private Gauge memoryUsageGauge;
    private Gauge redisConnectionsGauge;

    public PerformanceMonitor(RedisTemplate<String, Object> redisTemplate,
                              @Qualifier("taskScheduler") TaskScheduler monitoringScheduler) {
        this.redisTemplate = redisTemplate;
        this.monitoringScheduler = monitoringScheduler;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        this.meterRegistry = registry;

        
        this.requestCounter = Counter.builder("http.requests.total")
                .description("HTTP璇锋眰鎬绘暟")
                .register(registry);

        this.errorCounter = Counter.builder("http.requests.errors")
                .description("Total HTTP error requests")
                .register(registry);

        this.responseTimer = Timer.builder("http.response.time")
                .description("HTTP鍝嶅簲鏃堕棿")
                .register(registry);

        this.activeConnectionsGauge = Gauge.builder("db.connections.active", this, monitor -> (double) monitor.getActiveConnections())
                .description("娲昏穬鏁版嵁搴撹繛鎺ユ暟")
                .register(registry);

        this.memoryUsageGauge = Gauge.builder("jvm.memory.usage.ratio", this, monitor -> monitor.getMemoryUsageRatio())
                .description("JVM memory usage ratio")
                .register(registry);

        this.redisConnectionsGauge = Gauge.builder("redis.connections.active", this, monitor -> (double) monitor.getRedisConnections())
                .description("Active Redis connections")
                .register(registry);

        
        startMonitoringTasks();

        
    }

    






    public void recordRequest(String endpoint, long responseTime, boolean isError) {
        
        requestCounter.increment();
        responseTimer.record(responseTime, TimeUnit.MILLISECONDS);
        totalRequests.incrementAndGet();

        if (isError) {
            errorCounter.increment();
            errorRequests.incrementAndGet();
        }

        
        PerformanceMetrics metrics = endpointMetrics.computeIfAbsent(endpoint, k -> new PerformanceMetrics());
        metrics.recordRequest(responseTime);

        if (isError) {
            metrics.recordError();
        }

        
        checkAlerts(endpoint, metrics, responseTime);
    }

    




    public void recordDatabaseConnection(boolean increment) {
        if (increment) {
            activeConnections.incrementAndGet();
        } else {
            activeConnections.decrementAndGet();
        }
    }

    




    public Map<String, Object> getPerformanceSnapshot() {
        Map<String, Object> snapshot = new ConcurrentHashMap<>();

        
        snapshot.put("totalRequests", totalRequests.get());
        snapshot.put("errorRequests", errorRequests.get());
        snapshot.put("errorRate", getGlobalErrorRate());
        snapshot.put("activeConnections", activeConnections.get());
        snapshot.put("memoryUsage", getMemoryUsageRatio());
        snapshot.put("redisConnections", getRedisConnections());

        
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

    


    @Override
    public Health health() {
        try {
            Health.Builder builder = new Health.Builder();

            
            boolean dbHealthy = checkDatabaseHealth();

            
            boolean redisHealthy = checkRedisHealth();

            
            double memoryUsage = getMemoryUsageRatio();
            boolean memoryHealthy = memoryUsage < MEMORY_USAGE_THRESHOLD;

            
            double errorRate = getGlobalErrorRate();
            boolean errorRateHealthy = errorRate < ERROR_RATE_THRESHOLD;

            if (dbHealthy && redisHealthy && memoryHealthy && errorRateHealthy) {
                builder.up();
            } else {
                builder.down();
            }

            
            builder.withDetail("database", dbHealthy ? "UP" : "DOWN")
                    .withDetail("redis", redisHealthy ? "UP" : "DOWN")
                    .withDetail("memoryUsage", String.format("%.2f%%", memoryUsage * 100))
                    .withDetail("errorRate", String.format("%.2f%%", errorRate * 100))
                    .withDetail("activeConnections", activeConnections.get())
                    .withDetail("totalRequests", totalRequests.get());

            return builder.build();

        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    


    private synchronized void startMonitoringTasks() {
        if (metricsCollectionTask != null && !metricsCollectionTask.isCancelled()) {
            return;
        }

        metricsCollectionTask = monitoringScheduler.scheduleWithFixedDelay(() -> {
            try {
                recordSystemMetrics();
            } catch (Exception e) {
                log.error("Record system metrics failed", e);
            }
        }, Duration.ofMinutes(1));

        cleanupTask = monitoringScheduler.scheduleWithFixedDelay(() -> {
            try {
                cleanupExpiredMetrics();
            } catch (Exception e) {
                log.error("Cleanup expired metrics failed", e);
            }
        }, Duration.ofMinutes(5));
    }

    @PreDestroy
    public synchronized void stopMonitoringTasks() {
        if (metricsCollectionTask != null) {
            metricsCollectionTask.cancel(true);
            metricsCollectionTask = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel(true);
            cleanupTask = null;
        }
    }

    


    private void recordSystemMetrics() {
        try {
            
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsageRatio = (double) usedMemory / totalMemory;

            if (meterRegistry != null) {
                Gauge.builder("jvm.memory.used", () -> (double) usedMemory)
                        .description("JVM used memory")
                        .baseUnit("bytes")
                        .register(meterRegistry);

                Gauge.builder("jvm.memory.total", () -> (double) totalMemory)
                        .description("JVM total memory")
                        .baseUnit("bytes")
                        .register(meterRegistry);
            }

            log.debug("System metrics collected - memory usage: {:.2f}%, active connections: {}",
                    memoryUsageRatio * 100, activeConnections.get());

        } catch (Exception e) {
            log.error("E messagerror", e);
        }
    }

    


    private void cleanupExpiredMetrics() {
        Instant cutoffTime = Instant.now().minus(Duration.ofHours(1));

        endpointMetrics.entrySet().removeIf(entry -> {
            PerformanceMetrics metrics = entry.getValue();
            return metrics.getLastRequestTime() != null &&
                    metrics.getLastRequestTime().isBefore(cutoffTime);
        });

        log.debug("D messageebug", endpointMetrics.size());
    }

    


    private void checkAlerts(String endpoint, PerformanceMetrics metrics, long responseTime) {
        try {
            
            if (responseTime > RESPONSE_TIME_THRESHOLD) {
                triggerAlert("SLOW_RESPONSE",
                        String.format("绔偣 %s 鍝嶅簲鏃堕棿杩囬暱: %dms", endpoint, responseTime));
            }

            
            if (metrics.getRequestCount() >= 10 && metrics.getErrorRate() > ERROR_RATE_THRESHOLD) {
                triggerAlert("HIGH_ERROR_RATE",
                        String.format("绔偣 %s 閿欒鐜囪繃楂? %.2f%%", endpoint, metrics.getErrorRate() * 100));
            }

            
            double memoryUsage = getMemoryUsageRatio();
            if (memoryUsage > MEMORY_USAGE_THRESHOLD) {
                triggerAlert("HIGH_MEMORY_USAGE",
                        String.format("鍐呭瓨浣跨敤鐜囪繃楂? %.2f%%", memoryUsage * 100));
            }

        } catch (Exception e) {
            log.error("Alert check failed", e);
        }
    }

    


    private void triggerAlert(String alertType, String message) {
        
        log.warn("W messagearn", alertType, message);

        
        try {
            String alertKey = "performance:alerts:" + Instant.now().toString().substring(0, 10);
            Map<String, Object> alert = new ConcurrentHashMap<>();
            alert.put("type", alertType);
            alert.put("message", message);
            alert.put("timestamp", Instant.now().toString());

            redisTemplate.opsForList().leftPush(alertKey, alert);
            redisTemplate.expire(alertKey, Duration.ofDays(7));

        } catch (Exception e) {
            log.error("E messagerror", e);
        }
    }

    


    private boolean checkDatabaseHealth() {
        if (dataSource == null) {
            
            return true;
        }

        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); 
        } catch (SQLException e) {
            log.error("Database health check failed", e);
            return false;
        }
    }

    


    private boolean checkRedisHealth() {
        try {
            redisTemplate.opsForValue().get("health_check");
            return true;
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return false;
        }
    }

    


    private long getActiveConnections() {
        return activeConnections.get();
    }

    


    private double getMemoryUsageRatio() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return (double) usedMemory / totalMemory;
    }

    


    private long getRedisConnections() {
        
        
        return 5;
    }

    


    private double getGlobalErrorRate() {
        long total = totalRequests.get();
        long errors = errorRequests.get();
        return total > 0 ? (double) errors / total : 0.0;
    }

    





    public PerformanceMetrics getEndpointMetrics(String endpoint) {
        return endpointMetrics.get(endpoint);
    }

    


    public void resetMetrics() {
        totalRequests.set(0);
        errorRequests.set(0);
        activeConnections.set(0);
        endpointMetrics.clear();
    }

    


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
