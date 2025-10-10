package com.cloud.log.messaging.consumer;

import com.cloud.common.domain.event.payment.PaymentChangeEvent;
import com.cloud.log.messaging.processor.PaymentEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 支付事件消费者
 * 负责消费支付变更事件并存储到Elasticsearch
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class PaymentEventConsumer {

    private final PaymentEventProcessor paymentEventProcessor;

    /**
     * 支付事件消费函数
     * 使用官方标准的函数式编程模型
     */
    @Bean
    public Consumer<Message<PaymentChangeEvent>> paymentConsumer() {
        return message -> {
            try {
                // 获取消息内容和头信息
                PaymentChangeEvent event = message.getPayload();
                String traceId = (String) message.getHeaders().get("traceId");
                String eventType = (String) message.getHeaders().get("eventType");
                String tag = (String) message.getHeaders().get("rocketmq_TAGS");

                log.info("🔔 接收到支付事件 - 事件类型: {}, 支付ID: {}, 用户ID: {}, 订单ID: {}, Tag: {}, TraceId: {}",
                        eventType, event.getPaymentId(), event.getUserId(), event.getOrderId(), tag, traceId);

                // 处理支付事件
                paymentEventProcessor.processPaymentEvent(event, message.getHeaders());

                log.info("✅ 支付事件处理完成 - 支付ID: {}, 事件类型: {}, TraceId: {}",
                        event.getPaymentId(), eventType, traceId);

            } catch (Exception e) {
                log.error("❌ 处理支付事件时发生异常: {}", e.getMessage(), e);
                // 根据业务需要决定是否抛出异常进行重试
                throw new RuntimeException("支付事件处理失败", e);
            }
        };
    }
}
