package com.cloud.order.messaging;

import com.cloud.common.messaging.consumer.AbstractMqConsumer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "refund-cancelled",
        consumerGroup = "order-refund-cancelled-notification-group",
        selectorExpression = "REFUND_CANCELLED")
public class RefundCancelledNotificationConsumer extends AbstractMqConsumer<Map<String, Object>> {

    private static final String NS_REFUND_CANCELLED = "order:notify:refundCancelled";

    private final ObjectMapper objectMapper;

    @Override
    protected void doConsume(Map<String, Object> event, MessageExt msgExt) {
        if (event == null) {
            return;
        }
        String refundNo = getString(event, "refundNo");
        Long merchantId = getLong(event, "merchantId");

        if (merchantId != null) {
            sendNotification(
                    "MERCHANT",
                    merchantId,
                    "Refund request cancelled",
                    String.format("User cancelled refund request. refundNo=%s", refundNo));
        }
    }

    @Override
    protected Map<String, Object> deserialize(byte[] body) {
        try {
            if (body == null) {
                return null;
            }
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize refund cancelled notification", ex);
        }
    }

    @Override
    protected String resolveIdempotentNamespace(
            String topic, MessageExt msgExt, Map<String, Object> payload) {
        return NS_REFUND_CANCELLED;
    }

    @Override
    protected String buildIdempotentKey(
            String topic, String msgId, Map<String, Object> payload, MessageExt msgExt) {
        return getEventId(payload, "REFUND_CANCELLED");
    }

    private String getEventId(Map<String, Object> event, String eventType) {
        String eventId = getString(event, "eventId");
        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }
        String refundNo = getString(event, "refundNo");
        if (refundNo != null && !refundNo.isBlank()) {
            return eventType + ":" + refundNo;
        }
        return eventType + ":" + System.currentTimeMillis();
    }

    private String getString(Map<String, Object> event, String key) {
        Object value = event.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long getLong(Map<String, Object> event, String key) {
        Object value = event.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void sendNotification(
            String receiverType, Long receiverId, String title, String content) {
        log.info(
                "Refund notification dispatched: receiverType={}, receiverId={}, title={}, content={}",
                receiverType,
                receiverId,
                title,
                content);
    }
}
