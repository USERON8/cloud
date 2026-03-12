package com.cloud.order.messaging;

import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.common.messaging.event.StockRestoreEvent;
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

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, event.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "STOCK_RESTORE");

            Message<StockRestoreEvent> message = MessageBuilder
                    .withPayload(event)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("stockRestoreProducer-out-0", message);

            if (result) {
                

            } else {
                log.error("?? refundNo={}", event.getRefundNo());
            }

            return result;

        } catch (Exception e) {
            log.error("?? refundNo={}", event == null ? null : event.getRefundNo(), e);
            return false;
        }
    }

    public boolean sendRefundProcessEvent(PaymentRefundCommandDTO command) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, command.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_PROCESS");

            Message<PaymentRefundCommandDTO> message = MessageBuilder
                    .withPayload(command)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundProcessProducer-out-0", message);
            if (!result) {
                log.error("?? refundNo={}", command.getRefundNo());
            }
            return result;
        } catch (Exception e) {
            log.error("?? refundNo={}", command == null ? null : command.getRefundNo(), e);
            return false;
        }
    }
}
