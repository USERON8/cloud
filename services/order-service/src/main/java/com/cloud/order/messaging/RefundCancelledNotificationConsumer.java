package com.cloud.order.messaging;

import java.util.Map;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "refund-cancelled",
    consumerGroup = "order-refund-cancelled-notification-group",
    selectorExpression = "REFUND_CANCELLED")
public class RefundCancelledNotificationConsumer extends AbstractRefundNotificationConsumer {

  private static final String NS_REFUND_CANCELLED = "order:notify:refundCancelled";

  @Override
  protected void doConsume(Map<String, Object> event, MessageExt msgExt) {
    if (event == null) {
      return;
    }
    String refundNo = readString(event, "refundNo");
    Long merchantId = readLong(event, "merchantId");

    if (merchantId != null) {
      sendNotification(
          "MERCHANT",
          merchantId,
          "Refund request cancelled",
          String.format("User cancelled refund request. refundNo=%s", refundNo));
    }
  }

  @Override
  protected String namespace() {
    return NS_REFUND_CANCELLED;
  }

  @Override
  protected String eventType() {
    return "REFUND_CANCELLED";
  }

  @Override
  protected String notificationName() {
    return "refund cancelled";
  }
}
