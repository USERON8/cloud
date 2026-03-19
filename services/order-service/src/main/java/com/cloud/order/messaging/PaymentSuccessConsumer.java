package com.cloud.order.messaging;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.messaging.consumer.AbstractMqConsumer;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.StockReservationRemoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class PaymentSuccessConsumer extends AbstractMqConsumer<PaymentSuccessEvent> {

  private static final String NS_PAYMENT_SUCCESS = "order:payment:success";
  private static final Set<String> CONFIRMABLE_STATUSES = Set.of("STOCK_RESERVED");
  private static final String WS_CHANNEL_PREFIX = "ws:message:";

  private final OrderMainMapper orderMainMapper;
  private final OrderService orderService;
  private final StockReservationRemoteService stockReservationRemoteService;
  private final TradeMetrics tradeMetrics;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

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

    OrderAggregateResponse aggregate = orderService.getOrderAggregate(mainOrder.getId());
    if (aggregate == null || aggregate.getSubOrders() == null) {
      tradeMetrics.incrementMessageConsume("payment_success", "failed");
      return;
    }

    String targetSubOrderNo = event.getSubOrderNo();
    for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
      OrderSub subOrder = wrapped.getSubOrder();
      if (subOrder == null) {
        continue;
      }
      if (targetSubOrderNo != null
          && !targetSubOrderNo.isBlank()
          && !targetSubOrderNo.equals(subOrder.getSubOrderNo())) {
        continue;
      }
      if (!CONFIRMABLE_STATUSES.contains(subOrder.getOrderStatus())) {
        continue;
      }
      confirmStockForSubOrder(subOrder, wrapped.getItems(), event.getOrderNo());
      orderService.advanceSubOrderStatus(subOrder.getId(), OrderAction.PAY);
    }

    pushPaymentSuccessMessage(event);
  }

  @Override
  protected PaymentSuccessEvent deserialize(byte[] body) {
    try {
      return body == null ? null : objectMapper.readValue(body, PaymentSuccessEvent.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to deserialize PaymentSuccessEvent", ex);
    }
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, PaymentSuccessEvent payload) {
    return NS_PAYMENT_SUCCESS;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, PaymentSuccessEvent payload, MessageExt msgExt) {
    return resolveEventId(payload);
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

  private void confirmStockForSubOrder(OrderSub subOrder, List<OrderItem> items, String orderNo) {
    if (items == null || items.isEmpty()) {
      return;
    }
    Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
    for (OrderItem item : items) {
      if (item.getSkuId() == null || item.getQuantity() == null) {
        continue;
      }
      skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
    }
    for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
      StockOperateCommandDTO command = new StockOperateCommandDTO();
      command.setSubOrderNo(subOrder.getSubOrderNo());
      command.setOrderNo(orderNo);
      command.setSkuId(entry.getKey());
      command.setQuantity(entry.getValue());
      command.setReason("confirm stock for payment " + subOrder.getSubOrderNo());
      stockReservationRemoteService.confirm(command);
    }
  }

  private String resolveEventId(PaymentSuccessEvent event) {
    if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
      return event.getEventId();
    }
    if (event != null && event.getOrderNo() != null && !event.getOrderNo().isBlank()) {
      return "PAYMENT_SUCCESS:" + event.getOrderNo();
    }
    return "PAYMENT_SUCCESS:" + System.currentTimeMillis();
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
      String payload = objectMapper.writeValueAsString(message);
      stringRedisTemplate.convertAndSend(WS_CHANNEL_PREFIX + event.getUserId(), payload);
    } catch (Exception ex) {
      log.warn("Send payment success WS message failed: orderNo={}", event.getOrderNo(), ex);
    }
  }
}
