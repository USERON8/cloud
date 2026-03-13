package com.cloud.user.messaging;

import com.cloud.common.messaging.event.UserNotificationEvent;
import com.cloud.user.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user.notification.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class UserNotificationConsumerConfig {

    private static final String PROCESSING_PREFIX = "notification:processing:";
    private static final String DONE_PREFIX = "notification:done:";
    private static final String DONE_BUCKET_PREFIX = "notification:done:bucket:";
    private static final String INVALID_BUCKET_PREFIX = "notification:invalid:bucket:";
    private static final long PROCESSING_TTL_MINUTES = 5;
    private static final long DONE_TTL_DAYS = 7;
    private static final long INVALID_TTL_DAYS = 7;
    private static final int DONE_LOOKBACK_DAYS = 7;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserNotificationService userNotificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Bean
    public Consumer<UserNotificationEvent> userNotificationConsumer() {
        return event -> {
            if (event == null || event.getEventType() == null) {
                return;
            }
            String eventId = event.getEventId();
            if (!StringUtils.hasText(eventId)) {
                log.warn("Notification event missing eventId: eventType={}", event.getEventType());
                return;
            }
            if (isAlreadyProcessed(eventId)) {
                log.debug("Duplicate notification event ignored: eventId={}, eventType={}", eventId, event.getEventType());
                return;
            }
            if (!markProcessing(eventId)) {
                throw new IllegalStateException("notification processing already in progress");
            }
            try {
                boolean delivered = switch (event.getEventType()) {
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
                            log.warn("Notification event missing password reset payload: eventId={}", event.getEventId());
                            recordInvalid(eventId, "missing userId or token");
                            yield true;
                        }
                        yield userNotificationService.sendPasswordResetEmail(event.getUserId(), event.getToken());
                    }
                    case UserNotificationEvent.TYPE_ACTIVATION -> {
                        if (event.getUserId() == null || event.getToken() == null) {
                            log.warn("Notification event missing activation payload: eventId={}", event.getEventId());
                            recordInvalid(eventId, "missing userId or token");
                            yield true;
                        }
                        yield userNotificationService.sendActivationEmail(event.getUserId(), event.getToken());
                    }
                    case UserNotificationEvent.TYPE_STATUS_CHANGE -> {
                        if (event.getUserId() == null) {
                            log.warn("Notification event missing status change payload: eventId={}", event.getEventId());
                            recordInvalid(eventId, "missing userId");
                            yield true;
                        }
                        yield userNotificationService.sendStatusChangeNotification(
                                event.getUserId(),
                                event.getNewStatus(),
                                event.getReason()
                        );
                    }
                    case UserNotificationEvent.TYPE_BATCH -> {
                        if (event.getUserIds() == null || event.getUserIds().isEmpty()) {
                            log.warn("Notification event missing batch payload: eventId={}", event.getEventId());
                            recordInvalid(eventId, "missing userIds");
                            yield true;
                        }
                        yield userNotificationService.sendBatchNotification(
                                event.getUserIds(),
                                event.getTitle(),
                                event.getContent()
                        );
                    }
                    case UserNotificationEvent.TYPE_SYSTEM -> {
                        if (event.getTitle() == null || event.getContent() == null) {
                            log.warn("Notification event missing system payload: eventId={}", event.getEventId());
                            recordInvalid(eventId, "missing title or content");
                            yield true;
                        }
                        yield userNotificationService.sendSystemAnnouncement(
                                event.getTitle(),
                                event.getContent()
                        );
                    }
                    default -> {
                        log.warn("Unknown notification event type: {}", event.getEventType());
                        recordInvalid(eventId, "unknown eventType: " + event.getEventType());
                        yield true;
                    }
                };
                if (!delivered) {
                    log.warn("Notification delivery skipped or failed without exception: eventId={}, eventType={}",
                            eventId, event.getEventType());
                }
                markProcessed(eventId);
            } catch (Exception e) {
                log.error("Failed to dispatch notification event: eventId={}, eventType={}",
                        event.getEventId(), event.getEventType(), e);
                throw e;
            } finally {
                clearProcessing(eventId);
            }
        };
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
            return Boolean.TRUE.equals(redisTemplate.hasKey(DONE_PREFIX + eventId));
        } catch (Exception e) {
            log.warn("Check notification processed state failed: eventId={}", eventId, e);
            return false;
        }
    }

    private boolean markProcessing(String eventId) {
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(PROCESSING_PREFIX + eventId, "1", PROCESSING_TTL_MINUTES, TimeUnit.MINUTES);
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
