package com.cloud.payment.messaging;

import com.cloud.common.messaging.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;



@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessTxProducer {

    private static final String DESTINATION = "payment-success:PAYMENT_SUCCESS";

    private final RocketMQTemplate rocketMQTemplate;

    public void send(PaymentSuccessEvent event) {
        if (event == null) {
            return;
        }
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            event.setEventType("PAYMENT_SUCCESS");
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(System.currentTimeMillis());
        }

        Message<PaymentSuccessEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(RocketMQHeaders.KEYS, event.getOrderNo())
                .setHeader("eventId", event.getEventId())
                .setHeader("eventType", event.getEventType())
                .build();
        try {
            rocketMQTemplate.sendMessageInTransaction(DESTINATION, message, event);
        } catch (Exception ex) {
            log.error("Send payment success tx message failed: orderNo={}", event.getOrderNo(), ex);
            throw ex;
        }
    }
}
