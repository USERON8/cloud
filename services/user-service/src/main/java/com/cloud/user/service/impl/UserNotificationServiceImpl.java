package com.cloud.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.user.notification.UserNotificationDeliveryProvider;
import com.cloud.user.service.UserNotificationService;
import com.cloud.user.service.UserService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

  private final UserService userService;
  private final UserNotificationDeliveryProvider deliveryProvider;

  @Override
  public boolean sendWelcomeEmail(Long userId) {
    UserDTO user = getUserForNotification(userId);
    if (user == null || StrUtil.isBlank(user.getEmail())) {
      return false;
    }
    return deliveryProvider.deliverWelcome(userId);
  }

  @Override
  public boolean sendPasswordResetEmail(Long userId, String resetToken) {
    UserDTO user = getUserForNotification(userId);
    if (user == null || StrUtil.isBlank(user.getEmail()) || StrUtil.isBlank(resetToken)) {
      return false;
    }
    return deliveryProvider.deliverPasswordResetToken(userId, resetToken.trim());
  }

  @Override
  public boolean sendActivationEmail(Long userId, String activationToken) {
    UserDTO user = getUserForNotification(userId);
    if (user == null || StrUtil.isBlank(user.getEmail()) || StrUtil.isBlank(activationToken)) {
      return false;
    }
    return deliveryProvider.deliverActivationToken(userId, activationToken.trim());
  }

  @Override
  public boolean sendStatusChangeNotification(Long userId, Integer newStatus, String reason) {
    UserDTO user = getUserForNotification(userId);
    if (user == null) {
      return false;
    }
    return deliveryProvider.deliverStatusChange(userId, newStatus, reason);
  }

  @Override
  public boolean sendBatchNotification(List<Long> userIds, String title, String content) {
    if (userIds == null || userIds.isEmpty()) {
      return false;
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
    return successCount > 0;
  }

  @Override
  public boolean sendSystemAnnouncement(String title, String content) {
    return deliveryProvider.deliverSystemAnnouncement(title, content);
  }

  @Override
  @Async("userNotificationExecutor")
  public CompletableFuture<Boolean> sendWelcomeEmailAsync(Long userId) {
    return CompletableFuture.completedFuture(sendWelcomeEmail(userId));
  }

  @Override
  @Async("userNotificationExecutor")
  public CompletableFuture<Boolean> sendPasswordResetEmailAsync(Long userId, String resetToken) {
    return CompletableFuture.completedFuture(sendPasswordResetEmail(userId, resetToken));
  }

  @Override
  @Async("userNotificationExecutor")
  public CompletableFuture<Boolean> sendActivationEmailAsync(Long userId, String activationToken) {
    return CompletableFuture.completedFuture(sendActivationEmail(userId, activationToken));
  }

  @Override
  @Async("userNotificationExecutor")
  public CompletableFuture<Boolean> sendStatusChangeNotificationAsync(
      Long userId, Integer newStatus, String reason) {
    return CompletableFuture.completedFuture(
        sendStatusChangeNotification(userId, newStatus, reason));
  }

  @Override
  @Async("userNotificationExecutor")
  public CompletableFuture<Boolean> sendBatchNotificationAsync(
      List<Long> userIds, String title, String content) {
    return CompletableFuture.completedFuture(sendBatchNotification(userIds, title, content));
  }

  @Override
  @Async("userNotificationExecutor")
  public CompletableFuture<Boolean> sendSystemAnnouncementAsync(String title, String content) {
    return CompletableFuture.completedFuture(sendSystemAnnouncement(title, content));
  }

  private UserDTO getUserForNotification(Long userId) {
    if (userId == null || userId <= 0) {
      return null;
    }
    try {
      return userService.getUserById(userId);
    } catch (EntityNotFoundException e) {
      log.warn("User is not available for notification, userId={}", userId);
      return null;
    } catch (BizException e) {
      log.warn("User is not available for notification, userId={}", userId);
      return null;
    } catch (Exception e) {
      log.error("Failed to load user for notification, userId={}", userId, e);
      throw e;
    }
  }
}
