package com.cloud.order.messaging;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.messaging.consumer.AbstractMqConsumer;
import com.cloud.common.messaging.event.RefundCompletedEvent;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.AfterSaleAction;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    topic = "refund-completed",
    consumerGroup = "order-refund-completed-consumer-group",
    selectorExpression = "REFUND_COMPLETED")
public class RefundCompletedConsumer extends AbstractMqConsumer<RefundCompletedEvent> {

  private static final String NS_REFUND_COMPLETED = "order:refund:completed";

  private final AfterSaleMapper afterSaleMapper;
  private final OrderSubMapper orderSubMapper;
  private final OrderItemMapper orderItemMapper;
  private final OrderService orderService;
  private final OrderMessageProducer orderMessageProducer;
  private final TradeMetrics tradeMetrics;
  private final ObjectMapper objectMapper;

  @Override
  protected void doConsume(RefundCompletedEvent event, MessageExt msgExt) {
    if (event == null || event.getAfterSaleNo() == null || event.getAfterSaleNo().isBlank()) {
      tradeMetrics.incrementMessageConsume("refund_completed", "failed");
      return;
    }

    AfterSale afterSale =
        afterSaleMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getAfterSaleNo, event.getAfterSaleNo())
                .eq(AfterSale::getDeleted, 0)
                .last("LIMIT 1"));
    if (afterSale == null) {
      tradeMetrics.incrementMessageConsume("refund_completed", "failed");
      return;
    }

    if (!"REFUNDED".equals(afterSale.getStatus())) {
      orderService.advanceAfterSaleStatus(
          afterSale.getId(), AfterSaleAction.REFUND, "refund completed");
    }

    StockRestoreEvent restoreEvent = buildStockRestoreEvent(afterSale, event);
    if (restoreEvent != null) {
      orderMessageProducer.sendStockRestoreEvent(restoreEvent);
    }

    tradeMetrics.incrementMessageConsume("refund_completed", "success");
  }

  @Override
  protected RefundCompletedEvent deserialize(byte[] body) {
    try {
      return body == null ? null : objectMapper.readValue(body, RefundCompletedEvent.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to deserialize RefundCompletedEvent", ex);
    }
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, RefundCompletedEvent payload) {
    return NS_REFUND_COMPLETED;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, RefundCompletedEvent payload, MessageExt msgExt) {
    return resolveEventId(payload);
  }

  @Override
  protected void onBizException(
      MessageExt msgExt, RefundCompletedEvent payload, com.cloud.common.exception.BizException ex) {
    tradeMetrics.incrementMessageConsume("refund_completed", "biz");
  }

  @Override
  protected void onSystemException(
      MessageExt msgExt,
      RefundCompletedEvent payload,
      com.cloud.common.exception.SystemException ex,
      boolean retryable) {
    tradeMetrics.incrementMessageConsume("refund_completed", retryable ? "retry" : "failed");
  }

  @Override
  protected void onUnknownException(MessageExt msgExt, RefundCompletedEvent payload, Exception ex) {
    tradeMetrics.incrementMessageConsume("refund_completed", "retry");
  }

  private StockRestoreEvent buildStockRestoreEvent(
      AfterSale afterSale, RefundCompletedEvent event) {
    if (afterSale.getSubOrderId() == null) {
      return null;
    }
    OrderSub subOrder = orderSubMapper.selectById(afterSale.getSubOrderId());
    if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
      return null;
    }
    List<OrderItem> items = orderItemMapper.listActiveBySubOrderId(subOrder.getId());
    if (items == null || items.isEmpty()) {
      return null;
    }

    Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
    for (OrderItem item : items) {
      if (item.getSkuId() == null || item.getQuantity() == null) {
        continue;
      }
      skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
    }
    if (skuQuantities.isEmpty()) {
      return null;
    }

    List<StockOperateCommandDTO> commands = new ArrayList<>();
    for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
      StockOperateCommandDTO command = new StockOperateCommandDTO();
      command.setSubOrderNo(subOrder.getSubOrderNo());
      command.setOrderNo(event.getMainOrderNo());
      command.setSkuId(entry.getKey());
      command.setQuantity(entry.getValue());
      command.setReason("refund restore " + afterSale.getAfterSaleNo());
      commands.add(command);
    }

    return StockRestoreEvent.builder()
        .refundNo(event.getRefundNo())
        .mainOrderNo(event.getMainOrderNo())
        .subOrderNo(subOrder.getSubOrderNo())
        .items(commands)
        .build();
  }

  private String resolveEventId(RefundCompletedEvent event) {
    if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
      return event.getEventId();
    }
    if (event != null && event.getRefundNo() != null && !event.getRefundNo().isBlank()) {
      return "REFUND_COMPLETED:" + event.getRefundNo();
    }
    return "REFUND_COMPLETED:" + System.currentTimeMillis();
  }
}
