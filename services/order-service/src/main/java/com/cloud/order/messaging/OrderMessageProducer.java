package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.common.messaging.event.StockRestoreEvent;
import com.cloud.common.messaging.outbox.OutboxEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;







@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;








    public boolean sendOrderCreatedEvent(OrderCreatedEvent event) {
        try {

            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(System.currentTimeMillis());
            }
            if (event.getEventType() == null || event.getEventType().isBlank()) {
                event.setEventType("ORDER_CREATED");
            }

            String payload = objectMapper.writeValueAsString(event);
            outboxEventService.enqueue(
                    "ORDER",
                    event.getOrderNo(),
                    event.getEventType(),
                    payload,
                    event.getEventId()
            );
            return true;

        } catch (Exception e) {
            log.error("?? orderId={}, orderNo={}",
                    event.getOrderId(), event.getOrderNo(), e);
            return false;
        }
    }









    public boolean sendOrderCancelledEvent(Long orderId, String orderNo, String reason) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", orderId);
            payload.put("orderNo", orderNo);
            payload.put("reason", reason);
            payload.put("timestamp", System.currentTimeMillis());
            String eventId = UUID.randomUUID().toString();
            payload.put("eventId", eventId);
            payload.put("eventType", "ORDER_CANCELLED");
            String payloadJson = objectMapper.writeValueAsString(payload);

            outboxEventService.enqueue(
                    "ORDER",
                    orderNo,
                    "ORDER_CANCELLED",
                    payloadJson,
                    eventId
            );
            return true;

        } catch (Exception e) {
            log.error("?? orderId={}, orderNo={}",
                    orderId, orderNo, e);
            return false;
        }
    }












    public boolean sendStockRestoreEvent(StockRestoreEvent event) {
        try {
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(System.currentTimeMillis());
            }
            if (event.getEventType() == null || event.getEventType().isBlank()) {
                event.setEventType("STOCK_RESTORE");
            }
            String payload = objectMapper.writeValueAsString(event);
            outboxEventService.enqueue(
                    "REFUND",
                    event.getRefundNo(),
                    event.getEventType(),
                    payload,
                    event.getEventId()
            );
            return true;

        } catch (Exception e) {
            log.error("?? refundNo={}", event == null ? null : event.getRefundNo(), e);
            return false;
        }
    }

}
