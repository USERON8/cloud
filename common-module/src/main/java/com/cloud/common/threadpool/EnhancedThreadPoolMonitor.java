package com.cloud.common.threadpool;

import com.cloud.common.config.properties.DynamicAsyncProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;








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

    
    private Map<String, ThreadPoolInfo> lastSnapshot = new HashMap<>();

    



    @Scheduled(fixedRateString = "${app.async.common.monitoring-interval-seconds:30}000")
    public void collectMetrics() {
        if (meterRegistry == null) {
            return;
        }

        Map<String, ThreadPoolInfo> currentPools = getAllThreadPoolInfo();

        
        
        























        
        checkStatusChanges(currentPools);
        lastSnapshot = new HashMap<>(currentPools);
    }

    


    @Scheduled(fixedRateString = "${app.async.common.monitoring-interval-seconds:60}000")
    public void logThreadPoolStatus() {
        Map<String, ThreadPoolInfo> allThreadPools = getAllThreadPoolInfo();

        
        allThreadPools.forEach((name, info) -> {
            String statusIcon = getStatusIcon(info.getStatus());
            

        });
        
    }

    


    @Override
    public ThreadPoolHealthStatus checkThreadPoolHealth() {
        ThreadPoolHealthStatus healthStatus = super.checkThreadPoolHealth();

        
        if (asyncProperties != null) {
            DynamicAsyncProperties.CommonConfig common = asyncProperties.getCommon();
            double usageThreshold = common.getAlertThresholdUsageRate();
            double queueThreshold = common.getAlertThresholdQueueRate();

            Map<String, ThreadPoolInfo> allThreadPools = getAllThreadPoolInfo();
            allThreadPools.forEach((name, info) -> {
                if (info.getPoolUsageRate() > usageThreshold) {
                    healthStatus.addWarning(name,
                            String.format("绾跨▼姹犱娇鐢ㄧ巼杩囬珮: %.1f%% > %.1f%%",
                                    info.getPoolUsageRate(), usageThreshold));
                }
                if (info.getQueueUsageRate() > queueThreshold) {
                    healthStatus.addWarning(name,
                            String.format("闃熷垪浣跨敤鐜囪繃楂? %.1f%% > %.1f%%",
                                    info.getQueueUsageRate(), queueThreshold));
                }
            });
        }

        return healthStatus;
    }

    


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

    


    private void checkStatusChanges(Map<String, ThreadPoolInfo> currentPools) {
        currentPools.forEach((name, currentInfo) -> {
            ThreadPoolInfo lastInfo = lastSnapshot.get(name);
            if (lastInfo != null) {
                
                if (!currentInfo.getStatus().equals(lastInfo.getStatus())) {
                    logStatusChange(name, lastInfo.getStatus(), currentInfo.getStatus());
                }

                
                double usageDiff = Math.abs(currentInfo.getPoolUsageRate() - lastInfo.getPoolUsageRate());
                if (usageDiff > 20) { 
                    log.warn("?{} ? {:.1f}% -> {:.1f}%",
                            name, lastInfo.getPoolUsageRate(), currentInfo.getPoolUsageRate());
                }
            }
        });
    }

    


    private void logStatusChange(String poolName, String oldStatus, String newStatus) {
        String oldIcon = getStatusIcon(oldStatus);
        String newIcon = getStatusIcon(newStatus);

        if ("CRITICAL".equals(newStatus)) {
            log.error("E messagerror",
                    poolName, oldIcon, oldStatus, newIcon, newStatus);
        } else if ("WARNING".equals(newStatus)) {
            log.warn("W messagearn",
                    poolName, oldIcon, oldStatus, newIcon, newStatus);
        } else if ("HEALTHY".equals(newStatus) && !"HEALTHY".equals(oldStatus)) {
            

        }
    }

    


    private String getStatusIcon(String status) {
        return switch (status) {
            case "HEALTHY" -> "馃煝";
            case "WARNING" -> "馃煛";
            case "CRITICAL" -> "馃敶";
            default -> "?";
        };
    }

    


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

    


    public static class ThreadPoolPerformanceStats {
        private String poolName;
        private int currentPoolSize;
        private int activeThreadCount;
        private int queueSize;
        private double poolUsageRate;
        private double queueUsageRate;
        private double tasksPerSecond;
        private long timestamp;

        
        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public int getCurrentPoolSize() {
            return currentPoolSize;
        }

        public void setCurrentPoolSize(int currentPoolSize) {
            this.currentPoolSize = currentPoolSize;
        }

        public int getActiveThreadCount() {
            return activeThreadCount;
        }

        public void setActiveThreadCount(int activeThreadCount) {
            this.activeThreadCount = activeThreadCount;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }

        public double getPoolUsageRate() {
            return poolUsageRate;
        }

        public void setPoolUsageRate(double poolUsageRate) {
            this.poolUsageRate = poolUsageRate;
        }

        public double getQueueUsageRate() {
            return queueUsageRate;
        }

        public void setQueueUsageRate(double queueUsageRate) {
            this.queueUsageRate = queueUsageRate;
        }

        public double getTasksPerSecond() {
            return tasksPerSecond;
        }

        public void setTasksPerSecond(double tasksPerSecond) {
            this.tasksPerSecond = tasksPerSecond;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
