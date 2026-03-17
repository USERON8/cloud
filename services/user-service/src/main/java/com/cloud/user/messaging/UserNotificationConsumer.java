package com.cloud.user.messaging;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.SystemException;
import com.cloud.common.messaging.consumer.AbstractMqConsumer;
import com.cloud.common.messaging.event.UserNotificationEvent;
import com.cloud.user.service.UserNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
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
public class UserNotificationConsumer extends AbstractMqConsumer<UserNotificationEvent> {

  private static final String NS_USER_NOTIFICATION = "user:notification";
  private static final String PROCESSING_PREFIX = "notification:processing:";
  private static final String DONE_BUCKET_PREFIX = "notification:done:bucket:";
  private static final String INVALID_BUCKET_PREFIX = "notification:invalid:bucket:";
  private static final long PROCESSING_TTL_MINUTES = 5;
  private static final long DONE_TTL_DAYS = 7;
  private static final long INVALID_TTL_DAYS = 7;
  private static final int DONE_LOOKBACK_DAYS = 7;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

  private final UserNotificationService userNotificationService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  protected void doConsume(UserNotificationEvent event, MessageExt msgExt) {
    if (event == null || event.getEventType() == null) {
      return;
    }
    String eventId = event.getEventId();
    if (!StringUtils.hasText(eventId)) {
      log.warn("Notification event missing eventId: eventType={}", event.getEventType());
      return;
    }
    if (isAlreadyProcessed(eventId)) {
      log.debug(
          "Duplicate notification event ignored: eventId={}, eventType={}",
          eventId,
          event.getEventType());
      return;
    }
    if (!markProcessing(eventId)) {
      throw new IllegalStateException("notification processing already in progress");
    }

    try {
      boolean delivered =
          switch (event.getEventType()) {
            case UserNotificationEvent.TYPE_WELCOME -> {
              if (event.getUserId() == null) {
                log.warn("Notification event missing userId: eventId={}", event.getEventId());
                recordInvalid(eventId, "missing userId");
                yield true;
              }
              yield userNotificationService.sendWelcomeEmail(event.getUserId());
            }
            case UserNotificationEvent.TYPE_PASSWORD_RESET -> {
              if (event.getUserId() == null || event.getToken() == null) {
                log.warn(
                    "Notification event missing password reset payload: eventId={}",
                    event.getEventId());
                recordInvalid(eventId, "missing userId or token");
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
                recordInvalid(eventId, "missing userId or token");
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
                recordInvalid(eventId, "missing userId");
                yield true;
              }
              yield userNotificationService.sendStatusChangeNotification(
                  event.getUserId(), event.getNewStatus(), event.getReason());
            }
            case UserNotificationEvent.TYPE_BATCH -> {
              if (event.getUserIds() == null || event.getUserIds().isEmpty()) {
                log.warn(
                    "Notification event missing batch payload: eventId={}", event.getEventId());
                recordInvalid(eventId, "missing userIds");
                yield true;
              }
              yield userNotificationService.sendBatchNotification(
                  event.getUserIds(), event.getTitle(), event.getContent());
            }
            case UserNotificationEvent.TYPE_SYSTEM -> {
              if (event.getTitle() == null || event.getContent() == null) {
                log.warn(
                    "Notification event missing system payload: eventId={}", event.getEventId());
                recordInvalid(eventId, "missing title or content");
                yield true;
              }
              yield userNotificationService.sendSystemAnnouncement(
                  event.getTitle(), event.getContent());
            }
            default -> {
              log.warn("Unknown notification event type: {}", event.getEventType());
              recordInvalid(eventId, "unknown eventType: " + event.getEventType());
              yield true;
            }
          };
      if (!delivered) {
        log.warn(
            "Notification delivery skipped or failed without exception: eventId={}, eventType={}",
            eventId,
            event.getEventType());
      }
      markProcessed(eventId);
    } catch (BizException e) {
      log.warn(
          "Notification event skipped due to biz exception: eventId={}, eventType={}, message={}",
          event.getEventId(),
          event.getEventType(),
          e.getMessage());
      markProcessed(eventId);
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to dispatch notification event: eventId={}, eventType={}",
          event.getEventId(),
          event.getEventType(),
          e);
      throw new SystemException(
          ResultCode.SYSTEM_ERROR, "Failed to dispatch notification event", e);
    } finally {
      clearProcessing(eventId);
    }
  }

  @Override
  protected UserNotificationEvent deserialize(byte[] body) {
    try {
      return body == null ? null : objectMapper.readValue(body, UserNotificationEvent.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to deserialize UserNotificationEvent", ex);
    }
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

  private boolean isAlreadyProcessed(String eventId) {
    try {
      for (int i = 0; i < DONE_LOOKBACK_DAYS; i++) {
        String bucketKey = buildBucketKey(DONE_BUCKET_PREFIX, i);
        Boolean exists = redisTemplate.opsForHash().hasKey(bucketKey, eventId);
        if (Boolean.TRUE.equals(exists)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      log.warn("Check notification processed state failed: eventId={}", eventId, e);
      return false;
    }
  }

  private boolean markProcessing(String eventId) {
    Boolean locked =
        redisTemplate
            .opsForValue()
            .setIfAbsent(
                PROCESSING_PREFIX + eventId, "1", PROCESSING_TTL_MINUTES, TimeUnit.MINUTES);
    return Boolean.TRUE.equals(locked);
  }

  private void markProcessed(String eventId) {
    String bucketKey = buildBucketKey(DONE_BUCKET_PREFIX, 0);
    redisTemplate.opsForHash().put(bucketKey, eventId, "1");
    redisTemplate.expire(bucketKey, DONE_TTL_DAYS, TimeUnit.DAYS);
  }

  private void clearProcessing(String eventId) {
    redisTemplate.delete(PROCESSING_PREFIX + eventId);
  }

  private void recordInvalid(String eventId, String reason) {
    if (!StringUtils.hasText(eventId)) {
      return;
    }
    String bucketKey = buildBucketKey(INVALID_BUCKET_PREFIX, 0);
    redisTemplate.opsForHash().put(bucketKey, eventId, reason);
    redisTemplate.expire(bucketKey, INVALID_TTL_DAYS, TimeUnit.DAYS);
  }

  private String buildBucketKey(String prefix, int offsetDays) {
    LocalDate date = LocalDate.now().minusDays(offsetDays);
    return prefix + date.format(DATE_FORMATTER);
  }
}
