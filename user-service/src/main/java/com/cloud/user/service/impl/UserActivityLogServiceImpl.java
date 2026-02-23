package com.cloud.user.service.impl;

import com.cloud.user.module.enums.UserActivityType;
import com.cloud.user.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogServiceImpl implements UserActivityLogService {

    private static final int LOG_RETENTION_DAYS = 90;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logLoginActivityAsync(Long userId, String ip, String device) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("userId", userId);
            logData.put("activityType", UserActivityType.LOGIN.name());
            logData.put("ip", ip);
            logData.put("device", device);
            logData.put("timestamp", LocalDateTime.now().toString());

            String key = "user:activity:login:" + userId;
            redisTemplate.opsForList().leftPush(key, logData);
            redisTemplate.opsForList().trim(key, 0, 99);
            redisTemplate.expire(key, LOG_RETENTION_DAYS, TimeUnit.DAYS);

            redisTemplate.opsForValue().set("user:last_login:" + userId, LocalDateTime.now());
            incrementActivityScore(userId);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to log login activity, userId={}", userId, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logLogoutActivityAsync(Long userId) {
        return logActivityAsync(userId, UserActivityType.LOGOUT, UserActivityType.LOGOUT.getDescription(), null);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logRegistrationActivityAsync(Long userId, String registrationType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("registrationType", registrationType);
        return logActivityAsync(userId, UserActivityType.REGISTRATION, UserActivityType.REGISTRATION.getDescription(), metadata);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logProfileUpdateActivityAsync(Long userId, List<String> modifiedFields) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modifiedFields", modifiedFields);
        return logActivityAsync(userId, UserActivityType.PROFILE_UPDATE, UserActivityType.PROFILE_UPDATE.getDescription(), metadata);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logPasswordChangeActivityAsync(Long userId) {
        return logActivityAsync(userId, UserActivityType.PASSWORD_CHANGE, UserActivityType.PASSWORD_CHANGE.getDescription(), null);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logActivityAsync(Long userId, UserActivityType activityType,
                                                       String description, Map<String, Object> metadata) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("userId", userId);
            logData.put("activityType", activityType.name());
            logData.put("description", description);
            logData.put("metadata", metadata);
            logData.put("timestamp", LocalDateTime.now().toString());

            String allActivityKey = "user:activity:all:" + userId;
            redisTemplate.opsForList().leftPush(allActivityKey, logData);
            redisTemplate.opsForList().trim(allActivityKey, 0, 999);
            redisTemplate.expire(allActivityKey, LOG_RETENTION_DAYS, TimeUnit.DAYS);

            String typeKey = "activity:type:" + activityType.name() + ":" + userId;
            redisTemplate.opsForList().leftPush(typeKey, logData);
            redisTemplate.expire(typeKey, LOG_RETENTION_DAYS, TimeUnit.DAYS);

            incrementActivityScore(userId);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to log activity, userId={}, activityType={}", userId, activityType, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<List<Map<String, Object>>> getRecentActivitiesAsync(Long userId, Integer limit) {
        try {
            int safeLimit = limit == null || limit <= 0 ? 20 : limit;
            String key = "user:activity:all:" + userId;
            List<Object> activities = redisTemplate.opsForList().range(key, 0, safeLimit - 1);
            if (activities == null || activities.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object activity : activities) {
                if (activity instanceof Map<?, ?> activityMap) {
                    Map<String, Object> item = new HashMap<>();
                    activityMap.forEach((k, v) -> item.put(String.valueOf(k), v));
                    result.add(item);
                }
            }
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to query recent activities, userId={}", userId, e);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> calculateUserActivityScoreAsync(Long userId, Integer days) {
        try {
            Object score = redisTemplate.opsForValue().get("user:activity:score:" + userId);
            long value = score == null ? 0L : Long.parseLong(score.toString());
            return CompletableFuture.completedFuture(value);
        } catch (Exception e) {
            log.error("Failed to calculate user activity score, userId={}", userId, e);
            return CompletableFuture.completedFuture(0L);
        }
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logBatchActivitiesAsync(List<Map<String, Object>> activities) {
        if (activities == null || activities.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        int successCount = 0;
        for (Map<String, Object> activity : activities) {
            try {
                Long userId = (Long) activity.get("userId");
                String activityTypeStr = String.valueOf(activity.get("activityType"));
                UserActivityType activityType = UserActivityType.valueOf(activityTypeStr);
                String description = (String) activity.get("description");
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) activity.get("metadata");

                if (Boolean.TRUE.equals(logActivityAsync(userId, activityType, description, metadata).join())) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to log one activity in batch", e);
            }
        }
        return CompletableFuture.completedFuture(successCount > 0);
    }

    private void incrementActivityScore(Long userId) {
        try {
            String key = "user:activity:score:" + userId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Failed to increment user activity score, userId={}", userId, e);
        }
    }
}
