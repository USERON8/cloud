package com.cloud.user.controller.internal;

import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.user.service.UserStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics/internal")
@RequiredArgsConstructor
@Tag(name = "Internal User Statistics", description = "Internal governance user statistics APIs")
@Validated
public class InternalUserStatisticsController {

  private final UserStatisticsService userStatisticsService;

  @GetMapping("/overview")
  @Operation(summary = "Get overview for internal governance callers")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  public Result<UserStatisticsVO> getStatisticsOverview() {
    UserStatisticsVO statistics = userStatisticsService.getUserStatisticsOverview();
    return Result.success("query successful", statistics);
  }

  @GetMapping("/registration-trend")
  @Operation(summary = "Get registration trend for internal governance callers")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  public Result<Map<LocalDate, Long>> getRegistrationTrend(
      @RequestParam @Parameter(description = "Start date") @DateTimeFormat(iso = ISO.DATE)
          LocalDate startDate,
      @RequestParam @Parameter(description = "End date") @DateTimeFormat(iso = ISO.DATE)
          LocalDate endDate) {
    if (endDate.isBefore(startDate)) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "endDate must be greater than or equal to startDate");
    }
    if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
      throw new BizException(ResultCode.BAD_REQUEST, "date range cannot exceed 365 days");
    }
    Map<LocalDate, Long> trend = userStatisticsService.getUserRegistrationTrend(startDate, endDate);
    return Result.success("query successful", trend);
  }

  @GetMapping("/role-distribution")
  @Operation(summary = "Get role distribution for internal governance callers")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  public Result<Map<String, Long>> getRoleDistribution() {
    Map<String, Long> distribution = userStatisticsService.getRoleDistribution();
    return Result.success("query successful", distribution);
  }

  @GetMapping("/status-distribution")
  @Operation(summary = "Get status distribution for internal governance callers")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  public Result<Map<String, Long>> getStatusDistribution() {
    Map<String, Long> distribution = userStatisticsService.getUserStatusDistribution();
    return Result.success("query successful", distribution);
  }

  @GetMapping("/active-users")
  @Operation(summary = "Count active users for internal governance callers")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  public Result<Long> countActiveUsers(
      @RequestParam(defaultValue = "7")
          @Parameter(description = "Recent days")
          @Min(value = 1, message = "days must be greater than 0")
          @Max(value = 365, message = "days must be less than or equal to 365")
          Integer days) {
    Long count = userStatisticsService.countActiveUsers(days);
    return Result.success("query successful", count);
  }

  @GetMapping("/growth-rate")
  @Operation(summary = "Calculate growth rate for internal governance callers")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  public Result<Double> calculateGrowthRate(
      @RequestParam(defaultValue = "7")
          @Parameter(description = "Recent days")
          @Min(value = 1, message = "days must be greater than 0")
          @Max(value = 365, message = "days must be less than or equal to 365")
          Integer days) {
    Double growthRate = userStatisticsService.calculateUserGrowthRate(days);
    return Result.success("query successful", growthRate);
  }

  @GetMapping("/activity-ranking")
  @Operation(summary = "Get activity ranking for internal governance callers")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  public CompletableFuture<Result<Map<Long, Long>>> getActivityRanking(
      @RequestParam(defaultValue = "10")
          @Parameter(description = "Ranking size")
          @Min(value = 1, message = "limit must be greater than 0")
          @Max(value = 100, message = "limit must be less than or equal to 100")
          Integer limit,
      @RequestParam(defaultValue = "30")
          @Parameter(description = "Recent days")
          @Min(value = 1, message = "days must be greater than 0")
          @Max(value = 365, message = "days must be less than or equal to 365")
          Integer days) {
    return userStatisticsService
        .getUserActivityRankingAsync(limit, days)
        .thenApply(ranking -> Result.success("query successful", ranking));
  }

  @PostMapping("/refresh-cache")
  @Operation(summary = "Refresh statistics cache for internal governance callers")
  @PreAuthorize("hasAuthority('SCOPE_internal')")
  public CompletableFuture<Result<Boolean>> refreshStatisticsCache() {
    return userStatisticsService
        .refreshStatisticsCacheAsync()
        .thenApply(result -> Result.success("cache refresh completed", result));
  }
}
