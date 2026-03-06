package com.cloud.user.service;

import com.cloud.common.domain.vo.user.UserStatisticsVO;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;







public interface UserStatisticsService {

    





    UserStatisticsVO getUserStatisticsOverview();

    




    CompletableFuture<UserStatisticsVO> getUserStatisticsOverviewAsync();

    






    Map<LocalDate, Long> getUserRegistrationTrend(LocalDate startDate, LocalDate endDate);

    





    CompletableFuture<Map<LocalDate, Long>> getUserRegistrationTrendAsync(Integer days);

    




    Map<String, Long> getUserTypeDistribution();

    




    CompletableFuture<Map<String, Long>> getUserTypeDistributionAsync();

    




    Map<String, Long> getUserStatusDistribution();

    




    CompletableFuture<Map<String, Long>> getUserStatusDistributionAsync();

    





    Long countActiveUsers(Integer days);

    





    CompletableFuture<Long> countActiveUsersAsync(Integer days);

    




    Long countTodayNewUsers();

    




    Long countMonthNewUsers();

    





    Double calculateUserGrowthRate(Integer days);

    





    CompletableFuture<Double> calculateUserGrowthRateAsync(Integer days);

    






    CompletableFuture<Map<Long, Long>> getUserActivityRankingAsync(Integer limit, Integer days);

    




    CompletableFuture<Boolean> refreshStatisticsCacheAsync();
}
