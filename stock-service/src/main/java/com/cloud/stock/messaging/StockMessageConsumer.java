package com.cloud.stock.messaging;

import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;




@Slf4j
@Component
@RequiredArgsConstructor
public class StockMessageConsumer {

    private static final String ORDER_CREATED_NAMESPACE = "stock:orderCreated";
    private static final String PAYMENT_SUCCESS_NAMESPACE = "stock:paymentSuccess";
    private static final String STOCK_RESTORE_NAMESPACE = "stock:stockRestore";
    private static final String ORDER_RESERVED_KEY_PREFIX = "stock:order:reserved:";
    private static final String ORDER_CONFIRMED_KEY_PREFIX = "stock:order:confirmed:";
    private static final String ORDER_ROLLED_BACK_KEY_PREFIX = "stock:order:rolledback:";
    private static final Duration ORDER_STOCK_STATE_TTL = Duration.ofDays(7);

    private final StockService stockService;
    private final StockMessageProducer stockMessageProducer;
    private final MessageIdempotencyService messageIdempotencyService;
    private final StringRedisTemplate stringRedisTemplate;

    


    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();

            


            try {
                String eventId = event.getEventId();
                if (!messageIdempotencyService.tryAcquire(ORDER_CREATED_NAMESPACE, eventId)) {
                    log.warn("Duplicate order-created event, skip: orderId={}, orderNo={}, eventId={}",
                            event.getOrderId(), event.getOrderNo(), eventId);
                    return;
                }

                if (stockService.isStockFrozen(event.getOrderId())) {
                    log.warn("Order stock already frozen, skip: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    return;
                }

                Map<Long, Integer> productQuantityMap = event.getProductQuantityMap();
                if (productQuantityMap == null || productQuantityMap.isEmpty()) {
                    throw new RuntimeException("productQuantityMap is empty");
                }

                Map<Long, Integer> frozenSuccessMap = new HashMap<>();
                boolean allSuccess = true;
                String failureReason = null;

                for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
                    Long productId = entry.getKey();
                    Integer quantity = entry.getValue();

                    if (!stockService.checkStockSufficient(productId, quantity)) {
                        failureReason = "Insufficient stock: productId=" + productId + ", quantity=" + quantity;
                        allSuccess = false;
                        break;
                    }

                    boolean success = stockService.reserveStock(productId, quantity);
                    if (!success) {
                        failureReason = "Reserve stock failed: productId=" + productId;
                        allSuccess = false;
                        break;
                    }
                    frozenSuccessMap.put(productId, quantity);
                }

                if (allSuccess) {
                    markOrderState(ORDER_RESERVED_KEY_PREFIX, event.getOrderId());
                    clearOrderState(ORDER_CONFIRMED_KEY_PREFIX, event.getOrderId());
                    clearOrderState(ORDER_ROLLED_BACK_KEY_PREFIX, event.getOrderId());
                    

                } else {
                    log.error("Order stock freeze failed: orderId={}, orderNo={}, reason={}",
                            event.getOrderId(), event.getOrderNo(), failureReason);

                    stockMessageProducer.sendStockFreezeFailedEvent(
                            event.getOrderId(),
                            event.getOrderNo(),
                            failureReason
                    );

                    
                    for (Map.Entry<Long, Integer> frozen : frozenSuccessMap.entrySet()) {
                        try {
                            stockService.releaseReservedStock(frozen.getKey(), frozen.getValue());
                        } catch (Exception rollbackEx) {
                            log.error("Rollback reserved stock failed: orderId={}, productId={}, quantity={}",
                                    event.getOrderId(), frozen.getKey(), frozen.getValue(), rollbackEx);
                        }
                    }
                    clearOrderState(ORDER_RESERVED_KEY_PREFIX, event.getOrderId());
                    markOrderState(ORDER_ROLLED_BACK_KEY_PREFIX, event.getOrderId());
                }

            } catch (Exception e) {
                messageIdempotencyService.release(ORDER_CREATED_NAMESPACE, event.getEventId());
                log.error("Handle order-created event failed: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);

                stockMessageProducer.sendStockFreezeFailedEvent(
                        event.getOrderId(),
                        event.getOrderNo(),
                        "System exception: " + e.getMessage()
                );

                throw new RuntimeException("Handle order-created event failed", e);
            }
        };
    }

    


    @Bean
    public Consumer<Message<PaymentSuccessEvent>> paymentSuccessConsumer() {
        return message -> {
            PaymentSuccessEvent event = message.getPayload();

            


            try {
                String eventId = event.getEventId();
                if (!messageIdempotencyService.tryAcquire(PAYMENT_SUCCESS_NAMESPACE, eventId)) {
                    log.warn("Duplicate payment-success event, skip: orderId={}, orderNo={}, eventId={}",
                            event.getOrderId(), event.getOrderNo(), eventId);
                    return;
                }

                if (stockService.isStockDeducted(event.getOrderId())) {
                    log.warn("Order stock already deducted, skip: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    return;
                }

                Map<Long, Integer> productQuantityMap = event.getProductQuantityMap();
                if (productQuantityMap == null || productQuantityMap.isEmpty()) {
                    log.warn("Payment-success event has empty product map, skip deduction: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    return;
                }

                for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
                    Long productId = entry.getKey();
                    Integer quantity = entry.getValue();
                    boolean success = stockService.confirmReservedStockOut(
                            productId,
                            quantity,
                            event.getOrderId(),
                            event.getOrderNo(),
                            "Payment success confirm stock out"
                    );
                    if (!success) {
                        throw new RuntimeException("Confirm reserved stock out failed, productId=" + productId);
                    }
                }

                markOrderState(ORDER_CONFIRMED_KEY_PREFIX, event.getOrderId());
                clearOrderState(ORDER_RESERVED_KEY_PREFIX, event.getOrderId());
                


            } catch (Exception e) {
                messageIdempotencyService.release(PAYMENT_SUCCESS_NAMESPACE, event.getEventId());
                log.error("Handle payment-success event failed: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);
                throw new RuntimeException("Handle payment-success event failed", e);
            }
        };
    }

    


    @Bean
    public Consumer<Message<Map<String, Object>>> stockRestoreConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();

            Long orderId = ((Number) event.get("orderId")).longValue();
            String orderNo = (String) event.get("orderNo");
            String refundNo = (String) event.get("refundNo");
            @SuppressWarnings("unchecked")
            Map<Long, Integer> productQuantityMap = (Map<Long, Integer>) event.get("productQuantityMap");

            

            try {
                String eventId = (String) event.get("eventId");
                if (!messageIdempotencyService.tryAcquire(STOCK_RESTORE_NAMESPACE, eventId)) {
                    log.warn("Duplicate stock-restore event, skip: orderId={}, refundNo={}, eventId={}",
                            orderId, refundNo, eventId);
                    return;
                }

                if (productQuantityMap == null || productQuantityMap.isEmpty()) {
                    log.warn("No products to restore: orderId={}, refundNo={}", orderId, refundNo);
                    return;
                }

                boolean allSuccess = true;
                StringBuilder failureDetails = new StringBuilder();

                for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
                    Long productId = entry.getKey();
                    Integer quantity = entry.getValue();

                    boolean success = stockService.releaseReservedStock(productId, quantity);
                    if (!success) {
                        allSuccess = false;
                        failureDetails.append("productId=").append(productId)
                                .append(",quantity=").append(quantity).append("; ");
                    }
                }

                if (allSuccess) {
                    clearOrderState(ORDER_RESERVED_KEY_PREFIX, orderId);
                    clearOrderState(ORDER_CONFIRMED_KEY_PREFIX, orderId);
                    markOrderState(ORDER_ROLLED_BACK_KEY_PREFIX, orderId);
                    

                } else {
                    log.error("Stock restore partial failed: orderId={}, refundNo={}, details={}",
                            orderId, refundNo, failureDetails);
                }

            } catch (Exception e) {
                messageIdempotencyService.release(STOCK_RESTORE_NAMESPACE, (String) event.get("eventId"));
                log.error("Handle stock-restore event failed: orderId={}, refundNo={}", orderId, refundNo, e);
                throw new RuntimeException("Handle stock-restore event failed", e);
            }
        };
    }

    private void markOrderState(String keyPrefix, Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(keyPrefix + orderId, "1", ORDER_STOCK_STATE_TTL);
        } catch (Exception e) {
            log.warn("Mark order stock state failed, keyPrefix={}, orderId={}", keyPrefix, orderId, e);
        }
    }

    private void clearOrderState(String keyPrefix, Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(keyPrefix + orderId);
        } catch (Exception e) {
            log.warn("Clear order stock state failed, keyPrefix={}, orderId={}", keyPrefix, orderId, e);
        }
    }
}
