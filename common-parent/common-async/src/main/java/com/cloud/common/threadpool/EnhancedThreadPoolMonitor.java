package com.cloud.common.threadpool;

import com.cloud.common.config.properties.DynamicAsyncProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
    name = "app.async.common.monitoring-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class EnhancedThreadPoolMonitor extends ThreadPoolMonitor {

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

    allThreadPools.forEach(
        (name, info) -> {
          String statusIcon = getStatusIcon(info.getStatus());
          String poolUsageFormatted = String.format("%.1f", info.getPoolUsageRate());
          String queueUsageFormatted = String.format("%.1f", info.getQueueUsageRate());
          log.debug(
              "线程池状态: {} {} poolUsage={}% queueUsage={}%",
              name, statusIcon, poolUsageFormatted, queueUsageFormatted);
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
      allThreadPools.forEach(
          (name, info) -> {
            if (info.getPoolUsageRate() > usageThreshold) {
              healthStatus.addWarning(
                  name,
                  String.format(
                      "线程池使用率过高: %.1f%% > %.1f%%", info.getPoolUsageRate(), usageThreshold));
            }
            if (info.getQueueUsageRate() > queueThreshold) {
              healthStatus.addWarning(
                  name,
                  String.format(
                      "线程池队列使用率过高: %.1f%% > %.1f%%", info.getQueueUsageRate(), queueThreshold));
            }
          });
    }

    return healthStatus;
  }

  public Map<String, ThreadPoolPerformanceStats> getPerformanceStats() {
    Map<String, ThreadPoolInfo> currentPools = getAllThreadPoolInfo();
    Map<String, ThreadPoolPerformanceStats> statsMap = new HashMap<>();

    currentPools.forEach(
        (name, info) -> {
          ThreadPoolInfo lastInfo = lastSnapshot.get(name);
          ThreadPoolPerformanceStats stats = calculatePerformanceStats(info, lastInfo);
          statsMap.put(name, stats);
        });

    return statsMap;
  }

  private void checkStatusChanges(Map<String, ThreadPoolInfo> currentPools) {
    currentPools.forEach(
        (name, currentInfo) -> {
          ThreadPoolInfo lastInfo = lastSnapshot.get(name);
          if (lastInfo != null) {
            if (!currentInfo.getStatus().equals(lastInfo.getStatus())) {
              logStatusChange(name, lastInfo.getStatus(), currentInfo.getStatus());
            }

            double usageDiff =
                Math.abs(currentInfo.getPoolUsageRate() - lastInfo.getPoolUsageRate());
            if (usageDiff > 20) {
              String lastUsage = String.format("%.1f", lastInfo.getPoolUsageRate());
              String currentUsage = String.format("%.1f", currentInfo.getPoolUsageRate());
              log.warn("线程池使用率变化较大: {} {}% -> {}%", name, lastUsage, currentUsage);
            }
          }
        });
  }

  private void logStatusChange(String poolName, String oldStatus, String newStatus) {
    String oldIcon = getStatusIcon(oldStatus);
    String newIcon = getStatusIcon(newStatus);

    if ("CRITICAL".equals(newStatus)) {
      log.error("线程池状态变更: {} {}({}) -> {}({})", poolName, oldIcon, oldStatus, newIcon, newStatus);
    } else if ("WARNING".equals(newStatus)) {
      log.warn("线程池状态变更: {} {}({}) -> {}({})", poolName, oldIcon, oldStatus, newIcon, newStatus);
    } else if ("HEALTHY".equals(newStatus) && !"HEALTHY".equals(oldStatus)) {
      log.info("线程池状态恢复: {} {}({}) -> {}({})", poolName, oldIcon, oldStatus, newIcon, newStatus);
    }
  }

  private String getStatusIcon(String status) {
    return switch (status) {
      case "HEALTHY" -> "OK";
      case "WARNING" -> "WARN";
      case "CRITICAL" -> "CRIT";
      default -> "UNKNOWN";
    };
  }

  private ThreadPoolPerformanceStats calculatePerformanceStats(
      ThreadPoolInfo current, ThreadPoolInfo last) {
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

  @Setter
  @Getter
  public static class ThreadPoolPerformanceStats {
    private String poolName;
    private int currentPoolSize;
    private int activeThreadCount;
    private int queueSize;
    private double poolUsageRate;
    private double queueUsageRate;
    private double tasksPerSecond;
    private long timestamp;
  }
}
