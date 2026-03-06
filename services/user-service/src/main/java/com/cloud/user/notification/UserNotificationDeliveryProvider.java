package com.cloud.user.notification;

public interface UserNotificationDeliveryProvider {

    boolean deliverWelcome(Long userId);

    boolean deliverPasswordResetToken(Long userId, String resetToken);

    boolean deliverActivationToken(Long userId, String activationToken);

    boolean deliverStatusChange(Long userId, Integer newStatus, String reason);

    boolean deliverBatchNotification(Long userId, String title, String content);

    boolean deliverSystemAnnouncement(String title, String content);
}
