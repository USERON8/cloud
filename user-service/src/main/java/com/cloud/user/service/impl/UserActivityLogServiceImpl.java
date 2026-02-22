package com.cloud.user.service.impl;

import com.cloud.user.module.enums.UserActivityType;
import com.cloud.user.service.UserActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;







@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogServiceImpl implements UserActivityLogService {

    


    private static final int LOG_RETENTION_DAYS = 90;
    private final RedisTemplate<String, Object> redisTemplate;
    @Resource
    @Qualifier("userLogExecutor")
    private Executor userLogExecutor;
    @Resource
    @Qualifier("userStatisticsExecutor")
    private Executor userStatisticsExecutor;

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logLoginActivityAsync(Long userId, String ip, String device) {
        log.debug("璁板綍鐢ㄦ埛鐧诲綍琛屼负锛寀serId: {}, ip: {}", userId, ip);

        return CompletableFuture.supplyAsync(() -> {
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

                
                String lastLoginKey = "user:last_login:" + userId;
                redisTemplate.opsForValue().set(lastLoginKey, LocalDateTime.now());

                
                incrementActivityScore(userId);

                return true;

            } catch (Exception e) {
                log.error("璁板綍鐢ㄦ埛鐧诲綍琛屼负澶辫触: userId={}", userId, e);
                return false;
            }
        }, userLogExecutor);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logLogoutActivityAsync(Long userId) {
        log.debug("璁板綍鐢ㄦ埛鐧诲嚭琛屼负锛寀serId: {}", userId);

        return logActivityAsync(userId, UserActivityType.LOGOUT, "鐢ㄦ埛鐧诲嚭", null);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logRegistrationActivityAsync(Long userId, String registrationType) {
        log.debug("璁板綍鐢ㄦ埛娉ㄥ唽琛屼负锛寀serId: {}, type: {}", userId, registrationType);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("registrationType", registrationType);

        return logActivityAsync(userId, UserActivityType.REGISTRATION, "鐢ㄦ埛娉ㄥ唽", metadata);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logProfileUpdateActivityAsync(Long userId, List<String> modifiedFields) {
        log.debug("璁板綍鐢ㄦ埛淇℃伅淇敼琛屼负锛寀serId: {}, fields: {}", userId, modifiedFields);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modifiedFields", modifiedFields);

        return logActivityAsync(userId, UserActivityType.PROFILE_UPDATE, "淇敼涓汉淇℃伅", metadata);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logPasswordChangeActivityAsync(Long userId) {
        log.debug("璁板綍瀵嗙爜淇敼琛屼负锛寀serId: {}", userId);

        return logActivityAsync(userId, UserActivityType.PASSWORD_CHANGE, "淇敼瀵嗙爜", null);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logActivityAsync(Long userId, UserActivityType activityType,
                                                       String description, Map<String, Object> metadata) {
        log.debug("璁板綍鐢ㄦ埛琛屼负锛寀serId: {}, type: {}", userId, activityType);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> logData = new HashMap<>();
                logData.put("userId", userId);
                logData.put("activityType", activityType.name());
                logData.put("description", description);
                logData.put("metadata", metadata);
                logData.put("timestamp", LocalDateTime.now().toString());

                
                String userActivityKey = "user:activity:all:" + userId;
                redisTemplate.opsForList().leftPush(userActivityKey, logData);
                redisTemplate.opsForList().trim(userActivityKey, 0, 999); 
                redisTemplate.expire(userActivityKey, LOG_RETENTION_DAYS, TimeUnit.DAYS);

                
                String typeKey = "activity:type:" + activityType.name() + ":" + userId;
                redisTemplate.opsForList().leftPush(typeKey, logData);
                redisTemplate.expire(typeKey, LOG_RETENTION_DAYS, TimeUnit.DAYS);

                
                incrementActivityScore(userId);

                log.debug("鐢ㄦ埛琛屼负璁板綍鎴愬姛: userId={}, type={}", userId, activityType);
                return true;

            } catch (Exception e) {
                log.error("璁板綍鐢ㄦ埛琛屼负澶辫触: userId={}, type={}", userId, activityType, e);
                return false;
            }
        }, userLogExecutor);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<List<Map<String, Object>>> getRecentActivitiesAsync(Long userId, Integer limit) {
        log.debug("鑾峰彇鐢ㄦ埛鏈€杩戞椿鍔紝userId: {}, limit: {}", userId, limit);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "user:activity:all:" + userId;
                List<Object> activities = redisTemplate.opsForList().range(key, 0, limit - 1);

                if (activities == null || activities.isEmpty()) {
                    return Collections.emptyList();
                }

                List<Map<String, Object>> result = new ArrayList<>();
                for (Object activity : activities) {
                    if (activity instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> activityMap = (Map<String, Object>) activity;
                        result.add(activityMap);
                    }
                }

                return result;

            } catch (Exception e) {
                log.error("鑾峰彇鐢ㄦ埛鏈€杩戞椿鍔ㄥけ璐? userId={}", userId, e);
                return Collections.emptyList();
            }
        }, userLogExecutor);
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> calculateUserActivityScoreAsync(Long userId, Integer days) {
        log.debug("璁＄畻鐢ㄦ埛娲昏穬搴︼紝userId: {}, days: {}", userId, days);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "user:activity:score:" + userId;
                Object score = redisTemplate.opsForValue().get(key);

                return score != null ? Long.parseLong(score.toString()) : 0L;

            } catch (Exception e) {
                log.error("璁＄畻鐢ㄦ埛娲昏穬搴﹀け璐? userId={}", userId, e);
                return 0L;
            }
        }, userStatisticsExecutor);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logBatchActivitiesAsync(List<Map<String, Object>> activities) {
        

        return CompletableFuture.supplyAsync(() -> {
            try {
                int successCount = 0;

                for (Map<String, Object> activity : activities) {
                    try {
                        Long userId = (Long) activity.get("userId");
                        String activityTypeStr = (String) activity.get("activityType");
                        UserActivityType activityType = UserActivityType.valueOf(activityTypeStr);
                        String description = (String) activity.get("description");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = (Map<String, Object>) activity.get("metadata");

                        logActivityAsync(userId, activityType, description, metadata).join();
                        successCount++;

                    } catch (Exception e) {
                        log.warn("鎵归噺璁板綍鍗曟潯娲诲姩澶辫触", e);
                    }
                }

                
                return successCount > 0;

            } catch (Exception e) {
                log.error("鎵归噺璁板綍鐢ㄦ埛娲诲姩澶辫触", e);
                return false;
            }
        }, userLogExecutor);
    }

    


    private void incrementActivityScore(Long userId) {
        try {
            String key = "user:activity:score:" + userId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("澧炲姞鐢ㄦ埛娲昏穬搴﹀垎鏁板け璐? userId={}", userId);
        }
    }
}
