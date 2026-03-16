package com.cloud.order.messaging;

import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.OrderAutoReceiveEvent;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
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
public class OrderAutoReceiveConsumer {

  private static final String NS_ORDER_AUTO_RECEIVE = "order:auto-receive";
  private static final Set<String> ALLOW_STATUSES = Set.of("SHIPPED");

  private final MessageIdempotencyService messageIdempotencyService;
  private final OrderService orderService;
  private final OrderSubMapper orderSubMapper;

  @Bean
  public Consumer<Message<OrderAutoReceiveEvent>> orderAutoReceiveConsumer() {
    return message -> {
      OrderAutoReceiveEvent event = message.getPayload();
      String eventId = resolveEventId(event);
      if (!messageIdempotencyService.tryAcquire(NS_ORDER_AUTO_RECEIVE, eventId)) {
        log.warn("Duplicate order auto receive event, skip: eventId={}", eventId);
        return;
      }

      try {
        if (event == null || event.getSubOrderId() == null) {
          messageIdempotencyService.markSuccess(NS_ORDER_AUTO_RECEIVE, eventId);
          return;
        }

        OrderSub subOrder = orderSubMapper.selectById(event.getSubOrderId());
        if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
          messageIdempotencyService.markSuccess(NS_ORDER_AUTO_RECEIVE, eventId);
          return;
        }

        if (!ALLOW_STATUSES.contains(subOrder.getOrderStatus())) {
          messageIdempotencyService.markSuccess(NS_ORDER_AUTO_RECEIVE, eventId);
          return;
        }

        orderService.advanceSubOrderStatus(subOrder.getId(), "DONE");
        messageIdempotencyService.markSuccess(NS_ORDER_AUTO_RECEIVE, eventId);
      } catch (Exception ex) {
        log.error(
            "Handle order auto receive failed: eventId={}, subOrderId={}",
            eventId,
            event == null ? null : event.getSubOrderId(),
            ex);
        throw new RuntimeException("Handle order auto receive failed", ex);
      }
    };
  }

  private String resolveEventId(OrderAutoReceiveEvent event) {
    if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
      return event.getEventId();
    }
    if (event != null && event.getSubOrderNo() != null && !event.getSubOrderNo().isBlank()) {
      return "ORDER_AUTO_RECEIVE:" + event.getSubOrderNo();
    }
    if (event != null && event.getSubOrderId() != null) {
      return "ORDER_AUTO_RECEIVE:" + event.getSubOrderId();
    }
    return "ORDER_AUTO_RECEIVE:" + System.currentTimeMillis();
  }
}
