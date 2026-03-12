package com.cloud.payment.messaging;

import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.messaging.event.RefundCompletedEvent;
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
public class PaymentMessageProducer {

    private final StreamBridge streamBridge;

    public boolean sendPaymentSuccessEvent(PaymentSuccessEvent event) {
        if (event == null) {
            return false;
        }
        try {
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(System.currentTimeMillis());
            }
            if (event.getEventType() == null || event.getEventType().isBlank()) {
                event.setEventType("PAYMENT_SUCCESS");
            }

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, event.getOrderNo());
            headers.put(MessageConst.PROPERTY_TAGS, "PAYMENT_SUCCESS");
            headers.put("eventId", event.getEventId());
            headers.put("eventType", event.getEventType());

            Message<PaymentSuccessEvent> message = MessageBuilder.withPayload(event)
                    .copyHeaders(headers)
                    .build();

            return streamBridge.send("paymentSuccessProducer-out-0", message);
        } catch (Exception ex) {
            log.error("Send payment success event failed: orderNo={}", event.getOrderNo(), ex);
            return false;
        }
    }

    public boolean sendRefundCompletedEvent(RefundCompletedEvent event) {
        if (event == null) {
            return false;
        }
        try {
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(System.currentTimeMillis());
            }
            if (event.getEventType() == null || event.getEventType().isBlank()) {
                event.setEventType("REFUND_COMPLETED");
            }

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, event.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_COMPLETED");
            headers.put("eventId", event.getEventId());
            headers.put("eventType", event.getEventType());

            Message<RefundCompletedEvent> message = MessageBuilder.withPayload(event)
                    .copyHeaders(headers)
                    .build();

            return streamBridge.send("refundCompletedProducer-out-0", message);
        } catch (Exception ex) {
            log.error("Send refund completed event failed: refundNo={}", event.getRefundNo(), ex);
            return false;
        }
    }
}
