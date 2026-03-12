package com.cloud.user.messaging;

import com.cloud.common.messaging.event.UserNotificationEvent;
import com.cloud.user.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserNotificationConsumerConfig {

    private final UserNotificationService userNotificationService;

    @Bean
    public Consumer<UserNotificationEvent> userNotificationConsumer() {
        return event -> {
            if (event == null || event.getEventType() == null) {
                return;
            }
            try {
                switch (event.getEventType()) {
                    case UserNotificationEvent.TYPE_WELCOME -> userNotificationService.sendWelcomeEmailAsync(event.getUserId());
                    case UserNotificationEvent.TYPE_PASSWORD_RESET ->
                            userNotificationService.sendPasswordResetEmailAsync(event.getUserId(), event.getToken());
                    case UserNotificationEvent.TYPE_ACTIVATION ->
                            userNotificationService.sendActivationEmailAsync(event.getUserId(), event.getToken());
                    case UserNotificationEvent.TYPE_STATUS_CHANGE ->
                            userNotificationService.sendStatusChangeNotificationAsync(
                                    event.getUserId(),
                                    event.getNewStatus(),
                                    event.getReason()
                            );
                    case UserNotificationEvent.TYPE_BATCH ->
                            userNotificationService.sendBatchNotificationAsync(
                                    event.getUserIds(),
                                    event.getTitle(),
                                    event.getContent()
                            );
                    case UserNotificationEvent.TYPE_SYSTEM ->
                            userNotificationService.sendSystemAnnouncementAsync(
                                    event.getTitle(),
                                    event.getContent()
                            );
                    default -> log.warn("Unknown notification event type: {}", event.getEventType());
                }
            } catch (Exception e) {
                log.error("Failed to dispatch notification event: eventId={}, eventType={}",
                        event.getEventId(), event.getEventType(), e);
            }
        };
    }
}
