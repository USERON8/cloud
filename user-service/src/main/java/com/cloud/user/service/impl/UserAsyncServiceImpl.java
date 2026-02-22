package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserAsyncService;
import com.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAsyncServiceImpl implements UserAsyncService {

    private static final int BATCH_SIZE = 50;

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Resource
    @Qualifier("userQueryExecutor")
    private Executor userQueryExecutor;

    @Resource
    @Qualifier("userOperationExecutor")
    private Executor userOperationExecutor;

    @Resource
    @Qualifier("userNotificationExecutor")
    private Executor userNotificationExecutor;

    @Resource
    @Qualifier("userCommonAsyncExecutor")
    private Executor userCommonAsyncExecutor;

    @Resource
    @Qualifier("userStatisticsExecutor")
    private Executor userStatisticsExecutor;

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<List<UserDTO>> getUsersByIdsAsync(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        if (userIds.size() <= BATCH_SIZE) {
            return CompletableFuture.completedFuture(userService.getUsersByIds(userIds));
        }

        List<Long> allIds = new ArrayList<>(userIds);
        List<CompletableFuture<List<UserDTO>>> futures = new ArrayList<>();
        for (int i = 0; i < allIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allIds.size());
            List<Long> batch = allIds.subList(i, end);
            futures.add(CompletableFuture.supplyAsync(() -> userService.getUsersByIds(batch), userQueryExecutor));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).flatMap(List::stream).toList());
    }

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<List<UserVO>> getUserVOsByIdsAsync(Collection<Long> userIds) {
        return getUsersByIdsAsync(userIds)
                .thenApply(userConverter::dtoToVOList)
                .exceptionally(ex -> {
                    log.error("Failed to get user VO list asynchronously", ex);
                    return Collections.emptyList();
                });
    }

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<UserDTO> getUserByIdAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> userService.getUserById(userId), userQueryExecutor);
    }

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<Map<String, Boolean>> checkUsernamesExistAsync(List<String> usernames) {
        return CompletableFuture.supplyAsync(() -> {
            if (usernames == null || usernames.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Boolean> result = new LinkedHashMap<>();
            for (String username : usernames) {
                result.put(username, userService.findByUsername(username) != null);
            }
            return result;
        }, userQueryExecutor);
    }

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<Map<Long, Boolean>> checkUsersActiveAsync(Collection<Long> userIds) {
        return getUsersByIdsAsync(userIds)
                .thenApply(users -> users.stream().collect(Collectors.toMap(
                        UserDTO::getId,
                        u -> u.getStatus() != null && u.getStatus() == 1
                )))
                .exceptionally(ex -> {
                    log.error("Failed to check user active status asynchronously", ex);
                    return Collections.emptyMap();
                });
    }

    @Override
    @Async("userOperationExecutor")
    public CompletableFuture<Boolean> updateLastLoginTimeAsync(Collection<Long> userIds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (userIds == null || userIds.isEmpty()) {
                    return true;
                }
                LocalDateTime now = LocalDateTime.now();
                for (Long userId : userIds) {
                    redisTemplate.opsForValue().set("user:last_login:" + userId, now);
                }
                return true;
            } catch (Exception e) {
                log.error("Failed to update last login time asynchronously", e);
                return false;
            }
        }, userOperationExecutor);
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendWelcomeEmailAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserDTO user = userService.getUserById(userId);
                if (user == null || user.getEmail() == null) {
                    return false;
                }
                redisTemplate.opsForValue().set("notification:welcome:" + userId, System.currentTimeMillis());
                return true;
            } catch (Exception e) {
                log.error("Failed to send welcome email asynchronously", e);
                return false;
            }
        }, userNotificationExecutor);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Void> refreshUserCacheAsync(Long userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Cache userCache = cacheManager.getCache("user");
                Cache userInfoCache = cacheManager.getCache("userInfo");
                if (userCache != null) {
                    userCache.evict(userId);
                }
                if (userInfoCache != null) {
                    userInfoCache.evict(userId);
                }
                userService.getUserById(userId);
            } catch (Exception e) {
                log.error("Failed to refresh user cache asynchronously, userId={}", userId, e);
            }
        }, userCommonAsyncExecutor);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Void> refreshUserCacheAsync(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<CompletableFuture<Void>> futures = userIds.stream().map(this::refreshUserCacheAsync).toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Integer> preloadPopularUsersAsync(Integer limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int safeLimit = limit == null || limit <= 0 ? 100 : limit;
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getStatus, 1).orderByDesc(User::getCreatedAt).last("LIMIT " + safeLimit);
                List<User> users = userMapper.selectList(wrapper);
                for (User user : users) {
                    try {
                        userService.getUserById(user.getId());
                    } catch (Exception e) {
                        log.warn("Failed to preload user cache, userId={}", user.getId(), e);
                    }
                }
                return users.size();
            } catch (Exception e) {
                log.error("Failed to preload popular users", e);
                return 0;
            }
        }, userCommonAsyncExecutor);
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> countUsersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.count();
            } catch (Exception e) {
                log.error("Failed to count users asynchronously", e);
                return 0L;
            }
        }, userStatisticsExecutor);
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> countActiveUsersAsync(Integer days) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int safeDays = days == null || days <= 0 ? 7 : days;
                Set<String> keys = redisTemplate.keys("user:last_login:*");
                if (keys == null || keys.isEmpty()) {
                    return 0L;
                }
                LocalDateTime threshold = LocalDateTime.now().minusDays(safeDays);
                return keys.stream()
                        .map(key -> (LocalDateTime) redisTemplate.opsForValue().get(key))
                        .filter(Objects::nonNull)
                        .filter(time -> time.isAfter(threshold))
                        .count();
            } catch (Exception e) {
                log.error("Failed to count active users asynchronously", e);
                return 0L;
            }
        }, userStatisticsExecutor);
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<String, Long>> getUserGrowthTrendAsync(Integer days) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int safeDays = days == null || days <= 0 ? 7 : days;
                Map<String, Long> trend = new LinkedHashMap<>();
                LocalDateTime now = LocalDateTime.now();
                for (int i = safeDays - 1; i >= 0; i--) {
                    LocalDateTime date = now.minusDays(i);
                    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                    wrapper.ge(User::getCreatedAt, date.toLocalDate().atStartOfDay())
                            .lt(User::getCreatedAt, date.toLocalDate().plusDays(1).atStartOfDay());
                    long count = userMapper.selectCount(wrapper);
                    trend.put(date.toLocalDate().toString(), count);
                }
                return trend;
            } catch (Exception e) {
                log.error("Failed to get user growth trend asynchronously", e);
                return Collections.emptyMap();
            }
        }, userStatisticsExecutor);
    }
}