package com.cloud.order.messaging.producer;

import com.cloud.common.domain.event.OrderChangeEvent;
import com.cloud.common.exception.MessageSendException;
import com.cloud.common.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单事件生产者
 * 负责发送订单变更事件到RocketMQ
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class OrderEventProducer {

    private static final String ORDER_BINDING_NAME = "order-producer-out-0";
    private final StreamBridge streamBridge;

    /**
     * 发送订单创建事件
     */
    public void sendOrderCreatedEvent(OrderChangeEvent event) {
        sendOrderEvent(event, "ORDER_CREATED", "order-created");
    }

    /**
     * 发送订单支付事件
     */
    public void sendOrderPaidEvent(OrderChangeEvent event) {
        sendOrderEvent(event, "ORDER_PAID", "order-paid");
    }

    /**
     * 发送订单发货事件
     */
    public void sendOrderShippedEvent(OrderChangeEvent event) {
        sendOrderEvent(event, "ORDER_SHIPPED", "order-shipped");
    }

    /**
     * 发送订单完成事件
     */
    public void sendOrderCompletedEvent(OrderChangeEvent event) {
        sendOrderEvent(event, "ORDER_COMPLETED", "order-completed");
    }

    /**
     * 发送订单取消事件
     */
    public void sendOrderCancelledEvent(OrderChangeEvent event) {
        sendOrderEvent(event, "ORDER_CANCELLED", "order-cancelled");
    }

    /**
     * 发送订单退款事件
     */
    public void sendOrderRefundedEvent(OrderChangeEvent event) {
        sendOrderEvent(event, "ORDER_REFUNDED", "order-refunded");
    }

    /**
     * 发送订单状态变更事件
     */
    public void sendOrderStatusChangedEvent(OrderChangeEvent event) {
        sendOrderEvent(event, "ORDER_STATUS_CHANGED", "order-status-changed");
    }

    /**
     * 发送订单超时事件
     */
    public void sendOrderTimeoutEvent(OrderChangeEvent event) {
        sendOrderEvent(event, "ORDER_TIMEOUT", "order-timeout");
    }

    /**
     * 统一发送订单事件的内部方法
     * 按照官方示例标准实现，使用GenericMessage和MessageConst
     */
    private void sendOrderEvent(OrderChangeEvent event, String changeType, String tag) {
        try {
            // 按照官方示例构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "ORDER_" + event.getOrderId());
            headers.put("eventType", changeType);
            headers.put("traceId", generateTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            // 使用GenericMessage构建消息（官方标准方式）
            Message<OrderChangeEvent> message = new GenericMessage<>(event, headers);
            String traceId = (String) headers.get("traceId");

            // 记录发送日志
            MessageUtils.logMessageSend("order-events", event, traceId);

            // 发送消息
            boolean sent = streamBridge.send(ORDER_BINDING_NAME, message);

            if (sent) {
                log.info("✅ 订单事件发送成功 - 事件类型: {}, 订单ID: {}, 用户ID: {}, Tag: {}, TraceId: {}",
                        changeType, event.getOrderId(), event.getUserId(), tag, traceId);
            } else {
                log.error("❌ 订单事件发送失败 - 事件类型: {}, 订单ID: {}, TraceId: {}",
                        changeType, event.getOrderId(), traceId);
                throw new MessageSendException("订单事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送订单事件时发生异常 - 事件类型: {}, 订单ID: {}, 错误: {}",
                    changeType, event.getOrderId(), e.getMessage(), e);
            throw new MessageSendException("发送订单事件异常", e);
        }
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
