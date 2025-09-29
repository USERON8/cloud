package com.cloud.payment.messaging.consumer;

import com.cloud.common.domain.event.payment.PaymentRecordCreateEvent;
import com.cloud.common.exception.MessageConsumeException;
import com.cloud.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 支付记录创建事件消费者
 * 负责消费支付记录创建事件，创建支付记录准备支付流程
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class PaymentRecordCreateEventConsumer {

    private final PaymentService paymentService;

    /**
     * 支付记录创建消费者函数
     * 对应绑定名称: paymentRecordCreate-in-0
     */
    @Bean("paymentRecordCreateMessageConsumer")
    public Consumer<Message<PaymentRecordCreateEvent>> paymentRecordCreateConsumer() {
        return message -> {
            try {
                PaymentRecordCreateEvent event = message.getPayload();
                String traceId = event.getTraceId();
                Long orderId = event.getOrderId();
                String orderNo = event.getOrderNo();

                log.info("📥 接收到支付记录创建消息 - 订单ID: {}, 订单号: {}, 支付金额: {}, 支付方式: {}, 追踪ID: {}",
                        orderId, orderNo, event.getPaymentAmount(), event.getPaymentMethod(), traceId);

                // 1. 幂等性检查
                if (paymentService.isPaymentRecordExists(orderId)) {
                    log.warn("⚠️ 支付记录已存在，跳过处理 - 订单ID: {}, 追踪ID: {}", orderId, traceId);
                    return;
                }

                // 2. 创建支付记录
                boolean created = paymentService.createPaymentRecord(event);

                if (created) {
                    log.info("✅ 支付记录创建成功 - 订单ID: {}, 订单号: {}, 支付金额: {}, 追踪ID: {}",
                            orderId, orderNo, event.getPaymentAmount(), traceId);
                } else {
                    log.error("❌ 支付记录创建失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                            orderId, orderNo, traceId);
                    throw new MessageConsumeException("支付记录创建失败", null);
                }

            } catch (Exception e) {
                log.error("❌ 处理支付记录创建消息时发生异常: {}", e.getMessage(), e);
                throw new MessageConsumeException("处理支付记录创建消息异常", e);
            }
        };
    }
}
