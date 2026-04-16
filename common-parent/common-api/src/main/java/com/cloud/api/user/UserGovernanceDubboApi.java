package com.cloud.api.user;

import com.cloud.common.domain.vo.governance.ThreadPoolMetricsVO;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface UserGovernanceDubboApi {

  UserStatisticsVO getStatisticsOverview();

  Map<LocalDate, Long> getRegistrationTrend(LocalDate startDate, LocalDate endDate);

  Map<String, Long> getRoleDistribution();

  Map<String, Long> getStatusDistribution();

  Long countActiveUsers(Integer days);

  Double calculateGrowthRate(Integer days);

  Map<Long, Long> getActivityRanking(Integer limit, Integer days);

  Boolean refreshStatisticsCache();

  List<ThreadPoolMetricsVO> getThreadPoolInfoList();

  ThreadPoolMetricsVO getThreadPoolInfo(String name);
}
