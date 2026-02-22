package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 用户统计服务实现类
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    @Resource
    @Qualifier("userStatisticsExecutor")
    private Executor userStatisticsExecutor;

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'overview'", unless = "#result == null")
    public UserStatisticsVO getUserStatisticsOverview() {
        log.info("获取用户统计概览");
        long startTime = System.currentTimeMillis();

        try {
            UserStatisticsVO vo = new UserStatisticsVO();

            // 总用户数
            vo.setTotalUsers(userMapper.selectCount(null));

            // 今日新增
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LambdaQueryWrapper<User> todayWrapper = new LambdaQueryWrapper<>();
            todayWrapper.ge(User::getCreatedAt, todayStart);
            vo.setTodayNewUsers(userMapper.selectCount(todayWrapper));

            // 本月新增
            LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LambdaQueryWrapper<User> monthWrapper = new LambdaQueryWrapper<>();
            monthWrapper.ge(User::getCreatedAt, monthStart);
            vo.setMonthNewUsers(userMapper.selectCount(monthWrapper));

            // 活跃用户数（最近7天）
            vo.setActiveUsers(countActiveUsers(7));

            // 用户类型分布
            vo.setUserTypeDistribution(getUserTypeDistribution());

            // 用户状态分布
            vo.setUserStatusDistribution(getUserStatusDistribution());

            log.info("获取用户统计概览完成，耗时: {}ms", System.currentTimeMillis() - startTime);
            return vo;

        } catch (Exception e) {
            log.error("获取用户统计概览失败", e);
            return new UserStatisticsVO();
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<UserStatisticsVO> getUserStatisticsOverviewAsync() {
        return CompletableFuture.supplyAsync(this::getUserStatisticsOverview, userStatisticsExecutor);
    }

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'registration:' + #startDate + ':' + #endDate")
    public Map<LocalDate, Long> getUserRegistrationTrend(LocalDate startDate, LocalDate endDate) {
        log.info("统计用户注册趋势: {} - {}", startDate, endDate);

        try {
            Map<LocalDate, Long> trend = new LinkedHashMap<>();
            LocalDate current = startDate;

            while (!current.isAfter(endDate)) {
                LocalDateTime dayStart = current.atStartOfDay();
                LocalDateTime dayEnd = current.plusDays(1).atStartOfDay();

                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.ge(User::getCreatedAt, dayStart)
                        .lt(User::getCreatedAt, dayEnd);

                Long count = userMapper.selectCount(wrapper);
                trend.put(current, count);

                current = current.plusDays(1);
            }

            return trend;

        } catch (Exception e) {
            log.error("统计用户注册趋势失败", e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<LocalDate, Long>> getUserRegistrationTrendAsync(Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        return CompletableFuture.supplyAsync(() -> getUserRegistrationTrend(startDate, endDate), userStatisticsExecutor);
    }

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'type_distribution'")
    public Map<String, Long> getUserTypeDistribution() {
        log.debug("统计用户类型分布");

        try {
            List<User> users = userMapper.selectList(null);
            return users.stream()
                    .collect(Collectors.groupingBy(
                            user -> user.getUserType() != null ? user.getUserType() : "UNKNOWN",
                            Collectors.counting()
                    ));

        } catch (Exception e) {
            log.error("统计用户类型分布失败", e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<String, Long>> getUserTypeDistributionAsync() {
        return CompletableFuture.supplyAsync(this::getUserTypeDistribution, userStatisticsExecutor);
    }

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'status_distribution'")
    public Map<String, Long> getUserStatusDistribution() {
        log.debug("统计用户状态分布");

        try {
            List<User> users = userMapper.selectList(null);
            Map<String, Long> distribution = new HashMap<>();

            long active = users.stream().filter(u -> u.getStatus() != null && u.getStatus() == 1).count();
            long inactive = users.stream().filter(u -> u.getStatus() == null || u.getStatus() == 0).count();

            distribution.put("active", active);
            distribution.put("inactive", inactive);

            return distribution;

        } catch (Exception e) {
            log.error("统计用户状态分布失败", e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<String, Long>> getUserStatusDistributionAsync() {
        return CompletableFuture.supplyAsync(this::getUserStatusDistribution, userStatisticsExecutor);
    }

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'active:' + #days")
    public Long countActiveUsers(Integer days) {
        log.debug("统计活跃用户数，最近{}天", days);

        try {
            // 从Redis获取最近登录记录
            String pattern = "user:last_login:*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return 0L;
            }

            LocalDateTime threshold = LocalDateTime.now().minusDays(days);
            return keys.stream()
                    .map(key -> (LocalDateTime) redisTemplate.opsForValue().get(key))
                    .filter(Objects::nonNull)
                    .filter(loginTime -> loginTime.isAfter(threshold))
                    .count();

        } catch (Exception e) {
            log.error("统计活跃用户数失败", e);
            return 0L;
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> countActiveUsersAsync(Integer days) {
        return CompletableFuture.supplyAsync(() -> countActiveUsers(days), userStatisticsExecutor);
    }

    @Override
    public Long countTodayNewUsers() {
        log.debug("统计今日新增用户数");

        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(User::getCreatedAt, todayStart);

            return userMapper.selectCount(wrapper);

        } catch (Exception e) {
            log.error("统计今日新增用户数失败", e);
            return 0L;
        }
    }

    @Override
    public Long countMonthNewUsers() {
        log.debug("统计本月新增用户数");

        try {
            LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(User::getCreatedAt, monthStart);

            return userMapper.selectCount(wrapper);

        } catch (Exception e) {
            log.error("统计本月新增用户数失败", e);
            return 0L;
        }
    }

    @Override
    public Double calculateUserGrowthRate(Integer days) {
        log.debug("计算用户增长率，对比{}天", days);

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime periodStart = now.minusDays(days);
            LocalDateTime previousPeriodStart = now.minusDays(days * 2);

            // 当前周期新增用户
            LambdaQueryWrapper<User> currentWrapper = new LambdaQueryWrapper<>();
            currentWrapper.ge(User::getCreatedAt, periodStart);
            Long currentPeriodCount = userMapper.selectCount(currentWrapper);

            // 前一周期新增用户
            LambdaQueryWrapper<User> previousWrapper = new LambdaQueryWrapper<>();
            previousWrapper.ge(User::getCreatedAt, previousPeriodStart)
                    .lt(User::getCreatedAt, periodStart);
            Long previousPeriodCount = userMapper.selectCount(previousWrapper);

            // 计算增长率
            if (previousPeriodCount == 0) {
                return currentPeriodCount > 0 ? 100.0 : 0.0;
            }

            double growthRate = ((currentPeriodCount - previousPeriodCount) * 100.0) / previousPeriodCount;
            return Math.round(growthRate * 100.0) / 100.0; // 保留两位小数

        } catch (Exception e) {
            log.error("计算用户增长率失败", e);
            return 0.0;
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Double> calculateUserGrowthRateAsync(Integer days) {
        return CompletableFuture.supplyAsync(() -> calculateUserGrowthRate(days), userStatisticsExecutor);
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<Long, Long>> getUserActivityRankingAsync(Integer limit, Integer days) {
        log.info("异步获取用户活跃度排行，数量: {}, 天数: {}", limit, days);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 从Redis获取活跃度数据
                String pattern = "user:activity:*";
                Set<String> keys = redisTemplate.keys(pattern);

                if (keys == null || keys.isEmpty()) {
                    return Collections.emptyMap();
                }

                // 统计活跃度并排序
                Map<Long, Long> activityMap = new HashMap<>();
                LocalDateTime threshold = LocalDateTime.now().minusDays(days);

                for (String key : keys) {
                    try {
                        Long userId = Long.parseLong(key.substring(key.lastIndexOf(":") + 1));
                        // 这里可以根据实际的活跃度指标进行计算
                        // 例如：登录次数、操作次数等
                        Long activityCount = (Long) redisTemplate.opsForValue().get(key);
                        if (activityCount != null) {
                            activityMap.put(userId, activityCount);
                        }
                    } catch (Exception e) {
                        log.warn("解析活跃度数据失败: {}", key);
                    }
                }

                // 按活跃度排序并取前N名
                return activityMap.entrySet().stream()
                        .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                        .limit(limit)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));

            } catch (Exception e) {
                log.error("获取用户活跃度排行失败", e);
                return Collections.emptyMap();
            }
        }, userStatisticsExecutor);
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Boolean> refreshStatisticsCacheAsync() {
        log.info("异步刷新统计数据缓存");

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 刷新各类统计数据
                getUserStatisticsOverview();
                getUserTypeDistribution();
                getUserStatusDistribution();
                countActiveUsers(7);
                countActiveUsers(30);

                log.info("刷新统计数据缓存完成");
                return true;

            } catch (Exception e) {
                log.error("刷新统计数据缓存失败", e);
                return false;
            }
        }, userStatisticsExecutor);
    }
}
