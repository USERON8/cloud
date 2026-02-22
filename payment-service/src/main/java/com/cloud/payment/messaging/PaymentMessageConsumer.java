package com.cloud.payment.messaging;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.event.OrderCreatedEvent;
import com.cloud.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Consumer;




@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageConsumer {

    private static final String ORDER_CREATED_NAMESPACE = "payment:orderCreated";
    private static final String REFUND_PROCESS_NAMESPACE = "payment:refundProcess";

    private final PaymentService paymentService;
    private final PaymentMessageProducer paymentMessageProducer;
    private final MessageIdempotencyService messageIdempotencyService;

    


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

                if (paymentService.isPaymentRecordExists(event.getOrderId())) {
                    log.warn("Payment record already exists, skip: orderId={}, orderNo={}",
                            event.getOrderId(), event.getOrderNo());
                    return;
                }

                PaymentDTO paymentDTO = new PaymentDTO();
                paymentDTO.setOrderId(event.getOrderId());
                paymentDTO.setOrderNo(event.getOrderNo());
                paymentDTO.setUserId(event.getUserId());
                paymentDTO.setAmount(event.getTotalAmount());
                paymentDTO.setPaymentMethod("ALIPAY");
                paymentDTO.setStatus(0);
                paymentDTO.setChannel(1);

                Long paymentId = paymentService.createPayment(paymentDTO);
                if (paymentId == null) {
                    throw new RuntimeException("Create payment failed");
                }

                Boolean success = paymentService.processPaymentSuccess(paymentId);
                if (!Boolean.TRUE.equals(success)) {
                    throw new RuntimeException("Process payment success failed");
                }

                String transactionNo = "TXN" + System.currentTimeMillis() + paymentId;
                boolean sendResult = paymentMessageProducer.sendPaymentSuccessEvent(
                        paymentId,
                        event.getOrderId(),
                        event.getOrderNo(),
                        event.getUserId(),
                        event.getTotalAmount(),
                        "ALIPAY",
                        transactionNo,
                        event.getProductQuantityMap()
                );

                if (sendResult) {
                    

                } else {
                    log.error("Payment-success event send failed: paymentId={}, orderId={}, orderNo={}",
                            paymentId, event.getOrderId(), event.getOrderNo());
                }

            } catch (Exception e) {
                messageIdempotencyService.release(ORDER_CREATED_NAMESPACE, event.getEventId());
                log.error("Handle order-created event failed: orderId={}, orderNo={}",
                        event.getOrderId(), event.getOrderNo(), e);
                throw new RuntimeException("Handle order-created event failed", e);
            }
        };
    }

    


    @Bean
    public Consumer<Message<Map<String, Object>>> refundProcessConsumer() {
        return message -> {
            Map<String, Object> event = message.getPayload();

            Long refundId = ((Number) event.get("refundId")).longValue();
            String refundNo = (String) event.get("refundNo");
            Long orderId = ((Number) event.get("orderId")).longValue();
            String orderNo = (String) event.get("orderNo");
            Long userId = ((Number) event.get("userId")).longValue();
            BigDecimal refundAmount = new BigDecimal(event.get("refundAmount").toString());

            


            try {
                String eventId = (String) event.get("eventId");
                if (!messageIdempotencyService.tryAcquire(REFUND_PROCESS_NAMESPACE, eventId)) {
                    log.warn("Duplicate refund-process event, skip: refundNo={}, eventId={}", refundNo, eventId);
                    return;
                }

                String refundTransactionNo = "REFUND_TXN" + System.currentTimeMillis() + refundId;
                boolean sent = paymentMessageProducer.sendRefundCompletedEvent(
                        refundId,
                        refundNo,
                        orderId,
                        orderNo,
                        userId,
                        refundAmount,
                        refundTransactionNo
                );

                if (sent) {
                    
                } else {
                    log.error("Refund-completed event send failed: refundId={}, refundNo={}", refundId, refundNo);
                }

            } catch (Exception e) {
                messageIdempotencyService.release(REFUND_PROCESS_NAMESPACE, (String) event.get("eventId"));
                log.error("Handle refund-process event failed: refundId={}, refundNo={}, orderId={}",
                        refundId, refundNo, orderId, e);
                throw new RuntimeException("Handle refund-process event failed", e);
            }
        };
    }
}
