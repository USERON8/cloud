package com.cloud.api.user;

import com.cloud.common.domain.vo.governance.ThreadPoolMetricsVO;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户统计治理内部调用契约。
 *
 * <p>治理服务通过该接口读取用户统计、活跃度和线程池指标，用户服务负责指标口径。
 */
public interface UserGovernanceDubboApi {

  /** 获取用户统计总览。 */
  UserStatisticsVO getStatisticsOverview();

  /** 获取指定日期范围内的注册趋势。 */
  Map<LocalDate, Long> getRegistrationTrend(LocalDate startDate, LocalDate endDate);

  /** 获取用户角色分布。 */
  Map<String, Long> getRoleDistribution();

  /** 获取用户状态分布。 */
  Map<String, Long> getStatusDistribution();

  /** 统计近 N 天活跃用户数。 */
  Long countActiveUsers(Integer days);

  /** 计算近 N 天用户增长率。 */
  Double calculateGrowthRate(Integer days);

  /** 获取用户活跃度排行。 */
  Map<Long, Long> getActivityRanking(Integer limit, Integer days);

  /** 刷新用户统计缓存。 */
  Boolean refreshStatisticsCache();

  /** 获取线程池指标列表。 */
  List<ThreadPoolMetricsVO> getThreadPoolInfoList();

  /** 按线程池名称获取指标。 */
  ThreadPoolMetricsVO getThreadPoolInfo(String name);
}
