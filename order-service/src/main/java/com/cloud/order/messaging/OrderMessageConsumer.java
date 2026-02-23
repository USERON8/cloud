package com.cloud.order.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.StockFreezeFailedEvent;
import com.cloud.order.enums.OrderRefundStatusEnum;
import com.cloud.order.module.entity.Order;
import com.cloud.order.module.entity.OrderItem;
import com.cloud.order.service.OrderItemService;
import com.cloud.order.service.OrderService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;




@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private static final String PAYMENT_SUCCESS_NAMESPACE = "order:paymentSuccess";
    private static final String STOCK_FREEZE_FAILED_NAMESPACE = "order:stockFreezeFailed";
    private static final String REFUND_COMPLETED_NAMESPACE = "order:refundCompleted";

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final OrderMessageProducer orderMessageProducer;
    private final MessageIdempotencyService messageIdempotencyService;
    private final MeterRegistry meterRegistry;

    


    @Bean
    public Consumer<Message<PaymentSuccessEvent>> paymentSuccessConsumer() {
        return message -> {
            PaymentSuccessEvent event = message.getPayload();

            


            try {
                String eventId = event.getEventId();
                if (!messageIdempotencyService.tryAcquire(PAYMENT_SUCCESS_NAMESPACE, eventId)) {
                    log.warn("Duplicate payment-success event, skip: orderId={}, orderNo={}, eventId={}",
                            event.getOrderId(), event.getOrderNo(), eventId);
                    recordMessageMetric("PAYMENT_SUCCESS", "success");
                    return;
                }

                boolean success = orderService.updateOrderStatusAfterPayment(
                        event.getOrderId(),
                        event.getPaymentId(),
                        event.getTransactionNo()
                );

                if (success) {
                    recordMessageMetric("PAYMENT_SUCCESS", "success");
                } else {
                    log.error("Order payment status update failed: orderId={}, orderNo={}", event.getOrderId(), event.getOrderNo());
                    recordMessageMetric("PAYMENT_SUCCESS", "failed");
                }
            } catch (Exception e) {
                messageIdempotencyService.release(PAYMENT_SUCCESS_NAMESPACE, event.getEventId());
                log.error("Handle payment-success event failed: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);
                recordMessageMetric("PAYMENT_SUCCESS", "retry");
                throw new RuntimeException("Handle payment-success event failed", e);
            }
        };
    }

    


    @Bean
    public Consumer<Message<StockFreezeFailedEvent>> stockFreezeFailedConsumer() {
        return message -> {
            StockFreezeFailedEvent event = message.getPayload();

            log.warn("Receive stock-freeze-failed event: orderId={}, orderNo={}, reason={}",
                    event.getOrderId(), event.getOrderNo(), event.getReason());

            try {
                String eventId = event.getEventId();
                if (!messageIdempotencyService.tryAcquire(STOCK_FREEZE_FAILED_NAMESPACE, eventId)) {
                    log.warn("Duplicate stock-freeze-failed event, skip: orderId={}, orderNo={}, eventId={}",
                            event.getOrderId(), event.getOrderNo(), eventId);
                    recordMessageMetric("STOCK_FREEZE_FAILED", "success");
                    return;
                }

                boolean success = orderService.cancelOrderDueToStockFreezeFailed(
                        event.getOrderId(),
                        event.getReason()
                );

                if (success) {
                    recordMessageMetric("STOCK_FREEZE_FAILED", "success");

                } else {
                    log.error("Order cancel failed after stock-freeze-failed: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    recordMessageMetric("STOCK_FREEZE_FAILED", "failed");
                }
            } catch (Exception e) {
                messageIdempotencyService.release(STOCK_FREEZE_FAILED_NAMESPACE, event.getEventId());
                log.error("Handle stock-freeze-failed event failed: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);
                recordMessageMetric("STOCK_FREEZE_FAILED", "retry");
                throw new RuntimeException("Handle stock-freeze-failed event failed", e);
            }
        };
    }

    


    @Bean
    public Consumer<Message<Map<String, Object>>> refundCompletedConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();

            Long orderId = ((Number) event.get("orderId")).longValue();
            String orderNo = (String) event.get("orderNo");
            Long refundId = ((Number) event.get("refundId")).longValue();
            String refundNo = (String) event.get("refundNo");

            


            try {
                String eventId = (String) event.get("eventId");
                if (!messageIdempotencyService.tryAcquire(REFUND_COMPLETED_NAMESPACE, eventId)) {
                    log.warn("Duplicate refund-completed event, skip: orderId={}, refundNo={}, eventId={}",
                            orderId, refundNo, eventId);
                    recordMessageMetric("REFUND_COMPLETED", "success");
                    return;
                }

                LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderItem::getOrderId, orderId);
                List<OrderItem> orderItems = orderItemService.list(wrapper);

                if (orderItems == null || orderItems.isEmpty()) {
                    log.warn("No order items found, skip stock restore event: orderId={}", orderId);
                    recordMessageMetric("REFUND_COMPLETED", "failed");
                    recordRefundMetric("failed");
                    return;
                }

                Map<Long, Integer> productQuantityMap = new HashMap<>();
                for (OrderItem item : orderItems) {
                    productQuantityMap.put(item.getProductId(), item.getQuantity());
                }

                boolean sent = orderMessageProducer.sendStockRestoreEvent(
                        orderId,
                        orderNo,
                        refundId,
                        refundNo,
                        productQuantityMap
                );

                if (sent) {
                    recordMessageMetric("REFUND_COMPLETED", "success");
                } else {
                    log.error("Stock-restore event send failed: orderId={}, refundNo={}", orderId, refundNo);
                    recordMessageMetric("REFUND_COMPLETED", "failed");
                    recordRefundMetric("failed");
                }

                Order order = orderService.getById(orderId);
                if (order != null) {
                    order.setRefundStatus(OrderRefundStatusEnum.REFUND_SUCCESS.getCode());
                    order.setUpdatedAt(LocalDateTime.now());
                    orderService.updateById(order);
                    if (sent) {
                        recordRefundMetric("success");
                    }
                } else {
                    log.warn("Order not found when update refund status: orderId={}", orderId);
                    recordRefundMetric("failed");
                }

            } catch (Exception e) {
                messageIdempotencyService.release(REFUND_COMPLETED_NAMESPACE, (String) event.get("eventId"));
                log.error("Handle refund-completed event failed: orderId={}, refundNo={}", orderId, refundNo, e);
                recordMessageMetric("REFUND_COMPLETED", "retry");
                recordRefundMetric("failed");
                throw new RuntimeException("Handle refund-completed event failed", e);
            }
        };
    }

    private void recordMessageMetric(String eventType, String result) {
        meterRegistry.counter(
                "trade.message.consume",
                "service", "order-service",
                "eventType", eventType,
                "result", result
        ).increment();
    }

    private void recordRefundMetric(String result) {
        meterRegistry.counter(
                "trade.refund",
                "service", "order-service",
                "result", result
        ).increment();
    }
}
