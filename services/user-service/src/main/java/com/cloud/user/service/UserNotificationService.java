package com.cloud.user.service;

import java.util.concurrent.CompletableFuture;

public interface UserNotificationService {

    boolean sendWelcomeEmail(Long userId);

    boolean sendPasswordResetEmail(Long userId, String resetToken);

    boolean sendActivationEmail(Long userId, String activationToken);

    boolean sendStatusChangeNotification(Long userId, Integer newStatus, String reason);

    boolean sendBatchNotification(java.util.List<Long> userIds, String title, String content);

    boolean sendSystemAnnouncement(String title, String content);

    CompletableFuture<Boolean> sendWelcomeEmailAsync(Long userId);

    CompletableFuture<Boolean> sendPasswordResetEmailAsync(Long userId, String resetToken);

    CompletableFuture<Boolean> sendActivationEmailAsync(Long userId, String activationToken);

    CompletableFuture<Boolean> sendStatusChangeNotificationAsync(Long userId, Integer newStatus, String reason);

    CompletableFuture<Boolean> sendBatchNotificationAsync(java.util.List<Long> userIds, String title, String content);

    CompletableFuture<Boolean> sendSystemAnnouncementAsync(String title, String content);
}
