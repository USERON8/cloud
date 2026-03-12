package com.cloud.user.messaging;

import com.cloud.common.messaging.event.UserNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserNotificationProducer {

    private static final String BINDING_NAME = "userNotificationProducer-out-0";

    private final StreamBridge streamBridge;

    public boolean send(UserNotificationEvent event) {
        if (event == null || event.getEventType() == null || event.getEventType().isBlank()) {
            return false;
        }
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(System.currentTimeMillis());
        }
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_KEYS, event.getEventId());
        headers.put(MessageConst.PROPERTY_TAGS, event.getEventType());

        Message<UserNotificationEvent> message = MessageBuilder
                .withPayload(event)
                .copyHeaders(headers)
                .build();
        boolean sent = streamBridge.send(BINDING_NAME, message);
        if (!sent) {
            log.error("Failed to enqueue notification event: eventId={}, eventType={}",
                    event.getEventId(), event.getEventType());
        }
        return sent;
    }
}
