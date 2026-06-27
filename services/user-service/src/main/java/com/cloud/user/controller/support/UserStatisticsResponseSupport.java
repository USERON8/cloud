package com.cloud.user.controller.support;

import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.common.result.Result;
import com.cloud.common.util.DateRangeValidator;
import com.cloud.user.service.UserStatisticsService;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserStatisticsResponseSupport {

  private final UserStatisticsService userStatisticsService;

  public Result<UserStatisticsVO> getStatisticsOverview() {
    UserStatisticsVO statistics = userStatisticsService.getUserStatisticsOverview();
    return Result.success("query successful", statistics);
  }

  public CompletableFuture<Result<UserStatisticsVO>> getStatisticsOverviewAsync() {
    return userStatisticsService
        .getUserStatisticsOverviewAsync()
        .thenApply(statistics -> Result.success("query successful", statistics));
  }

  public Result<Map<LocalDate, Long>> getRegistrationTrend(LocalDate startDate, LocalDate endDate) {
    DateRangeValidator.validateInclusiveRange(startDate, endDate, 365);
    Map<LocalDate, Long> trend = userStatisticsService.getUserRegistrationTrend(startDate, endDate);
    return Result.success("query successful", trend);
  }

  public CompletableFuture<Result<Map<LocalDate, Long>>> getRegistrationTrendAsync(Integer days) {
    return userStatisticsService
        .getUserRegistrationTrendAsync(days)
        .thenApply(trend -> Result.success("query successful", trend));
  }

  public Result<Map<String, Long>> getRoleDistribution() {
    Map<String, Long> distribution = userStatisticsService.getRoleDistribution();
    return Result.success("query successful", distribution);
  }

  public Result<Map<String, Long>> getStatusDistribution() {
    Map<String, Long> distribution = userStatisticsService.getUserStatusDistribution();
    return Result.success("query successful", distribution);
  }

  public Result<Long> countActiveUsers(Integer days) {
    Long count = userStatisticsService.countActiveUsers(days);
    return Result.success("query successful", count);
  }

  public Result<Double> calculateGrowthRate(Integer days) {
    Double growthRate = userStatisticsService.calculateUserGrowthRate(days);
    return Result.success("query successful", growthRate);
  }

  public CompletableFuture<Result<Map<Long, Long>>> getActivityRanking(
      Integer limit, Integer days) {
    return userStatisticsService
        .getUserActivityRankingAsync(limit, days)
        .thenApply(ranking -> Result.success("query successful", ranking));
  }

  public CompletableFuture<Result<Boolean>> refreshStatisticsCache() {
    return userStatisticsService
        .refreshStatisticsCacheAsync()
        .thenApply(result -> Result.success("cache refresh completed", result));
  }
}
