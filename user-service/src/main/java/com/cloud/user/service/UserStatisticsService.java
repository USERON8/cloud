package com.cloud.user.service;

import com.cloud.common.domain.vo.user.UserStatisticsVO;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 用户统计服务接口
 * 提供各种用户数据统计功能
 *
 * @author what's up
 */
public interface UserStatisticsService {

    /**
     * 获取用户统计概览
     * 包括总用户数、活跃用户数、今日新增等
     *
     * @return 统计概览VO
     */
    UserStatisticsVO getUserStatisticsOverview();

    /**
     * 异步获取用户统计概览
     *
     * @return 统计概览VO的Future
     */
    CompletableFuture<UserStatisticsVO> getUserStatisticsOverviewAsync();

    /**
     * 统计用户注册趋势
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return Map<日期, 注册数>
     */
    Map<LocalDate, Long> getUserRegistrationTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 异步统计用户注册趋势
     *
     * @param days 最近天数
     * @return Map<日期, 注册数>的Future
     */
    CompletableFuture<Map<LocalDate, Long>> getUserRegistrationTrendAsync(Integer days);

    /**
     * 统计用户类型分布
     *
     * @return Map<用户类型, 数量>
     */
    Map<String, Long> getUserTypeDistribution();

    /**
     * 异步统计用户类型分布
     *
     * @return Map<用户类型, 数量>的Future
     */
    CompletableFuture<Map<String, Long>> getUserTypeDistributionAsync();

    /**
     * 统计用户状态分布
     *
     * @return Map<状态, 数量>
     */
    Map<String, Long> getUserStatusDistribution();

    /**
     * 异步统计用户状态分布
     *
     * @return Map<状态, 数量>的Future
     */
    CompletableFuture<Map<String, Long>> getUserStatusDistributionAsync();

    /**
     * 统计活跃用户数
     *
     * @param days 最近天数
     * @return 活跃用户数
     */
    Long countActiveUsers(Integer days);

    /**
     * 异步统计活跃用户数
     *
     * @param days 最近天数
     * @return 活跃用户数的Future
     */
    CompletableFuture<Long> countActiveUsersAsync(Integer days);

    /**
     * 统计今日新增用户数
     *
     * @return 今日新增数
     */
    Long countTodayNewUsers();

    /**
     * 统计本月新增用户数
     *
     * @return 本月新增数
     */
    Long countMonthNewUsers();

    /**
     * 统计用户增长率
     *
     * @param days 对比天数
     * @return 增长率（百分比）
     */
    Double calculateUserGrowthRate(Integer days);

    /**
     * 异步统计用户增长率
     *
     * @param days 对比天数
     * @return 增长率的Future
     */
    CompletableFuture<Double> calculateUserGrowthRateAsync(Integer days);

    /**
     * 统计用户活跃度排行
     *
     * @param limit 返回数量
     * @param days  统计天数
     * @return 用户ID列表
     */
    CompletableFuture<Map<Long, Long>> getUserActivityRankingAsync(Integer limit, Integer days);

    /**
     * 刷新所有统计数据缓存
     *
     * @return 刷新结果
     */
    CompletableFuture<Boolean> refreshStatisticsCacheAsync();
}
