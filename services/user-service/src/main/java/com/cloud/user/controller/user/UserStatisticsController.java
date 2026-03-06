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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "User Statistics", description = "User statistics APIs")
public class UserStatisticsController {

    private final UserStatisticsService userStatisticsService;

    @GetMapping("/overview")
    @Operation(summary = "Get overview", description = "Get user statistics overview")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    public Result<UserStatisticsVO> getStatisticsOverview() {
        try {
            UserStatisticsVO statistics = userStatisticsService.getUserStatisticsOverview();
            return Result.success("query successful", statistics);
        } catch (Exception e) {
            log.error("Failed to get statistics overview", e);
            return Result.fail("failed to get statistics overview");
        }
    }

    @GetMapping("/overview/async")
    @Operation(summary = "Get overview async", description = "Get user statistics overview asynchronously")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    public CompletableFuture<Result<UserStatisticsVO>> getStatisticsOverviewAsync() {
        return userStatisticsService.getUserStatisticsOverviewAsync()
                .thenApply(statistics -> Result.success("query successful", statistics))
                .exceptionally(e -> {
                    log.error("Failed to get statistics overview async", e);
                    return Result.fail("failed to get statistics overview");
                });
    }

    @GetMapping("/registration-trend")
    @Operation(summary = "Get registration trend", description = "Get user registration trend by date range")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    public Result<Map<LocalDate, Long>> getRegistrationTrend(
            @RequestParam @Parameter(description = "Start date") LocalDate startDate,
            @RequestParam @Parameter(description = "End date") LocalDate endDate) {
        try {
            Map<LocalDate, Long> trend = userStatisticsService.getUserRegistrationTrend(startDate, endDate);
            return Result.success("query successful", trend);
        } catch (Exception e) {
            log.error("Failed to get registration trend", e);
            return Result.fail("failed to get registration trend");
        }
    }

    @GetMapping("/registration-trend/async")
    @Operation(summary = "Get registration trend async", description = "Get user registration trend asynchronously")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    public CompletableFuture<Result<Map<LocalDate, Long>>> getRegistrationTrendAsync(
            @RequestParam(defaultValue = "30") @Parameter(description = "Recent days") Integer days) {
        return userStatisticsService.getUserRegistrationTrendAsync(days)
                .thenApply(trend -> Result.success("query successful", trend))
                .exceptionally(e -> {
                    log.error("Failed to get registration trend async", e);
                    return Result.fail("failed to get registration trend");
                });
    }

    @GetMapping("/type-distribution")
    @Operation(summary = "Get type distribution", description = "Get user type distribution")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    public Result<Map<String, Long>> getTypeDistribution() {
        try {
            Map<String, Long> distribution = userStatisticsService.getUserTypeDistribution();
            return Result.success("query successful", distribution);
        } catch (Exception e) {
            log.error("Failed to get user type distribution", e);
            return Result.fail("failed to get user type distribution");
        }
    }

    @GetMapping("/status-distribution")
    @Operation(summary = "Get status distribution", description = "Get user status distribution")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    public Result<Map<String, Long>> getStatusDistribution() {
        try {
            Map<String, Long> distribution = userStatisticsService.getUserStatusDistribution();
            return Result.success("query successful", distribution);
        } catch (Exception e) {
            log.error("Failed to get user status distribution", e);
            return Result.fail("failed to get user status distribution");
        }
    }

    @GetMapping("/active-users")
    @Operation(summary = "Count active users", description = "Count active users in recent days")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    public Result<Long> countActiveUsers(
            @RequestParam(defaultValue = "7") @Parameter(description = "Recent days") Integer days) {
        try {
            Long count = userStatisticsService.countActiveUsers(days);
            return Result.success("query successful", count);
        } catch (Exception e) {
            log.error("Failed to count active users", e);
            return Result.fail("failed to count active users");
        }
    }

    @GetMapping("/growth-rate")
    @Operation(summary = "Calculate growth rate", description = "Calculate user growth rate for recent days")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    public Result<Double> calculateGrowthRate(
            @RequestParam(defaultValue = "7") @Parameter(description = "Recent days") Integer days) {
        try {
            Double growthRate = userStatisticsService.calculateUserGrowthRate(days);
            return Result.success("query successful", growthRate);
        } catch (Exception e) {
            log.error("Failed to calculate user growth rate", e);
            return Result.fail("failed to calculate user growth rate");
        }
    }

    @GetMapping("/activity-ranking")
    @Operation(summary = "Get activity ranking", description = "Get top active users ranking")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:read')")
    public CompletableFuture<Result<Map<Long, Long>>> getActivityRanking(
            @RequestParam(defaultValue = "10") @Parameter(description = "Ranking size") Integer limit,
            @RequestParam(defaultValue = "30") @Parameter(description = "Recent days") Integer days) {
        return userStatisticsService.getUserActivityRankingAsync(limit, days)
                .thenApply(ranking -> Result.success("query successful", ranking))
                .exceptionally(e -> {
                    log.error("Failed to get activity ranking", e);
                    return Result.fail("failed to get activity ranking");
                });
    }

    @PostMapping("/refresh-cache")
    @Operation(summary = "Refresh statistics cache", description = "Refresh statistics cache asynchronously")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
    public CompletableFuture<Result<Boolean>> refreshStatisticsCache() {
        return userStatisticsService.refreshStatisticsCacheAsync()
                .thenApply(result -> Result.success("cache refresh completed", result))
                .exceptionally(e -> {
                    log.error("Failed to refresh statistics cache", e);
                    return Result.fail("failed to refresh statistics cache");
                });
    }
}
