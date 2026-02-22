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

/**
 * 用户行为日志服务实现类
 * 使用Redis存储用户行为日志，支持高并发写入
 *
 * @author what's up
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogServiceImpl implements UserActivityLogService {

    /**
     * 日志保留天数
     */
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
        log.debug("记录用户登录行为，userId: {}, ip: {}", userId, ip);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> logData = new HashMap<>();
                logData.put("userId", userId);
                logData.put("activityType", UserActivityType.LOGIN.name());
                logData.put("ip", ip);
                logData.put("device", device);
                logData.put("timestamp", LocalDateTime.now().toString());

                // 存储到Redis List（最近100条）
                String key = "user:activity:login:" + userId;
                redisTemplate.opsForList().leftPush(key, logData);
                redisTemplate.opsForList().trim(key, 0, 99);
                redisTemplate.expire(key, LOG_RETENTION_DAYS, TimeUnit.DAYS);

                // 更新最后登录时间
                String lastLoginKey = "user:last_login:" + userId;
                redisTemplate.opsForValue().set(lastLoginKey, LocalDateTime.now());

                // 增加活跃度分数
                incrementActivityScore(userId);

                return true;

            } catch (Exception e) {
                log.error("记录用户登录行为失败: userId={}", userId, e);
                return false;
            }
        }, userLogExecutor);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logLogoutActivityAsync(Long userId) {
        log.debug("记录用户登出行为，userId: {}", userId);

        return logActivityAsync(userId, UserActivityType.LOGOUT, "用户登出", null);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logRegistrationActivityAsync(Long userId, String registrationType) {
        log.debug("记录用户注册行为，userId: {}, type: {}", userId, registrationType);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("registrationType", registrationType);

        return logActivityAsync(userId, UserActivityType.REGISTRATION, "用户注册", metadata);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logProfileUpdateActivityAsync(Long userId, List<String> modifiedFields) {
        log.debug("记录用户信息修改行为，userId: {}, fields: {}", userId, modifiedFields);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modifiedFields", modifiedFields);

        return logActivityAsync(userId, UserActivityType.PROFILE_UPDATE, "修改个人信息", metadata);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logPasswordChangeActivityAsync(Long userId) {
        log.debug("记录密码修改行为，userId: {}", userId);

        return logActivityAsync(userId, UserActivityType.PASSWORD_CHANGE, "修改密码", null);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logActivityAsync(Long userId, UserActivityType activityType,
                                                       String description, Map<String, Object> metadata) {
        log.debug("记录用户行为，userId: {}, type: {}", userId, activityType);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> logData = new HashMap<>();
                logData.put("userId", userId);
                logData.put("activityType", activityType.name());
                logData.put("description", description);
                logData.put("metadata", metadata);
                logData.put("timestamp", LocalDateTime.now().toString());

                // 存储到用户活动列表
                String userActivityKey = "user:activity:all:" + userId;
                redisTemplate.opsForList().leftPush(userActivityKey, logData);
                redisTemplate.opsForList().trim(userActivityKey, 0, 999); // 保留最近1000条
                redisTemplate.expire(userActivityKey, LOG_RETENTION_DAYS, TimeUnit.DAYS);

                // 存储到活动类型列表
                String typeKey = "activity:type:" + activityType.name() + ":" + userId;
                redisTemplate.opsForList().leftPush(typeKey, logData);
                redisTemplate.expire(typeKey, LOG_RETENTION_DAYS, TimeUnit.DAYS);

                // 增加活跃度分数
                incrementActivityScore(userId);

                log.debug("用户行为记录成功: userId={}, type={}", userId, activityType);
                return true;

            } catch (Exception e) {
                log.error("记录用户行为失败: userId={}, type={}", userId, activityType, e);
                return false;
            }
        }, userLogExecutor);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<List<Map<String, Object>>> getRecentActivitiesAsync(Long userId, Integer limit) {
        log.debug("获取用户最近活动，userId: {}, limit: {}", userId, limit);

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
                log.error("获取用户最近活动失败: userId={}", userId, e);
                return Collections.emptyList();
            }
        }, userLogExecutor);
    }

    @Override
    @Async("userStatisticsExecutor")
    public CompletableFuture<Long> calculateUserActivityScoreAsync(Long userId, Integer days) {
        log.debug("计算用户活跃度，userId: {}, days: {}", userId, days);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "user:activity:score:" + userId;
                Object score = redisTemplate.opsForValue().get(key);

                return score != null ? Long.parseLong(score.toString()) : 0L;

            } catch (Exception e) {
                log.error("计算用户活跃度失败: userId={}", userId, e);
                return 0L;
            }
        }, userStatisticsExecutor);
    }

    @Override
    @Async("userLogExecutor")
    public CompletableFuture<Boolean> logBatchActivitiesAsync(List<Map<String, Object>> activities) {
        log.info("批量记录用户活动，数量: {}", activities.size());

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
                        log.warn("批量记录单条活动失败", e);
                    }
                }

                log.info("批量记录用户活动完成，总数: {}, 成功: {}", activities.size(), successCount);
                return successCount > 0;

            } catch (Exception e) {
                log.error("批量记录用户活动失败", e);
                return false;
            }
        }, userLogExecutor);
    }

    /**
     * 增加用户活跃度分数
     */
    private void incrementActivityScore(Long userId) {
        try {
            String key = "user:activity:score:" + userId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("增加用户活跃度分数失败: userId={}", userId);
        }
    }
}
