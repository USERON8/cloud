package com.cloud.order.messaging;

import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.service.support.OrderInventoryEventService;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "payment-success",
    consumerGroup = "order-payment-success-consumer-group",
    selectorExpression = "PAYMENT_SUCCESS")
public class PaymentSuccessConsumer extends AbstractJsonMqConsumer<PaymentSuccessEvent> {

  private static final String NS_PAYMENT_SUCCESS = "order:payment:success";
  private static final String WS_CHANNEL_PREFIX = "ws:message:";

  private final OrderMainMapper orderMainMapper;
  private final OrderInventoryEventService orderInventoryEventService;
  private final TradeMetrics tradeMetrics;
  private final StringRedisTemplate stringRedisTemplate;

  @Override
  protected void doConsume(PaymentSuccessEvent event, MessageExt msgExt) {
    if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
      tradeMetrics.incrementMessageConsume("payment_success", "failed");
      return;
    }

    OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(event.getOrderNo());
    if (mainOrder == null) {
      tradeMetrics.incrementMessageConsume("payment_success", "failed");
      return;
    }

    orderInventoryEventService.handlePaymentSuccess(event);
    pushPaymentSuccessMessage(event);
  }

  @Override
  protected Class<PaymentSuccessEvent> payloadClass() {
    return PaymentSuccessEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "PaymentSuccessEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, PaymentSuccessEvent payload) {
    return NS_PAYMENT_SUCCESS;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, PaymentSuccessEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "PAYMENT_SUCCESS",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getOrderNo());
  }

  @Override
  protected void onConsumeSuccess(MessageExt msgExt, PaymentSuccessEvent payload) {
    tradeMetrics.incrementMessageConsume("payment_success", "success");
  }

  @Override
  protected void onBizException(
      MessageExt msgExt, PaymentSuccessEvent payload, com.cloud.common.exception.BizException ex) {
    tradeMetrics.incrementMessageConsume("payment_success", "biz");
  }

  @Override
  protected void onSystemException(
      MessageExt msgExt,
      PaymentSuccessEvent payload,
      com.cloud.common.exception.SystemException ex,
      boolean retryable) {
    tradeMetrics.incrementMessageConsume("payment_success", retryable ? "retry" : "failed");
  }

  @Override
  protected void onUnknownException(MessageExt msgExt, PaymentSuccessEvent payload, Exception ex) {
    tradeMetrics.incrementMessageConsume("payment_success", "retry");
  }

  private void pushPaymentSuccessMessage(PaymentSuccessEvent event) {
    if (event == null || event.getUserId() == null) {
      return;
    }
    try {
      Map<String, Object> message =
          Map.of(
              "type",
              "PAYMENT_SUCCESS",
              "userId",
              String.valueOf(event.getUserId()),
              "data",
              Map.of("orderId", event.getOrderNo(), "subOrderNo", event.getSubOrderNo()),
              "timestamp",
              Instant.now().toEpochMilli());
      String payload = objectMapper().writeValueAsString(message);
      stringRedisTemplate.convertAndSend(WS_CHANNEL_PREFIX + event.getUserId(), payload);
    } catch (Exception ex) {
      log.warn("Send payment success WS message failed: orderNo={}", event.getOrderNo(), ex);
    }
  }
}
