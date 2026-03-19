package com.cloud.order.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMapMqConsumer;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;

@Slf4j
public abstract class AbstractRefundNotificationConsumer extends AbstractJsonMapMqConsumer {

  @Override
  protected final String payloadDescription() {
    return notificationName() + " notification";
  }

  @Override
  protected final String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, Map<String, Object> payload) {
    return namespace();
  }

  @Override
  protected final String buildIdempotentKey(
      String topic, String msgId, Map<String, Object> payload, MessageExt msgExt) {
    return resolveEventId(
        eventType(), readString(payload, "eventId"), readString(payload, "refundNo"));
  }

  protected abstract String namespace();

  protected abstract String eventType();

  protected abstract String notificationName();

  protected final void sendNotification(
      String receiverType, Long receiverId, String title, String content) {
    log.info(
        "Refund notification dispatched: receiverType={}, receiverId={}, title={}, content={}",
        receiverType,
        receiverId,
        title,
        content);
  }
}
