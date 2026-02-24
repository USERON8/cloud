package com.cloud.user.service.impl;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.user.notification.UserNotificationDeliveryProvider;
import com.cloud.user.service.UserNotificationService;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserService userService;
    private final UserNotificationDeliveryProvider deliveryProvider;

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendWelcomeEmailAsync(Long userId) {
        try {
            UserDTO user = getUserForNotification(userId);
            if (user == null || !StringUtils.hasText(user.getEmail())) {
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(deliveryProvider.deliverWelcome(userId));
        } catch (Exception e) {
            log.error("Failed to send welcome email", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendPasswordResetEmailAsync(Long userId, String resetToken) {
        try {
            UserDTO user = getUserForNotification(userId);
            if (user == null || !StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(resetToken)) {
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(
                    deliveryProvider.deliverPasswordResetToken(userId, resetToken.trim())
            );
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendActivationEmailAsync(Long userId, String activationToken) {
        try {
            UserDTO user = getUserForNotification(userId);
            if (user == null || !StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(activationToken)) {
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(
                    deliveryProvider.deliverActivationToken(userId, activationToken.trim())
            );
        } catch (Exception e) {
            log.error("Failed to send activation email", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    @Async("userNotificationExecutor")
    public CompletableFuture<Boolean> sendStatusChangeNotificationAsync(Long userId, Integer newStatus, String reason) {
        try {
            UserDTO user = getUserForNotification(userId);
            if (user == null) {
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(
                    deliveryProvider.deliverStatusChange(userId, newStatus, reason)
            );
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
                UserDTO user = getUserForNotification(userId);
                if (user == null) {
                    continue;
                }
                if (deliveryProvider.deliverBatchNotification(userId, title, content)) {
                    successCount++;
                }
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
            return CompletableFuture.completedFuture(
                    deliveryProvider.deliverSystemAnnouncement(title, content)
            );
        } catch (Exception e) {
            log.error("Failed to send system announcement", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private UserDTO getUserForNotification(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        try {
            return userService.getUserById(userId);
        } catch (Exception e) {
            log.warn("User is not available for notification, userId={}", userId);
            return null;
        }
    }
}
