package com.cloud.order.messaging;

import java.util.Map;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "refund-created",
    consumerGroup = "order-refund-created-notification-group",
    selectorExpression = "REFUND_CREATED")
public class RefundCreatedNotificationConsumer extends AbstractRefundNotificationConsumer {

  private static final String NS_REFUND_CREATED = "order:notify:refundCreated";

  @Override
  protected void doConsume(Map<String, Object> event, MessageExt msgExt) {
    if (event == null) {
      return;
    }
    String refundNo = readString(event, "refundNo");
    String orderNo = readString(event, "orderNo");
    Long merchantId = readLong(event, "merchantId");

    if (merchantId != null) {
      sendNotification(
          "MERCHANT",
          merchantId,
          "New refund request",
          String.format("Order %s has a new refund request. refundNo=%s", orderNo, refundNo));
    }
  }

  @Override
  protected String namespace() {
    return NS_REFUND_CREATED;
  }

  @Override
  protected String eventType() {
    return "REFUND_CREATED";
  }

  @Override
  protected String notificationName() {
    return "refund created";
  }
}
