package com.cloud.payment.messaging;

import com.cloud.common.messaging.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;




@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageProducer {

    private final StreamBridge streamBridge;

    


    public boolean sendPaymentSuccessEvent(Long paymentId, Long orderId, String orderNo,
                                           Long userId, BigDecimal amount, String paymentMethod,
                                           String transactionNo, Map<Long, Integer> productQuantityMap) {
        try {
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .orderNo(orderNo)
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod(paymentMethod)
                    .transactionNo(transactionNo)
                    .productQuantityMap(productQuantityMap)
                    .timestamp(System.currentTimeMillis())
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PAYMENT_SUCCESS")
                    .build();

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, orderNo);
            headers.put(MessageConst.PROPERTY_TAGS, "PAYMENT_SUCCESS");
            headers.put("eventId", event.getEventId());
            headers.put("eventType", event.getEventType());

            Message<PaymentSuccessEvent> message = MessageBuilder
                    .withPayload(event)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("paymentSuccessProducer-out-0", message);
            if (result) {
                

            } else {
                log.error("Payment-success event send failed: paymentId={}, orderId={}, orderNo={}",
                        paymentId, orderId, orderNo);
            }
            return result;
        } catch (Exception e) {
            log.error("Send payment-success event failed: paymentId={}, orderId={}, orderNo={}",
                    paymentId, orderId, orderNo, e);
            return false;
        }
    }

    


    public boolean sendRefundCompletedEvent(Long refundId, String refundNo, Long orderId,
                                            String orderNo, Long userId, BigDecimal refundAmount,
                                            String refundTransactionNo) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("eventType", "REFUND_COMPLETED");
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("refundId", refundId);
            payload.put("refundNo", refundNo);
            payload.put("orderId", orderId);
            payload.put("orderNo", orderNo);
            payload.put("userId", userId);
            payload.put("refundAmount", refundAmount);
            payload.put("refundTransactionNo", refundTransactionNo);

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refundNo);
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_COMPLETED");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundCompletedProducer-out-0", message);
            if (result) {
                

            } else {
                log.error("Refund-completed event send failed: refundId={}, refundNo={}, orderId={}",
                        refundId, refundNo, orderId);
            }
            return result;
        } catch (Exception e) {
            log.error("Send refund-completed event failed: refundId={}, refundNo={}, orderId={}",
                    refundId, refundNo, orderId, e);
            return false;
        }
    }
}
