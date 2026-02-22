package com.cloud.user.service.impl;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.service.UserNotificationService;
import com.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Resource
    @Qualifier("userNotificationExecutor")
    private Executor userNotificationExecutor;

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
                log.error("Failed to send welcome email", e);
                return false;
            }
        }, userNotificationExecutor);
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendPasswordResetEmailAsync(Long userId, String resetToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserDTO user = userService.getUserById(userId);
                if (user == null || user.getEmail() == null || resetToken == null || resetToken.isBlank()) {
                    return false;
                }
                redisTemplate.opsForValue().set("password:reset:token:" + resetToken, userId, 24, TimeUnit.HOURS);
                return true;
            } catch (Exception e) {
                log.error("Failed to send password reset email", e);
                return false;
            }
        }, userNotificationExecutor);
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendActivationEmailAsync(Long userId, String activationToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserDTO user = userService.getUserById(userId);
                if (user == null || user.getEmail() == null || activationToken == null || activationToken.isBlank()) {
                    return false;
                }
                redisTemplate.opsForValue().set("user:activation:token:" + activationToken, userId, 48, TimeUnit.HOURS);
                return true;
            } catch (Exception e) {
                log.error("Failed to send activation email", e);
                return false;
            }
        }, userNotificationExecutor);
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendStatusChangeNotificationAsync(Long userId, Integer newStatus, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserDTO user = userService.getUserById(userId);
                if (user == null) {
                    return false;
                }
                String key = "notification:status_change:" + userId + ":" + System.currentTimeMillis();
                String payload = "status=" + newStatus + ";reason=" + (reason == null ? "" : reason);
                redisTemplate.opsForValue().set(key, payload);
                return true;
            } catch (Exception e) {
                log.error("Failed to send status change notification", e);
                return false;
            }
        }, userNotificationExecutor);
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendBatchNotificationAsync(List<Long> userIds, String title, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (userIds == null || userIds.isEmpty()) {
                    return false;
                }
                int successCount = 0;
                for (Long userId : userIds) {
                    UserDTO user = userService.getUserById(userId);
                    if (user == null) {
                        continue;
                    }
                    String key = "notification:batch:" + userId + ":" + System.currentTimeMillis();
                    String payload = "title=" + title + ";content=" + content;
                    redisTemplate.opsForValue().set(key, payload);
                    successCount++;
                }
                return successCount > 0;
            } catch (Exception e) {
                log.error("Failed to send batch notification", e);
                return false;
            }
        }, userNotificationExecutor);
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendSystemAnnouncementAsync(String title, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "notification:system:" + System.currentTimeMillis();
                String payload = "title=" + title + ";content=" + content;
                redisTemplate.opsForValue().set(key, payload);
                return true;
            } catch (Exception e) {
                log.error("Failed to send system announcement", e);
                return false;
            }
        }, userNotificationExecutor);
    }
}