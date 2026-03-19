package com.cloud.order.messaging;

import java.util.Map;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "refund-audited",
    consumerGroup = "order-refund-audited-notification-group",
    selectorExpression = "REFUND_AUDITED")
public class RefundAuditedNotificationConsumer extends AbstractRefundNotificationConsumer {

  private static final String NS_REFUND_AUDITED = "order:notify:refundAudited";

  @Override
  protected void doConsume(Map<String, Object> event, MessageExt msgExt) {
    if (event == null) {
      return;
    }
    String refundNo = readString(event, "refundNo");
    Boolean approved = readBoolean(event, "approved");
    Long userId = readLong(event, "userId");

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
  protected String namespace() {
    return NS_REFUND_AUDITED;
  }

  @Override
  protected String eventType() {
    return "REFUND_AUDITED";
  }

  @Override
  protected String notificationName() {
    return "refund audited";
  }
}
