package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.user.converter.UserConverter;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.UserAsyncService;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
public class UserAsyncServiceImpl implements UserAsyncService {

    private static final int BATCH_SIZE = 50;

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

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
        List<UserDTO> result = new ArrayList<>();
        for (int i = 0; i < allIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allIds.size());
            List<Long> batch = allIds.subList(i, end);
            List<UserDTO> batchResult = userService.getUsersByIds(batch);
            if (batchResult != null && !batchResult.isEmpty()) {
                result.addAll(batchResult);
            }
        }
        return CompletableFuture.completedFuture(result);
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
        return CompletableFuture.completedFuture(userService.getUserById(userId));
    }

    @Override
    @Async("userQueryExecutor")
    public CompletableFuture<Map<String, Boolean>> checkUsernamesExistAsync(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String username : usernames) {
            result.put(username, userService.findByUsername(username) != null);
        }
        return CompletableFuture.completedFuture(result);
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
        try {
            if (userIds == null || userIds.isEmpty()) {
                return CompletableFuture.completedFuture(true);
            }
            LocalDateTime now = LocalDateTime.now();
            for (Long userId : userIds) {
                redisTemplate.opsForValue().set("user:last_login:" + userId, now);
            }
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to update last login time asynchronously", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Void> refreshUserCacheAsync(Long userId) {
        refreshUserCacheDirect(userId);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Void> refreshUserCacheAsync(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        userIds.stream()
                .filter(Objects::nonNull)
                .forEach(this::refreshUserCacheDirect);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async("userCommonAsyncExecutor")
    public CompletableFuture<Integer> preloadPopularUsersAsync(Integer limit) {
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
            return CompletableFuture.completedFuture(users.size());
        } catch (Exception e) {
            log.error("Failed to preload popular users", e);
            return CompletableFuture.completedFuture(0);
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> countUsersAsync() {
        try {
            return CompletableFuture.completedFuture(userService.count());
        } catch (Exception e) {
            log.error("Failed to count users asynchronously", e);
            return CompletableFuture.completedFuture(0L);
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> countActiveUsersAsync(Integer days) {
        try {
            int safeDays = days == null || days <= 0 ? 7 : days;
            Set<String> keys = scanKeys("user:last_login:*");
            if (keys == null || keys.isEmpty()) {
                return CompletableFuture.completedFuture(0L);
            }
            LocalDateTime threshold = LocalDateTime.now().minusDays(safeDays);
            long count = keys.stream()
                    .map(key -> (LocalDateTime) redisTemplate.opsForValue().get(key))
                    .filter(Objects::nonNull)
                    .filter(time -> time.isAfter(threshold))
                    .count();
            return CompletableFuture.completedFuture(count);
        } catch (Exception e) {
            log.error("Failed to count active users asynchronously", e);
            return CompletableFuture.completedFuture(0L);
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Map<String, Long>> getUserGrowthTrendAsync(Integer days) {
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
            return CompletableFuture.completedFuture(trend);
        } catch (Exception e) {
            log.error("Failed to get user growth trend asynchronously", e);
            return CompletableFuture.completedFuture(Collections.emptyMap());
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

    private void refreshUserCacheDirect(Long userId) {
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
    }
}
