package com.cloud.order.messaging;

import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.OrderTimeoutEvent;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderTimeoutService;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

  private static final String NS_ORDER_TIMEOUT = "order:timeout:cancel";
  private static final Set<String> CANCELLABLE_STATUSES = Set.of("CREATED", "STOCK_RESERVED");

  private final MessageIdempotencyService messageIdempotencyService;
  private final OrderTimeoutService orderTimeoutService;
  private final OrderSubMapper orderSubMapper;

  @Bean
  public Consumer<Message<OrderTimeoutEvent>> orderTimeoutConsumer() {
    return message -> {
      OrderTimeoutEvent event = message.getPayload();
      String eventId = resolveEventId(event);
      if (!messageIdempotencyService.tryAcquire(NS_ORDER_TIMEOUT, eventId)) {
        log.warn("Duplicate order-timeout event, skip: eventId={}", eventId);
        return;
      }

      try {
        if (event == null || event.getSubOrderId() == null) {
          messageIdempotencyService.markSuccess(NS_ORDER_TIMEOUT, eventId);
          return;
        }

        OrderSub subOrder = orderSubMapper.selectById(event.getSubOrderId());
        if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
          messageIdempotencyService.markSuccess(NS_ORDER_TIMEOUT, eventId);
          return;
        }

        if (!CANCELLABLE_STATUSES.contains(subOrder.getOrderStatus())) {
          messageIdempotencyService.markSuccess(NS_ORDER_TIMEOUT, eventId);
          return;
        }

        orderTimeoutService.cancelTimeoutOrder(subOrder.getId());
        messageIdempotencyService.markSuccess(NS_ORDER_TIMEOUT, eventId);
      } catch (Exception ex) {
        log.error(
            "Handle order timeout failed: eventId={}, subOrderId={}",
            eventId,
            event == null ? null : event.getSubOrderId(),
            ex);
        throw new RuntimeException("Handle order timeout failed", ex);
      }
    };
  }

  private String resolveEventId(OrderTimeoutEvent event) {
    if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
      return event.getEventId();
    }
    if (event != null && event.getSubOrderNo() != null && !event.getSubOrderNo().isBlank()) {
      return "ORDER_TIMEOUT:" + event.getSubOrderNo();
    }
    if (event != null && event.getSubOrderId() != null) {
      return "ORDER_TIMEOUT:" + event.getSubOrderId();
    }
    return "ORDER_TIMEOUT:" + System.currentTimeMillis();
  }
}
