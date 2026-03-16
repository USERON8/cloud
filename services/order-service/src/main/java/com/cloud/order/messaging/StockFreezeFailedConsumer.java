package com.cloud.order.messaging;

import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.StockFreezeFailedEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.SystemException;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockFreezeFailedConsumer {

  private static final String NS_STOCK_FREEZE_FAILED = "order:stock:freeze-failed";

  private final MessageIdempotencyService messageIdempotencyService;
  private final OrderMainMapper orderMainMapper;
  private final OrderSubMapper orderSubMapper;
  private final OrderService orderService;
  private final TradeMetrics tradeMetrics;

  @Bean
  public Consumer<Message<StockFreezeFailedEvent>> stockFreezeFailedConsumer() {
    return message -> {
      StockFreezeFailedEvent event = message.getPayload();
      String eventId = getEventId(event);
      if (!messageIdempotencyService.tryAcquire(NS_STOCK_FREEZE_FAILED, eventId)) {
        log.warn("Duplicate stock-freeze-failed event, skip: eventId={}", eventId);
        return;
      }

      try {
        if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
          tradeMetrics.incrementMessageConsume("stock_freeze_failed", "failed");
          messageIdempotencyService.markSuccess(NS_STOCK_FREEZE_FAILED, eventId);
          return;
        }
        OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(event.getOrderNo());
        if (mainOrder == null) {
          tradeMetrics.incrementMessageConsume("stock_freeze_failed", "failed");
          messageIdempotencyService.markSuccess(NS_STOCK_FREEZE_FAILED, eventId);
          return;
        }
        List<OrderSub> subOrders = orderSubMapper.listActiveByMainOrderId(mainOrder.getId());
        for (OrderSub subOrder : subOrders) {
          if (subOrder == null) {
            continue;
          }
          String status = subOrder.getOrderStatus();
          if (!"CANCELLED".equals(status) && !"CLOSED".equals(status)) {
            orderService.advanceSubOrderStatus(subOrder.getId(), "CANCEL");
          }
        }
        tradeMetrics.incrementMessageConsume("stock_freeze_failed", "success");
        messageIdempotencyService.markSuccess(NS_STOCK_FREEZE_FAILED, eventId);
      } catch (BizException ex) {
        tradeMetrics.incrementMessageConsume("stock_freeze_failed", "biz");
        log.warn(
            "Stock-freeze-failed skipped due to biz exception: eventId={}, orderNo={}, message={}",
            eventId,
            event == null ? null : event.getOrderNo(),
            ex.getMessage());
        messageIdempotencyService.markSuccess(NS_STOCK_FREEZE_FAILED, eventId);
      } catch (Exception ex) {
        tradeMetrics.incrementMessageConsume("stock_freeze_failed", "retry");
        log.error(
            "Handle stock-freeze-failed event failed: eventId={}, orderNo={}",
            eventId,
            event == null ? null : event.getOrderNo(),
            ex);
        throw new SystemException(
            ResultCode.SYSTEM_ERROR, "Handle stock-freeze-failed event failed", ex);
      }
    };
  }

  private String getEventId(StockFreezeFailedEvent event) {
    if (event != null && event.getEventId() != null && !event.getEventId().isBlank()) {
      return event.getEventId();
    }
    if (event != null && event.getOrderNo() != null && !event.getOrderNo().isBlank()) {
      return "STOCK_FREEZE_FAILED:" + event.getOrderNo();
    }
    return "STOCK_FREEZE_FAILED:" + System.currentTimeMillis();
  }
}
