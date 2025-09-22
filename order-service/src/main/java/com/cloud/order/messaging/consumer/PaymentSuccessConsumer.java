package com.cloud.order.messaging.consumer;

import com.cloud.common.domain.event.PaymentSuccessEvent;
import com.cloud.common.exception.MessageConsumeException;
import com.cloud.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 支付成功消费者
 * 负责消费支付成功事件，更新订单状态
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class PaymentSuccessConsumer {

    private final OrderService orderService;

    /**
     * 支付成功消费者函数
     * 对应绑定名称: paymentSuccessConsumer-in-0
     */
    @Bean
    public Consumer<Message<PaymentSuccessEvent>> paymentSuccessConsumer() {
        return message -> {
            try {
                PaymentSuccessEvent event = message.getPayload();
                String traceId = event.getTraceId();
                Long orderId = event.getOrderId();
                String orderNo = event.getOrderNo();
                Long paymentId = event.getPaymentId();

                log.info("📥 接收到支付成功消息 - 订单ID: {}, 订单号: {}, 支付ID: {}, 追踪ID: {}", 
                        orderId, orderNo, paymentId, traceId);

                // 1. 幂等性检查
                if (orderService.isOrderPaid(orderId)) {
                    log.warn("⚠️ 订单已支付，跳过处理 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
                    return;
                }

                // 2. 更新订单状态为已支付
                boolean updated = orderService.updateOrderToPaid(event);

                if (updated) {
                    log.info("✅ 订单状态更新成功 - 订单ID: {}, 订单号: {}, 支付金额: {}, 追踪ID: {}", 
                            orderId, orderNo, event.getPaymentAmount(), traceId);
                } else {
                    log.error("❌ 订单状态更新失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}", 
                            orderId, orderNo, traceId);
                    throw new MessageConsumeException("订单状态更新失败", null);
                }

            } catch (Exception e) {
                log.error("❌ 处理支付成功消息时发生异常: {}", e.getMessage(), e);
                throw new MessageConsumeException("处理支付成功消息异常", e);
            }
        };
    }
}
