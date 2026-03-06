package com.cloud.order.messaging;

import com.cloud.common.messaging.MessageIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;




@Slf4j
@Component
@RequiredArgsConstructor
public class RefundNotificationConsumer {

    private static final String NS_REFUND_CREATED = "order:notify:refundCreated";
    private static final String NS_REFUND_AUDITED = "order:notify:refundAudited";
    private static final String NS_REFUND_PROCESS = "order:notify:refundProcess";
    private static final String NS_REFUND_CANCELLED = "order:notify:refundCancelled";

    private final MessageIdempotencyService messageIdempotencyService;

    @Bean
    public Consumer<Message<Map<String, Object>>> refundCreatedNotificationConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();
            String eventId = getEventId(event, "REFUND_CREATED");
            if (!messageIdempotencyService.tryAcquire(NS_REFUND_CREATED, eventId)) {
                log.warn("Duplicate refund-created notification event, skip: eventId={}", eventId);
                return;
            }

            try {
                String refundNo = getString(event, "refundNo");
                String orderNo = getString(event, "orderNo");
                Long merchantId = getLong(event, "merchantId");

                if (merchantId != null) {
                    sendNotification(
                            "MERCHANT",
                            merchantId,
                            "New refund request",
                            String.format("Order %s has a new refund request. refundNo=%s", orderNo, refundNo)
                    );
                }
                messageIdempotencyService.markSuccess(NS_REFUND_CREATED, eventId);
                
            } catch (Exception ex) {
                log.error("Handle refund-created notification failed: eventId={}", eventId, ex);
                throw new RuntimeException("Handle refund-created notification failed", ex);
            }
        };
    }

    @Bean
    public Consumer<Message<Map<String, Object>>> refundAuditedNotificationConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();
            String eventId = getEventId(event, "REFUND_AUDITED");
            if (!messageIdempotencyService.tryAcquire(NS_REFUND_AUDITED, eventId)) {
                log.warn("Duplicate refund-audited notification event, skip: eventId={}", eventId);
                return;
            }

            try {
                String refundNo = getString(event, "refundNo");
                Boolean approved = (Boolean) event.get("approved");
                Long userId = getLong(event, "userId");

                if (userId != null) {
                    String title = Boolean.TRUE.equals(approved) ? "Refund request approved" : "Refund request rejected";
                    String content = Boolean.TRUE.equals(approved)
                            ? String.format("Your refund request has been approved. refundNo=%s", refundNo)
                            : String.format("Your refund request has been rejected. refundNo=%s", refundNo);
                    sendNotification("USER", userId, title, content);
                }
                messageIdempotencyService.markSuccess(NS_REFUND_AUDITED, eventId);
                

            } catch (Exception ex) {
                log.error("Handle refund-audited notification failed: eventId={}", eventId, ex);
                throw new RuntimeException("Handle refund-audited notification failed", ex);
            }
        };
    }

    @Bean
    public Consumer<Message<Map<String, Object>>> refundProcessNotificationConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();
            String eventId = getEventId(event, "REFUND_PROCESS");
            if (!messageIdempotencyService.tryAcquire(NS_REFUND_PROCESS, eventId)) {
                log.warn("Duplicate refund-process notification event, skip: eventId={}", eventId);
                return;
            }

            try {
                String refundNo = getString(event, "refundNo");
                Long userId = getLong(event, "userId");
                if (userId != null) {
                    sendNotification(
                            "USER",
                            userId,
                            "Refund is processing",
                            String.format("Your refund is in processing. refundNo=%s", refundNo)
                    );
                }
                messageIdempotencyService.markSuccess(NS_REFUND_PROCESS, eventId);
                
            } catch (Exception ex) {
                log.error("Handle refund-process notification failed: eventId={}", eventId, ex);
                throw new RuntimeException("Handle refund-process notification failed", ex);
            }
        };
    }

    @Bean
    public Consumer<Message<Map<String, Object>>> refundCancelledNotificationConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();
            String eventId = getEventId(event, "REFUND_CANCELLED");
            if (!messageIdempotencyService.tryAcquire(NS_REFUND_CANCELLED, eventId)) {
                log.warn("Duplicate refund-cancelled notification event, skip: eventId={}", eventId);
                return;
            }

            try {
                String refundNo = getString(event, "refundNo");
                Long merchantId = getLong(event, "merchantId");
                if (merchantId != null) {
                    sendNotification(
                            "MERCHANT",
                            merchantId,
                            "Refund request cancelled",
                            String.format("User cancelled refund request. refundNo=%s", refundNo)
                    );
                }
                messageIdempotencyService.markSuccess(NS_REFUND_CANCELLED, eventId);
                
            } catch (Exception ex) {
                log.error("Handle refund-cancelled notification failed: eventId={}", eventId, ex);
                throw new RuntimeException("Handle refund-cancelled notification failed", ex);
            }
        };
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

    private void sendNotification(String receiverType, Long receiverId, String title, String content) {
        log.info("Refund notification dispatched: receiverType={}, receiverId={}, title={}, content={}",
                receiverType, receiverId, title, content);
    }
}
