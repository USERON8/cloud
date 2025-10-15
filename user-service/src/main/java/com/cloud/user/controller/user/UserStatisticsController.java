package com.cloud.user.controller.user;

import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.common.result.Result;
import com.cloud.user.service.UserStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 用户统计Controller
 * 提供各种用户数据统计接口
 *
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "用户统计", description = "用户数据统计相关接口")
public class UserStatisticsController {

    private final UserStatisticsService userStatisticsService;

    @GetMapping("/overview")
    @Operation(summary = "获取用户统计概览", description = "获取用户总数、今日新增、本月新增等统计数据")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public Result<UserStatisticsVO> getStatisticsOverview() {
        log.info("获取用户统计概览");

        try {
            UserStatisticsVO statistics = userStatisticsService.getUserStatisticsOverview();
            return Result.success("获取统计数据成功", statistics);

        } catch (Exception e) {
            log.error("获取用户统计概览失败", e);
            return Result.fail("获取统计数据失败");
        }
    }

    @GetMapping("/overview/async")
    @Operation(summary = "异步获取用户统计概览", description = "异步方式获取用户统计数据，提升响应速度")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public CompletableFuture<Result<UserStatisticsVO>> getStatisticsOverviewAsync() {
        log.info("异步获取用户统计概览");

        return userStatisticsService.getUserStatisticsOverviewAsync()
                .thenApply(statistics -> Result.success("获取统计数据成功", statistics))
                .exceptionally(e -> {
                    log.error("异步获取用户统计概览失败", e);
                    return Result.fail("获取统计数据失败");
                });
    }

    @GetMapping("/registration-trend")
    @Operation(summary = "获取用户注册趋势", description = "获取指定时间段内的用户注册趋势")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public Result<Map<LocalDate, Long>> getRegistrationTrend(
            @RequestParam @Parameter(description = "开始日期") LocalDate startDate,
            @RequestParam @Parameter(description = "结束日期") LocalDate endDate) {

        log.info("获取用户注册趋势: {} - {}", startDate, endDate);

        try {
            Map<LocalDate, Long> trend = userStatisticsService.getUserRegistrationTrend(startDate, endDate);
            return Result.success("获取注册趋势成功", trend);

        } catch (Exception e) {
            log.error("获取用户注册趋势失败", e);
            return Result.fail("获取注册趋势失败");
        }
    }

    @GetMapping("/registration-trend/async")
    @Operation(summary = "异步获取用户注册趋势", description = "异步获取最近N天的用户注册趋势")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public CompletableFuture<Result<Map<LocalDate, Long>>> getRegistrationTrendAsync(
            @RequestParam(defaultValue = "30") @Parameter(description = "统计天数") Integer days) {

        log.info("异步获取用户注册趋势，天数: {}", days);

        return userStatisticsService.getUserRegistrationTrendAsync(days)
                .thenApply(trend -> Result.success("获取注册趋势成功", trend))
                .exceptionally(e -> {
                    log.error("异步获取用户注册趋势失败", e);
                    return Result.fail("获取注册趋势失败");
                });
    }

    @GetMapping("/type-distribution")
    @Operation(summary = "获取用户类型分布", description = "获取各类型用户的数量分布")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public Result<Map<String, Long>> getTypeDistribution() {
        log.info("获取用户类型分布");

        try {
            Map<String, Long> distribution = userStatisticsService.getUserTypeDistribution();
            return Result.success("获取类型分布成功", distribution);

        } catch (Exception e) {
            log.error("获取用户类型分布失败", e);
            return Result.fail("获取类型分布失败");
        }
    }

    @GetMapping("/status-distribution")
    @Operation(summary = "获取用户状态分布", description = "获取活跃/禁用用户的数量分布")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public Result<Map<String, Long>> getStatusDistribution() {
        log.info("获取用户状态分布");

        try {
            Map<String, Long> distribution = userStatisticsService.getUserStatusDistribution();
            return Result.success("获取状态分布成功", distribution);

        } catch (Exception e) {
            log.error("获取用户状态分布失败", e);
            return Result.fail("获取状态分布失败");
        }
    }

    @GetMapping("/active-users")
    @Operation(summary = "统计活跃用户数", description = "统计最近N天的活跃用户数量")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public Result<Long> countActiveUsers(
            @RequestParam(defaultValue = "7") @Parameter(description = "统计天数") Integer days) {

        log.info("统计活跃用户数，天数: {}", days);

        try {
            Long count = userStatisticsService.countActiveUsers(days);
            return Result.success("统计活跃用户成功", count);

        } catch (Exception e) {
            log.error("统计活跃用户数失败", e);
            return Result.fail("统计活跃用户失败");
        }
    }

    @GetMapping("/growth-rate")
    @Operation(summary = "计算用户增长率", description = "计算最近N天相比前N天的用户增长率")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public Result<Double> calculateGrowthRate(
            @RequestParam(defaultValue = "7") @Parameter(description = "对比天数") Integer days) {

        log.info("计算用户增长率，天数: {}", days);

        try {
            Double growthRate = userStatisticsService.calculateUserGrowthRate(days);
            return Result.success("计算增长率成功", growthRate);

        } catch (Exception e) {
            log.error("计算用户增长率失败", e);
            return Result.fail("计算增长率失败");
        }
    }

    @GetMapping("/activity-ranking")
    @Operation(summary = "获取用户活跃度排行", description = "获取最活跃的N个用户")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public CompletableFuture<Result<Map<Long, Long>>> getActivityRanking(
            @RequestParam(defaultValue = "10") @Parameter(description = "返回数量") Integer limit,
            @RequestParam(defaultValue = "30") @Parameter(description = "统计天数") Integer days) {

        log.info("获取用户活跃度排行，数量: {}, 天数: {}", limit, days);

        return userStatisticsService.getUserActivityRankingAsync(limit, days)
                .thenApply(ranking -> Result.success("获取活跃度排行成功", ranking))
                .exceptionally(e -> {
                    log.error("获取用户活跃度排行失败", e);
                    return Result.fail("获取活跃度排行失败");
                });
    }

    @PostMapping("/refresh-cache")
    @Operation(summary = "刷新统计缓存", description = "手动刷新所有统计数据缓存")
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    public CompletableFuture<Result<Boolean>> refreshStatisticsCache() {
        log.info("刷新统计数据缓存");

        return userStatisticsService.refreshStatisticsCacheAsync()
                .thenApply(result -> Result.success("刷新缓存成功", result))
                .exceptionally(e -> {
                    log.error("刷新统计缓存失败", e);
                    return Result.fail("刷新缓存失败");
                });
    }
}
