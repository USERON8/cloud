package com.cloud.user.messaging;

import com.cloud.common.messaging.event.UserNotificationEvent;
import com.cloud.user.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user.notification.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class UserNotificationConsumerConfig {

    private final UserNotificationService userNotificationService;

    @Bean
    public Consumer<UserNotificationEvent> userNotificationConsumer() {
        return event -> {
            if (event == null || event.getEventType() == null) {
                return;
            }
            try {
                boolean delivered = switch (event.getEventType()) {
                    case UserNotificationEvent.TYPE_WELCOME -> {
                        if (event.getUserId() == null) {
                            log.warn("Notification event missing userId: eventId={}", event.getEventId());
                            yield true;
                        }
                        yield userNotificationService.sendWelcomeEmail(event.getUserId());
                    }
                    case UserNotificationEvent.TYPE_PASSWORD_RESET -> {
                        if (event.getUserId() == null || event.getToken() == null) {
                            log.warn("Notification event missing password reset payload: eventId={}", event.getEventId());
                            yield true;
                        }
                        yield userNotificationService.sendPasswordResetEmail(event.getUserId(), event.getToken());
                    }
                    case UserNotificationEvent.TYPE_ACTIVATION -> {
                        if (event.getUserId() == null || event.getToken() == null) {
                            log.warn("Notification event missing activation payload: eventId={}", event.getEventId());
                            yield true;
                        }
                        yield userNotificationService.sendActivationEmail(event.getUserId(), event.getToken());
                    }
                    case UserNotificationEvent.TYPE_STATUS_CHANGE -> {
                        if (event.getUserId() == null) {
                            log.warn("Notification event missing status change payload: eventId={}", event.getEventId());
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
                            yield true;
                        }
                        yield userNotificationService.sendSystemAnnouncement(
                                event.getTitle(),
                                event.getContent()
                        );
                    }
                    default -> {
                        log.warn("Unknown notification event type: {}", event.getEventType());
                        yield true;
                    }
                };
                if (!delivered) {
                    throw new IllegalStateException("notification delivery failed");
                }
            } catch (Exception e) {
                log.error("Failed to dispatch notification event: eventId={}, eventType={}",
                        event.getEventId(), event.getEventType(), e);
                throw e;
            }
        };
    }
}
