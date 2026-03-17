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
    topic = "refund-audited",
    consumerGroup = "order-refund-audited-notification-group",
    selectorExpression = "REFUND_AUDITED")
public class RefundAuditedNotificationConsumer extends AbstractMqConsumer<Map<String, Object>> {

  private static final String NS_REFUND_AUDITED = "order:notify:refundAudited";

  private final ObjectMapper objectMapper;

  @Override
  protected void doConsume(Map<String, Object> event, MessageExt msgExt) {
    if (event == null) {
      return;
    }
    String refundNo = getString(event, "refundNo");
    Boolean approved = getBoolean(event, "approved");
    Long userId = getLong(event, "userId");

    if (userId != null) {
      String title =
          Boolean.TRUE.equals(approved) ? "Refund request approved" : "Refund request rejected";
      String content =
          Boolean.TRUE.equals(approved)
              ? String.format("Your refund request has been approved. refundNo=%s", refundNo)
              : String.format("Your refund request has been rejected. refundNo=%s", refundNo);
      sendNotification("USER", userId, title, content);
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
      throw new IllegalArgumentException("Failed to deserialize refund audited notification", ex);
    }
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, Map<String, Object> payload) {
    return NS_REFUND_AUDITED;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, Map<String, Object> payload, MessageExt msgExt) {
    return getEventId(payload, "REFUND_AUDITED");
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

  private Boolean getBoolean(Map<String, Object> event, String key) {
    Object value = event.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof Number number) {
      return number.intValue() != 0;
    }
    String text = String.valueOf(value);
    if (text.isBlank()) {
      return null;
    }
    return Boolean.parseBoolean(text);
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
