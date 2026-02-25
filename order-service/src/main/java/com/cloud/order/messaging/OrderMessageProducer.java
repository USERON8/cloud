package com.cloud.order.messaging;

import com.cloud.common.messaging.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;







@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final StreamBridge streamBridge;

    






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

            
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, event.getOrderNo());
            headers.put(MessageConst.PROPERTY_TAGS, "ORDER_CREATED");
            headers.put("eventId", event.getEventId());
            headers.put("eventType", event.getEventType());

            
            Message<OrderCreatedEvent> message = MessageBuilder
                    .withPayload(event)
                    .copyHeaders(headers)
                    .build();

            
            boolean result = streamBridge.send("orderCreatedProducer-out-0", message);

            if (result) {
                

            } else {
                log.error("?? orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo());
            }

            return result;

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
            payload.put("eventId", UUID.randomUUID().toString());

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, orderNo);
            headers.put(MessageConst.PROPERTY_TAGS, "ORDER_CANCELLED");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("orderCancelledProducer-out-0", message);

            if (result) {
                

            } else {
                log.error("?? orderId={}, orderNo={}",
                        orderId, orderNo);
            }

            return result;

        } catch (Exception e) {
            log.error("?? orderId={}, orderNo={}",
                    orderId, orderNo, e);
            return false;
        }
    }

    










    public boolean sendStockRestoreEvent(Long orderId, String orderNo, Long refundId,
                                         String refundNo, Map<Long, Integer> productQuantityMap) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("eventType", "STOCK_RESTORE");
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("orderId", orderId);
            payload.put("orderNo", orderNo);
            payload.put("refundId", refundId);
            payload.put("refundNo", refundNo);
            payload.put("productQuantityMap", productQuantityMap);

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refundNo);
            headers.put(MessageConst.PROPERTY_TAGS, "STOCK_RESTORE");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("stockRestoreProducer-out-0", message);

            if (result) {
                

            } else {
                log.error("?? orderId={}, refundNo={}",
                        orderId, refundNo);
            }

            return result;

        } catch (Exception e) {
            log.error("?? orderId={}, refundNo={}",
                    orderId, refundNo, e);
            return false;
        }
    }
}
