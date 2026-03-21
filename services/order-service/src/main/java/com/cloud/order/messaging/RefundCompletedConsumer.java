package com.cloud.order.messaging;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.exception.SystemException;
import com.cloud.common.messaging.consumer.AbstractJsonMqConsumer;
import com.cloud.common.messaging.event.RefundCompletedEvent;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.AfterSaleItem;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.AfterSaleAction;
import com.cloud.order.mapper.AfterSaleItemMapper;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderItemMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
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
public class RefundCompletedConsumer extends AbstractJsonMqConsumer<RefundCompletedEvent> {

  private static final String NS_REFUND_COMPLETED = "order:refund:completed";
  private static final String RETURN_REFUND_TYPE = "RETURN_REFUND";

  private final AfterSaleMapper afterSaleMapper;
  private final AfterSaleItemMapper afterSaleItemMapper;
  private final OrderSubMapper orderSubMapper;
  private final OrderItemMapper orderItemMapper;
  private final OrderService orderService;
  private final OrderMessageProducer orderMessageProducer;
  private final TradeMetrics tradeMetrics;

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
    if (restoreEvent != null && !orderMessageProducer.sendStockRestoreEvent(restoreEvent)) {
      throw new SystemException("failed to enqueue stock restore event");
    }

    tradeMetrics.incrementMessageConsume("refund_completed", "success");
  }

  @Override
  protected Class<RefundCompletedEvent> payloadClass() {
    return RefundCompletedEvent.class;
  }

  @Override
  protected String payloadDescription() {
    return "RefundCompletedEvent";
  }

  @Override
  protected String resolveIdempotentNamespace(
      String topic, MessageExt msgExt, RefundCompletedEvent payload) {
    return NS_REFUND_COMPLETED;
  }

  @Override
  protected String buildIdempotentKey(
      String topic, String msgId, RefundCompletedEvent payload, MessageExt msgExt) {
    return resolveEventId(
        "REFUND_COMPLETED",
        payload == null ? null : payload.getEventId(),
        payload == null ? null : payload.getRefundNo());
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
    if (!RETURN_REFUND_TYPE.equalsIgnoreCase(afterSale.getAfterSaleType())
        || afterSale.getSubOrderId() == null) {
      return null;
    }
    OrderSub subOrder = orderSubMapper.selectById(afterSale.getSubOrderId());
    if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
      return null;
    }
    List<StockOperateCommandDTO> commands = resolveRestoreCommands(afterSale, event, subOrder);
    if (commands.isEmpty()) {
      return null;
    }

    return StockRestoreEvent.builder()
        .refundNo(event.getRefundNo())
        .mainOrderNo(event.getMainOrderNo())
        .subOrderNo(subOrder.getSubOrderNo())
        .items(commands)
        .build();
  }

  private List<StockOperateCommandDTO> resolveRestoreCommands(
      AfterSale afterSale, RefundCompletedEvent event, OrderSub subOrder) {
    List<StockOperateCommandDTO> fromEvent = sanitizeRestoreItems(event, subOrder, afterSale);
    if (!fromEvent.isEmpty()) {
      return fromEvent;
    }

    List<AfterSaleItem> afterSaleItems =
        afterSale.getId() == null
            ? Collections.emptyList()
            : afterSaleItemMapper.listActiveByAfterSaleId(afterSale.getId());
    List<StockOperateCommandDTO> fromAfterSaleItems =
        buildCommandsFromAfterSaleItems(afterSaleItems, subOrder, event, afterSale);
    if (!fromAfterSaleItems.isEmpty()) {
      return fromAfterSaleItems;
    }

    if (!isFullSubOrderRefund(afterSale, subOrder)) {
      return Collections.emptyList();
    }
    return buildCommandsFromOrderItems(subOrder, event, afterSale);
  }

  private List<StockOperateCommandDTO> sanitizeRestoreItems(
      RefundCompletedEvent event, OrderSub subOrder, AfterSale afterSale) {
    if (event == null || event.getItems() == null || event.getItems().isEmpty()) {
      return Collections.emptyList();
    }
    List<StockOperateCommandDTO> commands = new ArrayList<>();
    for (StockOperateCommandDTO item : event.getItems()) {
      if (item == null
          || item.getSkuId() == null
          || item.getQuantity() == null
          || item.getQuantity() <= 0) {
        continue;
      }
      StockOperateCommandDTO command = new StockOperateCommandDTO();
      command.setSubOrderNo(subOrder.getSubOrderNo());
      command.setOrderNo(event.getMainOrderNo());
      command.setSkuId(item.getSkuId());
      command.setQuantity(item.getQuantity());
      command.setReason("refund restore " + afterSale.getAfterSaleNo());
      commands.add(command);
    }
    return commands;
  }

  private List<StockOperateCommandDTO> buildCommandsFromAfterSaleItems(
      List<AfterSaleItem> afterSaleItems,
      OrderSub subOrder,
      RefundCompletedEvent event,
      AfterSale afterSale) {
    if (afterSaleItems == null || afterSaleItems.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
    for (AfterSaleItem item : afterSaleItems) {
      if (item == null
          || item.getSkuId() == null
          || item.getQuantity() == null
          || item.getQuantity() <= 0) {
        continue;
      }
      skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
    }
    return buildCommandsFromQuantities(skuQuantities, subOrder, event, afterSale);
  }

  private List<StockOperateCommandDTO> buildCommandsFromOrderItems(
      OrderSub subOrder, RefundCompletedEvent event, AfterSale afterSale) {
    List<OrderItem> items = orderItemMapper.listActiveBySubOrderId(subOrder.getId());
    if (items == null || items.isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, Integer> skuQuantities = new LinkedHashMap<>();
    for (OrderItem item : items) {
      if (item.getSkuId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
        continue;
      }
      skuQuantities.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
    }
    return buildCommandsFromQuantities(skuQuantities, subOrder, event, afterSale);
  }

  private List<StockOperateCommandDTO> buildCommandsFromQuantities(
      Map<Long, Integer> skuQuantities,
      OrderSub subOrder,
      RefundCompletedEvent event,
      AfterSale afterSale) {
    if (skuQuantities == null || skuQuantities.isEmpty()) {
      return Collections.emptyList();
    }
    List<StockOperateCommandDTO> commands = new ArrayList<>();
    for (Map.Entry<Long, Integer> entry : skuQuantities.entrySet()) {
      Integer quantity = entry.getValue();
      if (entry.getKey() == null || quantity == null || quantity <= 0) {
        continue;
      }
      StockOperateCommandDTO command = new StockOperateCommandDTO();
      command.setSubOrderNo(subOrder.getSubOrderNo());
      command.setOrderNo(event.getMainOrderNo());
      command.setSkuId(entry.getKey());
      command.setQuantity(quantity);
      command.setReason("refund restore " + afterSale.getAfterSaleNo());
      commands.add(command);
    }
    return commands;
  }

  private boolean isFullSubOrderRefund(AfterSale afterSale, OrderSub subOrder) {
    BigDecimal refundAmount = resolveRefundAmount(afterSale);
    BigDecimal payableAmount =
        subOrder.getPayableAmount() == null ? BigDecimal.ZERO : subOrder.getPayableAmount();
    return refundAmount.compareTo(BigDecimal.ZERO) > 0
        && payableAmount.compareTo(BigDecimal.ZERO) > 0
        && refundAmount.compareTo(payableAmount) >= 0;
  }

  private BigDecimal resolveRefundAmount(AfterSale afterSale) {
    if (afterSale.getApprovedAmount() != null) {
      return afterSale.getApprovedAmount();
    }
    if (afterSale.getApplyAmount() != null) {
      return afterSale.getApplyAmount();
    }
    return BigDecimal.ZERO;
  }
}
