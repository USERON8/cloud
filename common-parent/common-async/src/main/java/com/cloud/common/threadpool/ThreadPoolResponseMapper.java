package com.cloud.common.threadpool;

import com.cloud.common.domain.vo.governance.ThreadPoolMetricsVO;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ThreadPoolResponseMapper {

  private ThreadPoolResponseMapper() {}

  public static Map<String, Object> toResponse(ThreadPoolInfo info) {
    return toResponse(
        info.getBeanName(),
        info.getCorePoolSize(),
        info.getMaximumPoolSize(),
        info.getActiveThreadCount(),
        info.getCurrentPoolSize(),
        info.getQueueSize(),
        info.getCompletedTaskCount(),
        info.getTotalTaskCount(),
        Math.max(info.getQueueCapacity() - info.getQueueSize(), 0));
  }

  public static ThreadPoolMetricsVO toMetrics(ThreadPoolInfo info) {
    ThreadPoolMetricsVO metrics = new ThreadPoolMetricsVO();
    metrics.setName(info.getBeanName());
    metrics.setCorePoolSize(info.getCorePoolSize());
    metrics.setMaxPoolSize(info.getMaximumPoolSize());
    metrics.setActiveCount(info.getActiveThreadCount());
    metrics.setPoolSize(info.getCurrentPoolSize());
    metrics.setQueueSize(info.getQueueSize());
    metrics.setCompletedTaskCount(info.getCompletedTaskCount());
    metrics.setTaskCount(info.getTotalTaskCount());
    metrics.setQueueRemainingCapacity(Math.max(info.getQueueCapacity() - info.getQueueSize(), 0));
    return metrics;
  }

  public static Map<String, Object> toResponse(ThreadPoolMetricsVO metrics) {
    return toResponse(
        metrics.getName(),
        metrics.getCorePoolSize(),
        metrics.getMaxPoolSize(),
        metrics.getActiveCount(),
        metrics.getPoolSize(),
        metrics.getQueueSize(),
        metrics.getCompletedTaskCount(),
        metrics.getTaskCount(),
        metrics.getQueueRemainingCapacity());
  }

  private static Map<String, Object> toResponse(
      String name,
      Integer corePoolSize,
      Integer maxPoolSize,
      Integer activeCount,
      Integer poolSize,
      Integer queueSize,
      Long completedTaskCount,
      Long taskCount,
      Integer queueRemainingCapacity) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("name", name);
    response.put("corePoolSize", corePoolSize);
    response.put("maxPoolSize", maxPoolSize);
    response.put("activeCount", activeCount);
    response.put("poolSize", poolSize);
    response.put("queueSize", queueSize);
    response.put("completedTaskCount", completedTaskCount);
    response.put("taskCount", taskCount);
    response.put("queueRemainingCapacity", queueRemainingCapacity);
    return response;
  }
}
