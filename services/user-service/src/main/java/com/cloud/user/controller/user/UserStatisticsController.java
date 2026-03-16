package com.cloud.user.controller.user;

import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.common.result.Result;
import com.cloud.user.service.UserStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "User Statistics", description = "User statistics APIs")
public class UserStatisticsController {

  private final UserStatisticsService userStatisticsService;

  @GetMapping("/overview")
  @Operation(summary = "Get overview", description = "Get user statistics overview")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<UserStatisticsVO> getStatisticsOverview() {
    UserStatisticsVO statistics = userStatisticsService.getUserStatisticsOverview();
    return Result.success("query successful", statistics);
  }

  @GetMapping("/overview/async")
  @Operation(
      summary = "Get overview async",
      description = "Get user statistics overview asynchronously")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<UserStatisticsVO>> getStatisticsOverviewAsync() {
    return userStatisticsService
        .getUserStatisticsOverviewAsync()
        .thenApply(statistics -> Result.success("query successful", statistics));
  }

  @GetMapping("/registration-trend")
  @Operation(
      summary = "Get registration trend",
      description = "Get user registration trend by date range")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<LocalDate, Long>> getRegistrationTrend(
      @RequestParam @Parameter(description = "Start date") LocalDate startDate,
      @RequestParam @Parameter(description = "End date") LocalDate endDate) {
    Map<LocalDate, Long> trend =
        userStatisticsService.getUserRegistrationTrend(startDate, endDate);
    return Result.success("query successful", trend);
  }

  @GetMapping("/registration-trend/async")
  @Operation(
      summary = "Get registration trend async",
      description = "Get user registration trend asynchronously")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<Map<LocalDate, Long>>> getRegistrationTrendAsync(
      @RequestParam(defaultValue = "30") @Parameter(description = "Recent days") Integer days) {
    return userStatisticsService
        .getUserRegistrationTrendAsync(days)
        .thenApply(trend -> Result.success("query successful", trend));
  }

  @GetMapping("/role-distribution")
  @Operation(summary = "Get role distribution", description = "Get user role distribution")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Long>> getRoleDistribution() {
    Map<String, Long> distribution = userStatisticsService.getRoleDistribution();
    return Result.success("query successful", distribution);
  }

  @GetMapping("/status-distribution")
  @Operation(summary = "Get status distribution", description = "Get user status distribution")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Long>> getStatusDistribution() {
    Map<String, Long> distribution = userStatisticsService.getUserStatusDistribution();
    return Result.success("query successful", distribution);
  }

  @GetMapping("/active-users")
  @Operation(summary = "Count active users", description = "Count active users in recent days")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Long> countActiveUsers(
      @RequestParam(defaultValue = "7") @Parameter(description = "Recent days") Integer days) {
    Long count = userStatisticsService.countActiveUsers(days);
    return Result.success("query successful", count);
  }

  @GetMapping("/growth-rate")
  @Operation(
      summary = "Calculate growth rate",
      description = "Calculate user growth rate for recent days")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Double> calculateGrowthRate(
      @RequestParam(defaultValue = "7") @Parameter(description = "Recent days") Integer days) {
    Double growthRate = userStatisticsService.calculateUserGrowthRate(days);
    return Result.success("query successful", growthRate);
  }

  @GetMapping("/activity-ranking")
  @Operation(summary = "Get activity ranking", description = "Get top active users ranking")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<Map<Long, Long>>> getActivityRanking(
      @RequestParam(defaultValue = "10") @Parameter(description = "Ranking size") Integer limit,
      @RequestParam(defaultValue = "30") @Parameter(description = "Recent days") Integer days) {
    return userStatisticsService
        .getUserActivityRankingAsync(limit, days)
        .thenApply(ranking -> Result.success("query successful", ranking));
  }

  @PostMapping("/refresh-cache")
  @Operation(
      summary = "Refresh statistics cache",
      description = "Refresh statistics cache asynchronously")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<Boolean>> refreshStatisticsCache() {
    return userStatisticsService
        .refreshStatisticsCacheAsync()
        .thenApply(result -> Result.success("cache refresh completed", result));
  }
}
