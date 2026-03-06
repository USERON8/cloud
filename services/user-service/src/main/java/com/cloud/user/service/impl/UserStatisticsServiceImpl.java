package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'overview'", unless = "#result == null")
    public UserStatisticsVO getUserStatisticsOverview() {
        try {
            UserStatisticsVO vo = new UserStatisticsVO();
            vo.setTotalUsers(userMapper.selectCount(null));
            vo.setTodayNewUsers(countTodayNewUsers());
            vo.setMonthNewUsers(countMonthNewUsers());
            vo.setActiveUsers(countActiveUsers(7));
            vo.setUserTypeDistribution(getUserTypeDistribution());
            vo.setUserStatusDistribution(getUserStatusDistribution());
            vo.setGrowthRate(calculateUserGrowthRate(7));
            return vo;
        } catch (Exception e) {
            log.error("Failed to get user statistics overview", e);
            return new UserStatisticsVO();
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<UserStatisticsVO> getUserStatisticsOverviewAsync() {
        return CompletableFuture.completedFuture(getUserStatisticsOverview());
    }

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'registration:' + #startDate + ':' + #endDate")
    public Map<LocalDate, Long> getUserRegistrationTrend(LocalDate startDate, LocalDate endDate) {
        try {
            Map<LocalDate, Long> trend = new LinkedHashMap<>();
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                LocalDateTime dayStart = current.atStartOfDay();
                LocalDateTime dayEnd = current.plusDays(1).atStartOfDay();
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.ge(User::getCreatedAt, dayStart).lt(User::getCreatedAt, dayEnd);
                trend.put(current, userMapper.selectCount(wrapper));
                current = current.plusDays(1);
            }
            return trend;
        } catch (Exception e) {
            log.error("Failed to get registration trend", e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<LocalDate, Long>> getUserRegistrationTrendAsync(Integer days) {
        int safeDays = days == null || days <= 0 ? 30 : days;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        return CompletableFuture.completedFuture(getUserRegistrationTrend(startDate, endDate));
    }

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'type_distribution'")
    public Map<String, Long> getUserTypeDistribution() {
        try {
            List<User> users = userMapper.selectList(null);
            return users.stream().collect(Collectors.groupingBy(
                    user -> user.getUserType() == null ? "UNKNOWN" : user.getUserType(),
                    Collectors.counting()
            ));
        } catch (Exception e) {
            log.error("Failed to get user type distribution", e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<String, Long>> getUserTypeDistributionAsync() {
        return CompletableFuture.completedFuture(getUserTypeDistribution());
    }

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'status_distribution'")
    public Map<String, Long> getUserStatusDistribution() {
        try {
            List<User> users = userMapper.selectList(null);
            Map<String, Long> distribution = new HashMap<>();
            long active = users.stream().filter(u -> u.getStatus() != null && u.getStatus() == 1).count();
            long inactive = users.stream().filter(u -> u.getStatus() == null || u.getStatus() == 0).count();
            distribution.put("active", active);
            distribution.put("inactive", inactive);
            return distribution;
        } catch (Exception e) {
            log.error("Failed to get user status distribution", e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<String, Long>> getUserStatusDistributionAsync() {
        return CompletableFuture.completedFuture(getUserStatusDistribution());
    }

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'active:' + #days")
    public Long countActiveUsers(Integer days) {
        try {
            int safeDays = days == null || days <= 0 ? 7 : days;
            Set<String> keys = scanKeys("user:last_login:*");
            if (keys == null || keys.isEmpty()) {
                return 0L;
            }
            LocalDateTime threshold = LocalDateTime.now().minusDays(safeDays);
            return keys.stream()
                    .map(key -> (LocalDateTime) redisTemplate.opsForValue().get(key))
                    .filter(Objects::nonNull)
                    .filter(loginTime -> loginTime.isAfter(threshold))
                    .count();
        } catch (Exception e) {
            log.error("Failed to count active users", e);
            return 0L;
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> countActiveUsersAsync(Integer days) {
        return CompletableFuture.completedFuture(countActiveUsers(days));
    }

    @Override
    public Long countTodayNewUsers() {
        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(User::getCreatedAt, todayStart);
            return userMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.error("Failed to count today new users", e);
            return 0L;
        }
    }

    @Override
    public Long countMonthNewUsers() {
        try {
            LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(User::getCreatedAt, monthStart);
            return userMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.error("Failed to count month new users", e);
            return 0L;
        }
    }

    @Override
    public Double calculateUserGrowthRate(Integer days) {
        try {
            int safeDays = days == null || days <= 0 ? 7 : days;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime periodStart = now.minusDays(safeDays);
            LocalDateTime previousPeriodStart = now.minusDays(safeDays * 2L);

            LambdaQueryWrapper<User> currentWrapper = new LambdaQueryWrapper<>();
            currentWrapper.ge(User::getCreatedAt, periodStart);
            Long currentCount = userMapper.selectCount(currentWrapper);

            LambdaQueryWrapper<User> previousWrapper = new LambdaQueryWrapper<>();
            previousWrapper.ge(User::getCreatedAt, previousPeriodStart).lt(User::getCreatedAt, periodStart);
            Long previousCount = userMapper.selectCount(previousWrapper);

            if (previousCount == 0) {
                return currentCount > 0 ? 100.0 : 0.0;
            }

            double growthRate = ((currentCount - previousCount) * 100.0) / previousCount;
            return Math.round(growthRate * 100.0) / 100.0;
        } catch (Exception e) {
            log.error("Failed to calculate user growth rate", e);
            return 0.0;
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Double> calculateUserGrowthRateAsync(Integer days) {
        return CompletableFuture.completedFuture(calculateUserGrowthRate(days));
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<Long, Long>> getUserActivityRankingAsync(Integer limit, Integer days) {
        try {
            int safeLimit = limit == null || limit <= 0 ? 10 : limit;
            Set<String> keys = scanKeys("user:activity:*");
            if (keys == null || keys.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyMap());
            }

            Map<Long, Long> activityMap = new HashMap<>();
            for (String key : keys) {
                try {
                    Long userId = Long.parseLong(key.substring(key.lastIndexOf(':') + 1));
                    Long count = (Long) redisTemplate.opsForValue().get(key);
                    if (count != null) {
                        activityMap.put(userId, count);
                    }
                } catch (Exception ignore) {
                    log.warn("Failed to parse activity key: {}", key);
                }
            }

            Map<Long, Long> result = activityMap.entrySet().stream()
                    .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                    .limit(safeLimit)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to get activity ranking", e);
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Boolean> refreshStatisticsCacheAsync() {
        try {
            getUserStatisticsOverview();
            getUserTypeDistribution();
            getUserStatusDistribution();
            countActiveUsers(7);
            countActiveUsers(30);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to refresh statistics cache", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private Set<String> scanKeys(String pattern) {
        return redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return keys;
        });
    }
}
