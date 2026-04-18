package com.cloud.user.rpc;

import com.cloud.api.user.UserGovernanceDubboApi;
import com.cloud.common.domain.vo.governance.ThreadPoolMetricsVO;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.common.threadpool.ThreadPoolInfo;
import com.cloud.common.threadpool.ThreadPoolMonitor;
import com.cloud.user.service.UserStatisticsService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = UserGovernanceDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class UserGovernanceDubboService implements UserGovernanceDubboApi {

  private final UserStatisticsService userStatisticsService;
  private final ThreadPoolMonitor threadPoolMonitor;

  @Override
  public UserStatisticsVO getStatisticsOverview() {
    return userStatisticsService.getUserStatisticsOverview();
  }

  @Override
  public Map<LocalDate, Long> getRegistrationTrend(LocalDate startDate, LocalDate endDate) {
    return userStatisticsService.getUserRegistrationTrend(startDate, endDate);
  }

  @Override
  public Map<String, Long> getRoleDistribution() {
    return userStatisticsService.getRoleDistribution();
  }

  @Override
  public Map<String, Long> getStatusDistribution() {
    return userStatisticsService.getUserStatusDistribution();
  }

  @Override
  public Long countActiveUsers(Integer days) {
    return userStatisticsService.countActiveUsers(days);
  }

  @Override
  public Double calculateGrowthRate(Integer days) {
    return userStatisticsService.calculateUserGrowthRate(days);
  }

  @Override
  public Map<Long, Long> getActivityRanking(Integer limit, Integer days) {
    return userStatisticsService.getUserActivityRankingAsync(limit, days).join();
  }

  @Override
  public Boolean refreshStatisticsCache() {
    return userStatisticsService.refreshStatisticsCacheAsync().join();
  }

  @Override
  public List<ThreadPoolMetricsVO> getThreadPoolInfoList() {
    return threadPoolMonitor.getAllThreadPoolInfo().values().stream().map(this::toMetrics).toList();
  }

  @Override
  public ThreadPoolMetricsVO getThreadPoolInfo(String name) {
    ThreadPoolInfo info = threadPoolMonitor.getThreadPoolInfo(name);
    return info == null ? null : toMetrics(info);
  }

  private ThreadPoolMetricsVO toMetrics(ThreadPoolInfo info) {
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
}
