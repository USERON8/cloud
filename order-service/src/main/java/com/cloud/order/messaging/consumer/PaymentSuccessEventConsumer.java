package com.cloud.order.messaging.consumer;

import com.cloud.common.domain.event.payment.PaymentSuccessEvent;
import com.cloud.order.service.OrderBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 支付成功事件消费者
 * 监听支付成功事件，自动更新订单状态并发布订单完成事件
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessEventConsumer {

    private final OrderBusinessService orderBusinessService;

    /**
     * 支付成功事件消费者
     */
    @Bean("paymentSuccessConsumer")
    public Consumer<Message<PaymentSuccessEvent>> paymentSuccessEventConsumer() {
        return message -> {
            PaymentSuccessEvent event = message.getPayload();
            try {
                log.info("🔔 接收到支付成功事件 - 订单ID: {}, 支付ID: {}, 支付金额: {}, 追踪ID: {}",
                        event.getOrderId(), event.getPaymentId(), event.getPaymentAmount(), event.getTraceId());

                // 校验必要参数
                if (event.getOrderId() == null) {
                    log.error("❌ 支付成功事件参数错误 - 订单ID为空, 追踪ID: {}", event.getTraceId());
                    return;
                }

                if (event.getPaymentAmount() == null) {
                    log.error("❌ 支付成功事件参数错误 - 支付金额为空, 订单ID: {}, 追踪ID: {}",
                            event.getOrderId(), event.getTraceId());
                    return;
                }

                // 处理支付成功事件
                boolean success = orderBusinessService.handlePaymentSuccess(
                        event.getOrderId(),
                        event.getPaymentId(),
                        event.getPaymentAmount()
                );

                if (success) {
                    log.info("✅ 支付成功事件处理完成 - 订单ID: {}, 支付ID: {}, 追踪ID: {}",
                            event.getOrderId(), event.getPaymentId(), event.getTraceId());
                } else {
                    log.error("❌ 支付成功事件处理失败 - 订单ID: {}, 支付ID: {}, 追踪ID: {}",
                            event.getOrderId(), event.getPaymentId(), event.getTraceId());
                    // 支付成功但订单处理失败的情况需要告警
                    throw new RuntimeException("支付成功事件处理失败，需要人工干预");
                }

            } catch (Exception e) {
                log.error("❌ 处理支付成功事件异常 - 订单ID: {}, 支付ID: {}, 追踪ID: {}, 错误: {}",
                        event != null ? event.getOrderId() : "null",
                        event != null ? event.getPaymentId() : "null",
                        event != null ? event.getTraceId() : "null",
                        e.getMessage(), e);

                // 这里可以添加失败重试机制或者死信队列处理
                // 暂时记录错误日志，不抛出异常避免消息重复消费
            }
        };
    }
}
