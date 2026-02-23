package com.cloud.user.service.impl;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.service.UserNotificationService;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendWelcomeEmailAsync(Long userId) {
        try {
            UserDTO user = userService.getUserById(userId);
            if (user == null || user.getEmail() == null) {
                return CompletableFuture.completedFuture(false);
            }
            redisTemplate.opsForValue().set("notification:welcome:" + userId, System.currentTimeMillis());
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send welcome email", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendPasswordResetEmailAsync(Long userId, String resetToken) {
        try {
            UserDTO user = userService.getUserById(userId);
            if (user == null || user.getEmail() == null || resetToken == null || resetToken.isBlank()) {
                return CompletableFuture.completedFuture(false);
            }
            redisTemplate.opsForValue().set("password:reset:token:" + resetToken, userId, 24, TimeUnit.HOURS);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendActivationEmailAsync(Long userId, String activationToken) {
        try {
            UserDTO user = userService.getUserById(userId);
            if (user == null || user.getEmail() == null || activationToken == null || activationToken.isBlank()) {
                return CompletableFuture.completedFuture(false);
            }
            redisTemplate.opsForValue().set("user:activation:token:" + activationToken, userId, 48, TimeUnit.HOURS);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send activation email", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendStatusChangeNotificationAsync(Long userId, Integer newStatus, String reason) {
        try {
            UserDTO user = userService.getUserById(userId);
            if (user == null) {
                return CompletableFuture.completedFuture(false);
            }
            String key = "notification:status_change:" + userId + ":" + System.currentTimeMillis();
            String payload = "status=" + newStatus + ";reason=" + (reason == null ? "" : reason);
            redisTemplate.opsForValue().set(key, payload);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send status change notification", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendBatchNotificationAsync(List<Long> userIds, String title, String content) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                return CompletableFuture.completedFuture(false);
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
            return CompletableFuture.completedFuture(successCount > 0);
        } catch (Exception e) {
            log.error("Failed to send batch notification", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendSystemAnnouncementAsync(String title, String content) {
        try {
            String key = "notification:system:" + System.currentTimeMillis();
            String payload = "title=" + title + ";content=" + content;
            redisTemplate.opsForValue().set(key, payload);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send system announcement", e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
