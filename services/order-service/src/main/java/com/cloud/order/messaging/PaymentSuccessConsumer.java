package com.cloud.order.messaging;

import com.cloud.common.domain.dto.stock.StockOperateCommandDTO;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.order.dto.OrderAggregateResponse;
import com.cloud.order.entity.OrderItem;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.support.StockReservationRemoteService;
import com.cloud.common.metrics.TradeMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessConsumer {

    private static final String NS_PAYMENT_SUCCESS = "order:payment:success";
    private static final Set<String> CONFIRMABLE_STATUSES = Set.of("STOCK_RESERVED");
    private static final String WS_CHANNEL_PREFIX = "ws:message:";

    private final MessageIdempotencyService messageIdempotencyService;
    private final OrderMainMapper orderMainMapper;
    private final OrderService orderService;
    private final StockReservationRemoteService stockReservationRemoteService;
    private final TradeMetrics tradeMetrics;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Bean
    public Consumer<Message<PaymentSuccessEvent>> paymentSuccessConsumer() {
        return message -> {
            PaymentSuccessEvent event = message.getPayload();
            String eventId = getEventId(event);
            if (!messageIdempotencyService.tryAcquire(NS_PAYMENT_SUCCESS, eventId)) {
                log.warn("Duplicate payment success event, skip: eventId={}", eventId);
                return;
            }

            try {
                if (event == null || event.getOrderNo() == null || event.getOrderNo().isBlank()) {
                    tradeMetrics.incrementMessageConsume("payment_success", "failed");
                    messageIdempotencyService.markSuccess(NS_PAYMENT_SUCCESS, eventId);
                    return;
                }

                OrderMain mainOrder = orderMainMapper.selectActiveByOrderNo(event.getOrderNo());
                if (mainOrder == null) {
                    tradeMetrics.incrementMessageConsume("payment_success", "failed");
                    messageIdempotencyService.markSuccess(NS_PAYMENT_SUCCESS, eventId);
                    return;
                }

                OrderAggregateResponse aggregate = orderService.getOrderAggregate(mainOrder.getId());
                if (aggregate == null || aggregate.getSubOrders() == null) {
                    tradeMetrics.incrementMessageConsume("payment_success", "failed");
                    messageIdempotencyService.markSuccess(NS_PAYMENT_SUCCESS, eventId);
                    return;
                }

                String targetSubOrderNo = event.getSubOrderNo();
                for (OrderAggregateResponse.SubOrderWithItems wrapped : aggregate.getSubOrders()) {
                    OrderSub subOrder = wrapped.getSubOrder();
                    if (subOrder == null) {
                        continue;
                    }
                    if (targetSubOrderNo != null && !targetSubOrderNo.isBlank()
                            && !targetSubOrderNo.equals(subOrder.getSubOrderNo())) {
                        continue;
                    }
                    if (!CONFIRMABLE_STATUSES.contains(subOrder.getOrderStatus())) {
                        continue;
                    }
                    confirmStockForSubOrder(subOrder, wrapped.getItems(), event.getOrderNo());
                    orderService.advanceSubOrderStatus(subOrder.getId(), "PAY");
                }

                pushPaymentSuccessMessage(event);
                tradeMetrics.incrementMessageConsume("payment_success", "success");
                messageIdempotencyService.markSuccess(NS_PAYMENT_SUCCESS, eventId);
            } catch (Exception ex) {
                tradeMetrics.incrementMessageConsume("payment_success", "retry");
                log.error("Handle payment success failed: eventId={}, orderNo={}", eventId,
                        event == null ? null : event.getOrderNo(), ex);
                throw new RuntimeException("Handle payment success failed", ex);
            }
        };
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

    private String getEventId(PaymentSuccessEvent event) {
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
            Map<String, Object> message = Map.of(
                    "type", "PAYMENT_SUCCESS",
                    "userId", String.valueOf(event.getUserId()),
                    "data", Map.of(
                            "orderId", event.getOrderNo(),
                            "subOrderNo", event.getSubOrderNo()
                    ),
                    "timestamp", Instant.now().toEpochMilli()
            );
            String payload = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(WS_CHANNEL_PREFIX + event.getUserId(), payload);
        } catch (Exception ex) {
            log.warn("Send payment success WS message failed: orderNo={}", event.getOrderNo(), ex);
        }
    }
}
