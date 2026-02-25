package com.cloud.order.messaging;

import com.cloud.order.module.entity.Refund;
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
public class RefundMessageProducer {

    private final StreamBridge streamBridge;

    






    public boolean sendRefundCreatedEvent(Refund refund) {
        try {
            Map<String, Object> payload = buildRefundPayload(refund, "REFUND_CREATED");

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refund.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_CREATED");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundCreatedProducer-out-0", message);

            if (result) {
                

            } else {
                log.error("?? refundId={}, refundNo={}",
                        refund.getId(), refund.getRefundNo());
            }

            return result;

        } catch (Exception e) {
            log.error("?? refundId={}, refundNo={}",
                    refund.getId(), refund.getRefundNo(), e);
            return false;
        }
    }

    







    public boolean sendRefundAuditedEvent(Refund refund, Boolean approved) {
        try {
            Map<String, Object> payload = buildRefundPayload(refund, "REFUND_AUDITED");
            payload.put("approved", approved);

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refund.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_AUDITED");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundAuditedProducer-out-0", message);

            if (result) {
                

            }

            return result;

        } catch (Exception e) {
            log.error("?? refundId={}", refund.getId(), e);
            return false;
        }
    }

    






    public boolean sendRefundProcessEvent(Refund refund) {
        try {
            Map<String, Object> payload = buildRefundPayload(refund, "REFUND_PROCESS");

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refund.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_PROCESS");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundProcessProducer-out-0", message);

            if (result) {
                

            } else {
                log.error("?? refundId={}", refund.getId());
            }

            return result;

        } catch (Exception e) {
            log.error("?? refundId={}", refund.getId(), e);
            return false;
        }
    }

    





    public boolean sendRefundCancelledEvent(Refund refund) {
        try {
            Map<String, Object> payload = buildRefundPayload(refund, "REFUND_CANCELLED");

            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, refund.getRefundNo());
            headers.put(MessageConst.PROPERTY_TAGS, "REFUND_CANCELLED");

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .copyHeaders(headers)
                    .build();

            boolean result = streamBridge.send("refundCancelledProducer-out-0", message);

            if (result) {
                
            }

            return result;

        } catch (Exception e) {
            log.error("?? refundId={}", refund.getId(), e);
            return false;
        }
    }

    


    private Map<String, Object> buildRefundPayload(Refund refund, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", eventType);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("refundId", refund.getId());
        payload.put("refundNo", refund.getRefundNo());
        payload.put("orderId", refund.getOrderId());
        payload.put("orderNo", refund.getOrderNo());
        payload.put("userId", refund.getUserId());
        payload.put("merchantId", refund.getMerchantId());
        payload.put("refundType", refund.getRefundType());
        payload.put("refundAmount", refund.getRefundAmount());
        payload.put("refundReason", refund.getRefundReason());
        payload.put("status", refund.getStatus());
        return payload;
    }
}
