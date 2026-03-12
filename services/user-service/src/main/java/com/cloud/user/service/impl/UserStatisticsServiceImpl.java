package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cloud.common.domain.vo.user.UserStatisticsVO;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserStatisticsService;
import com.cloud.user.service.support.AuthPrincipalRemoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
    private final AuthPrincipalRemoteService authPrincipalRemoteService;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'overview'", unless = "#result == null")
    public UserStatisticsVO getUserStatisticsOverview() {
        try {
            UserStatisticsVO vo = new UserStatisticsVO();
            vo.setTotalUsers(userMapper.selectCount(null));
            vo.setTodayNewUsers(countTodayNewUsers());
            vo.setMonthNewUsers(countMonthNewUsers());
            vo.setActiveUsers(countActiveUsers(7));
            vo.setRoleDistribution(getRoleDistribution());
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
            if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
                return Collections.emptyMap();
            }
            Map<LocalDate, Long> trend = new LinkedHashMap<>();
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                trend.put(current, 0L);
                current = current.plusDays(1);
            }

            LocalDateTime rangeStart = startDate.atStartOfDay();
            LocalDateTime rangeEnd = endDate.plusDays(1).atStartOfDay();
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.select("DATE(created_at) AS day", "COUNT(*) AS cnt")
                    .ge("created_at", rangeStart)
                    .lt("created_at", rangeEnd)
                    .groupBy("DATE(created_at)")
                    .orderByAsc("DATE(created_at)");

            List<Map<String, Object>> rows = userMapper.selectMaps(wrapper);
            for (Map<String, Object> row : rows) {
                Object dayObj = row.get("day");
                Object cntObj = row.get("cnt");
                LocalDate day = null;
                if (dayObj instanceof LocalDate localDate) {
                    day = localDate;
                } else if (dayObj instanceof java.sql.Date sqlDate) {
                    day = sqlDate.toLocalDate();
                } else if (dayObj != null) {
                    try {
                        day = LocalDate.parse(dayObj.toString());
                    } catch (Exception ignore) {
                        log.debug("Failed to parse registration day value: {}", dayObj);
                    }
                }

                Long count = null;
                if (cntObj instanceof Number number) {
                    count = number.longValue();
                } else if (cntObj != null) {
                    try {
                        count = Long.parseLong(cntObj.toString());
                    } catch (Exception ignore) {
                        log.debug("Failed to parse registration count value: {}", cntObj);
                    }
                }

                if (day != null && count != null) {
                    trend.put(day, count);
                }
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
    @Cacheable(cacheNames = "user:statistics", key = "'role_distribution'")
    public Map<String, Long> getRoleDistribution() {
        try {
            return authPrincipalRemoteService.getRoleDistribution();
        } catch (Exception e) {
            log.error("Failed to get role distribution", e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<String, Long>> getRoleDistributionAsync() {
        return CompletableFuture.completedFuture(getRoleDistribution());
    }

    @Override
    @Cacheable(cacheNames = "user:statistics", key = "'status_distribution'")
    public Map<String, Long> getUserStatusDistribution() {
        try {
            Map<String, Long> distribution = new HashMap<>();
            long total = userMapper.selectCount(null);
            long active = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getStatus, 1));
            long inactive = Math.max(total - active, 0L);
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
            List<String> keyList = List.copyOf(keys);
            List<Object> values = redisTemplate.opsForValue().multiGet(keyList);
            if (values == null || values.isEmpty()) {
                return 0L;
            }
            long count = 0L;
            for (Object value : values) {
                LocalDateTime loginTime = null;
                if (value instanceof LocalDateTime time) {
                    loginTime = time;
                } else if (value instanceof java.sql.Timestamp timestamp) {
                    loginTime = timestamp.toLocalDateTime();
                } else if (value instanceof String raw) {
                    try {
                        loginTime = LocalDateTime.parse(raw);
                    } catch (Exception ignore) {
                        log.debug("Failed to parse login time value: {}", raw);
                    }
                }
                if (loginTime != null && loginTime.isAfter(threshold)) {
                    count++;
                }
            }
            return count;
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
            List<String> keyList = List.copyOf(keys);
            List<Object> values = redisTemplate.opsForValue().multiGet(keyList);
            if (values != null && !values.isEmpty()) {
                for (int i = 0; i < keyList.size() && i < values.size(); i++) {
                    String key = keyList.get(i);
                    Object value = values.get(i);
                    try {
                        Long userId = Long.parseLong(key.substring(key.lastIndexOf(':') + 1));
                        Long count = null;
                        if (value instanceof Number number) {
                            count = number.longValue();
                        } else if (value instanceof String raw) {
                            count = Long.parseLong(raw);
                        }
                        if (count != null) {
                            activityMap.put(userId, count);
                        }
                    } catch (Exception ignore) {
                        log.warn("Failed to parse activity key: {}", key);
                    }
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
            UserStatisticsVO overview = getUserStatisticsOverview();
            Map<String, Long> roleDistribution = overview.getRoleDistribution();
            Map<String, Long> statusDistribution = overview.getUserStatusDistribution();
            Long active7 = overview.getActiveUsers();
            Long active30 = countActiveUsers(30);

            Cache cache = cacheManager.getCache("user:statistics");
            if (cache != null) {
                cache.put("overview", overview);
                if (roleDistribution != null) {
                    cache.put("role_distribution", roleDistribution);
                }
                if (statusDistribution != null) {
                    cache.put("status_distribution", statusDistribution);
                }
                if (active7 != null) {
                    cache.put("active:7", active7);
                }
                if (active30 != null) {
                    cache.put("active:30", active30);
                }
            }
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
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return keys;
        });
    }
}
