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

/**
 * 支付消息生产者
 * 发送支付相关的事件消息
 *
 * @author what's up
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageProducer {

    private final StreamBridge streamBridge;

    /**
     * 发送支付成功事件
     * 通知订单服务完成订单、库存服务扣减库存
     *
     * @param paymentId     支付ID
     * @param orderId       订单ID
     * @param orderNo       订单号
     * @param userId        用户ID
     * @param amount        支付金额
     * @param paymentMethod 支付方式
     * @param transactionNo 支付流水号
     * @return 是否发送成功
     */
    public boolean sendPaymentSuccessEvent(Long paymentId, Long orderId, String orderNo,
                                           Long userId, BigDecimal amount, String paymentMethod,
                                           String transactionNo) {
        try {
            // 构建事件
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .orderNo(orderNo)
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod(paymentMethod)
                    .transactionNo(transactionNo)
                    .timestamp(System.currentTimeMillis())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            // 构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_KEYS, orderNo);
            headers.put(MessageConst.PROPERTY_TAGS, "PAYMENT_SUCCESS");
            headers.put("eventId", event.getEventId());
            headers.put("eventType", "PAYMENT_SUCCESS");

            // 构建消息
            Message<PaymentSuccessEvent> message = MessageBuilder
                    .withPayload(event)
                    .copyHeaders(headers)
                    .build();

            // 发送到payment-success topic
            boolean result = streamBridge.send("paymentSuccessProducer-out-0", message);

            if (result) {
                log.info("✅ 支付成功事件发送成功: paymentId={}, orderId={}, orderNo={}, amount={}, eventId={}",
                        paymentId, orderId, orderNo, amount, event.getEventId());
            } else {
                log.error("❌ 支付成功事件发送失败: paymentId={}, orderId={}, orderNo={}",
                        paymentId, orderId, orderNo);
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 发送支付成功事件异常: paymentId={}, orderId={}, orderNo={}",
                    paymentId, orderId, orderNo, e);
            return false;
        }
    }
}
