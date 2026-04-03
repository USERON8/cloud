package com.cloud.user.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.UserNotificationEvent;
import com.cloud.user.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "user.notification.enabled",
    havingValue = "true",
    matchIfMissing = true)
@RocketMQMessageListener(topic = "user-notification", consumerGroup = "user-notification-consumer")
public class UserNotificationConsumer extends AbstractJsonMqConsumer<UserNotificationEvent> {

  private static final String NS_USER_NOTIFICATION = "user:notification";

  private final UserNotificationService userNotificationService;

  @Override
  protected void doConsume(UserNotificationEvent event, MessageExt msgExt) {
    if (event == null || event.getEventType() == null) {
      return;
    }

    boolean delivered =
        switch (event.getEventType()) {
          case UserNotificationEvent.TYPE_WELCOME -> {
            if (event.getUserId() == null) {
              log.warn("Notification event missing userId: eventId={}", event.getEventId());
              yield true;
            }
            yield userNotificationService.sendWelcomeEmail(event.getUserId());
          }
          case UserNotificationEvent.TYPE_PASSWORD_RESET -> {
            if (event.getUserId() == null || event.getToken() == null) {
              log.warn(
                  "Notification event missing password reset payload: eventId={}",
                  event.getEventId());
              yield true;
            }
            yield userNotificationService.sendPasswordResetEmail(
                event.getUserId(), event.getToken());
          }
          case UserNotificationEvent.TYPE_ACTIVATION -> {
            if (event.getUserId() == null || event.getToken() == null) {
              log.warn(
                  "Notification event missing activation payload: eventId={}",
                  event.getEventId());
              yield true;
            }
            yield userNotificationService.sendActivationEmail(
                event.getUserId(), event.getToken());
          }
          case UserNotificationEvent.TYPE_STATUS_CHANGE -> {
            if (event.getUserId() == null) {
              log.warn(
                  "Notification event missing status change payload: eventId={}",
                  event.getEventId());
              yield true;
            }
            yield userNotificationService.sendStatusChangeNotification(
                event.getUserId(), event.getNewStatus(), event.getReason());
          }
          case UserNotificationEvent.TYPE_BATCH -> {
            if (event.getUserIds() == null || event.getUserIds().isEmpty()) {
              log.warn(
                  "Notification event missing batch payload: eventId={}", event.getEventId());
              yield true;
            }
            yield userNotificationService.sendBatchNotification(
                event.getUserIds(), event.getTitle(), event.getContent());
          }
          case UserNotificationEvent.TYPE_SYSTEM -> {
            if (event.getTitle() == null || event.getContent() == null) {
              log.warn(
                  "Notification event missing system payload: eventId={}", event.getEventId());
              yield true;
            }
            yield userNotificationService.sendSystemAnnouncement(
                event.getTitle(), event.getContent());
          }
          default -> {
            log.warn("Unknown notification event type: {}", event.getEventType());
            yield true;
          }
        };

    if (!delivered) {
      log.warn(
          "Notification delivery skipped or failed: eventId={}, eventType={}",
          event.getEventId(),
          event.getEventType());
    }
  }

  @Override
  protected Class<UserNotificationEvent> payloadClass() {
    return UserNotificationEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "UserNotificationEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, UserNotificationEvent payload) {
    return NS_USER_NOTIFICATION;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, UserNotificationEvent payload, MessageExt msgExt) {
    if (payload != null && StringUtils.hasText(payload.getEventId())) {
      return payload.getEventId();
    }
    return msgId == null ? "" : msgId;
  }
}
