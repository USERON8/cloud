package com.cloud.user.controller.user;

import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.common.result.Result;
import com.cloud.user.controller.support.UserStatisticsResponseSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
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
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@Tag(name = "用户统计", description = "用户统计接口")
@Validated
@ApiResponses({
  @ApiResponse(responseCode = "400", description = "统计查询参数无效"),
  @ApiResponse(responseCode = "401", description = "需要认证"),
  @ApiResponse(responseCode = "403", description = "权限不足"),
  @ApiResponse(responseCode = "500", description = "统计服务内部错误")
})
public class UserStatisticsController {

  private final UserStatisticsResponseSupport userStatisticsResponseSupport;

  @GetMapping("/overview")
  @Operation(summary = "获取统计总览", description = "获取用户统计总览")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<UserStatisticsVO> getStatisticsOverview() {
    return userStatisticsResponseSupport.getStatisticsOverview();
  }

  @GetMapping("/overview/async")
  @Operation(
      summary = "异步获取统计总览",
      description = "异步获取用户统计总览")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<UserStatisticsVO>> getStatisticsOverviewAsync() {
    return userStatisticsResponseSupport.getStatisticsOverviewAsync();
  }

  @GetMapping("/registration-trend")
  @Operation(
      summary = "获取注册趋势",
      description = "按日期范围获取用户注册趋势")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<LocalDate, Long>> getRegistrationTrend(
      @RequestParam @Parameter(description = "开始日期") @DateTimeFormat(iso = ISO.DATE)
          LocalDate startDate,
      @RequestParam @Parameter(description = "结束日期") @DateTimeFormat(iso = ISO.DATE)
          LocalDate endDate) {
    return userStatisticsResponseSupport.getRegistrationTrend(startDate, endDate);
  }

  @GetMapping("/registration-trend/async")
  @Operation(
      summary = "异步获取注册趋势",
      description = "异步获取用户注册趋势")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<Map<LocalDate, Long>>> getRegistrationTrendAsync(
      @RequestParam(defaultValue = "30")
          @Parameter(description = "最近天数")
          @Min(value = 1, message = "days must be greater than 0")
          @Max(value = 365, message = "days must be less than or equal to 365")
          Integer days) {
    return userStatisticsResponseSupport.getRegistrationTrendAsync(days);
  }

  @GetMapping("/role-distribution")
  @Operation(summary = "获取角色分布", description = "获取用户角色分布")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Long>> getRoleDistribution() {
    return userStatisticsResponseSupport.getRoleDistribution();
  }

  @GetMapping("/status-distribution")
  @Operation(summary = "获取状态分布", description = "获取用户状态分布")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Map<String, Long>> getStatusDistribution() {
    return userStatisticsResponseSupport.getStatusDistribution();
  }

  @GetMapping("/active-users")
  @Operation(summary = "统计活跃用户", description = "统计最近天数内的活跃用户")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Long> countActiveUsers(
      @RequestParam(defaultValue = "7")
          @Parameter(description = "最近天数")
          @Min(value = 1, message = "days must be greater than 0")
          @Max(value = 365, message = "days must be less than or equal to 365")
          Integer days) {
    return userStatisticsResponseSupport.countActiveUsers(days);
  }

  @GetMapping("/growth-rate")
  @Operation(
      summary = "计算增长率",
      description = "计算最近天数内的用户增长率")
  @PreAuthorize("hasAuthority('admin:all')")
  public Result<Double> calculateGrowthRate(
      @RequestParam(defaultValue = "7")
          @Parameter(description = "最近天数")
          @Min(value = 1, message = "days must be greater than 0")
          @Max(value = 365, message = "days must be less than or equal to 365")
          Integer days) {
    return userStatisticsResponseSupport.calculateGrowthRate(days);
  }

  @GetMapping("/activity-ranking")
  @Operation(summary = "获取活跃度排行", description = "获取活跃用户排行")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<Map<Long, Long>>> getActivityRanking(
      @RequestParam(defaultValue = "10")
          @Parameter(description = "排行数量")
          @Min(value = 1, message = "limit must be greater than 0")
          @Max(value = 100, message = "limit must be less than or equal to 100")
          Integer limit,
      @RequestParam(defaultValue = "30")
          @Parameter(description = "最近天数")
          @Min(value = 1, message = "days must be greater than 0")
          @Max(value = 365, message = "days must be less than or equal to 365")
          Integer days) {
    return userStatisticsResponseSupport.getActivityRanking(limit, days);
  }

  @PostMapping("/cache-refreshes")
  @Operation(
      summary = "刷新统计缓存",
      description = "异步刷新统计缓存")
  @PreAuthorize("hasAuthority('admin:all')")
  public CompletableFuture<Result<Boolean>> refreshStatisticsCache() {
    return userStatisticsResponseSupport.refreshStatisticsCache();
  }
}
