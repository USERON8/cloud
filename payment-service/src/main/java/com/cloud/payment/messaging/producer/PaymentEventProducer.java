package com.cloud.payment.messaging.producer;

import com.cloud.common.constant.MessageTopicConstants;
import com.cloud.common.domain.event.RefundCreateEvent;
import com.cloud.common.domain.event.payment.PaymentChangeEvent;
import com.cloud.common.domain.event.payment.PaymentSuccessEvent;
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
 * 支付事件生产者
 * 负责发送支付变更事件到RocketMQ
 * 基于阿里巴巴官方示例标准实现
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class PaymentEventProducer {

    private static final String PAYMENT_BINDING_NAME = "payment-producer-out-0";
    private final StreamBridge streamBridge;

    /**
     * 发送支付创建事件
     */
    public void sendPaymentCreatedEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "PAYMENT_CREATED", "payment-created");
    }

    /**
     * 发送支付处理中事件
     */
    public void sendPaymentProcessingEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "PAYMENT_PROCESSING", "payment-processing");
    }

    /**
     * 发送支付成功事件
     */
    public void sendPaymentSuccessEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "PAYMENT_SUCCESS", "payment-success");
    }

    /**
     * 发送支付失败事件
     */
    public void sendPaymentFailedEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "PAYMENT_FAILED", "payment-failed");
    }

    /**
     * 发送支付超时事件
     */
    public void sendPaymentTimeoutEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "PAYMENT_TIMEOUT", "payment-timeout");
    }

    /**
     * 发送退款申请事件
     */
    public void sendRefundAppliedEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "REFUND_APPLIED", "refund-applied");
    }

    /**
     * 发送退款成功事件
     */
    public void sendRefundSuccessEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "REFUND_SUCCESS", "refund-success");
    }

    /**
     * 发送退款失败事件
     */
    public void sendRefundFailedEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "REFUND_FAILED", "refund-failed");
    }

    /**
     * 发送支付状态变更事件
     */
    public void sendPaymentStatusChangedEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "PAYMENT_STATUS_CHANGED", "payment-status-changed");
    }

    /**
     * 发送支付回调事件
     */
    public void sendPaymentCallbackEvent(PaymentChangeEvent event) {
        sendPaymentEvent(event, "PAYMENT_CALLBACK", "payment-callback");
    }

    // ================================ 新增专用事件方法 ================================

    /**
     * 发送支付成功事件（新版本）
     * 通知订单服务更新订单状态
     *
     * @param event 支付成功事件
     */
    public void sendPaymentSuccessEvent(PaymentSuccessEvent event) {
        try {
            // 构建消息头
            Map<String, Object> headers = createMessageHeaders(
                    MessageTopicConstants.PaymentTags.PAYMENT_SUCCESS,
                    "PAYMENT_SUCCESS_" + event.getPaymentId(),
                    "PAYMENT_SUCCESS"
            );

            // 使用GenericMessage构建消息
            Message<PaymentSuccessEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            log.info("📨 准备发送支付成功事件 - 支付ID: {}, 订单ID: {}, 追踪ID: {}",
                    event.getPaymentId(), event.getOrderId(), traceId);

            // 发送消息
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.PAYMENT_SUCCESS_PRODUCER, message);

            if (sent) {
                log.info("✅ 支付成功事件发送成功 - 支付ID: {}, 订单ID: {}, 金额: {}, 追踪ID: {}",
                        event.getPaymentId(), event.getOrderId(), event.getPaymentAmount(), traceId);
            } else {
                log.error("❌ 支付成功事件发送失败 - 支付ID: {}, 订单ID: {}, 追踪ID: {}",
                        event.getPaymentId(), event.getOrderId(), traceId);
                throw new MessageSendException("支付成功事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送支付成功事件时发生异常 - 支付ID: {}, 错误: {}",
                    event.getPaymentId(), e.getMessage(), e);
            throw new MessageSendException("发送支付成功事件异常", e);
        }
    }

    /**
     * 发送退款创建事件
     * 通知相关服务处理退款创建
     *
     * @param event 退款创建事件
     */
    public void sendRefundCreateEvent(RefundCreateEvent event) {
        try {
            // 构建消息头
            Map<String, Object> headers = createMessageHeaders(
                    MessageTopicConstants.PaymentTags.REFUND_APPLIED,
                    "REFUND_CREATE_" + event.getRefundId(),
                    "REFUND_CREATE"
            );

            // 使用GenericMessage构建消息
            Message<RefundCreateEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            log.info("📨 准备发送退款创建事件 - 退款ID: {}, 原支付ID: {}, 订单ID: {}, 退款金额: {}, 追踪ID: {}",
                    event.getRefundId(), event.getOriginalPaymentId(), event.getOrderId(),
                    event.getRefundAmount(), traceId);

            // 发送消息
            streamBridge.send(PAYMENT_BINDING_NAME, message);

            log.info("✅ 退款创建事件发送成功 - 退款ID: {}, 追踪ID: {}", event.getRefundId(), traceId);

        } catch (Exception e) {
            log.error("❌ 发送退款创建事件异常 - 退款ID: {}, 错误: {}",
                    event.getRefundId(), e.getMessage(), e);
            throw new MessageSendException("发送退款创建事件异常", e);
        }
    }

    /**
     * 统一发送支付事件的内部方法
     * 按照官方示例标准实现，使用GenericMessage和MessageConst
     */
    private void sendPaymentEvent(PaymentChangeEvent event, String changeType, String tag) {
        try {
            // 按照官方示例构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "PAYMENT_" + event.getPaymentId());
            headers.put("eventType", changeType);
            headers.put("traceId", generateTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            // 使用GenericMessage构建消息（官方标准方式）
            Message<PaymentChangeEvent> message = new GenericMessage<>(event, headers);
            String traceId = (String) headers.get("traceId");

            // 记录发送日志
            MessageUtils.logMessageSend("payment-events", event, traceId);

            // 发送消息
            boolean sent = streamBridge.send(PAYMENT_BINDING_NAME, message);

            if (sent) {
                log.info("✅ 支付事件发送成功 - 事件类型: {}, 支付ID: {}, 用户ID: {}, 订单ID: {}, Tag: {}, TraceId: {}",
                        changeType, event.getPaymentId(), event.getUserId(), event.getOrderId(), tag, traceId);
            } else {
                log.error("❌ 支付事件发送失败 - 事件类型: {}, 支付ID: {}, TraceId: {}",
                        changeType, event.getPaymentId(), traceId);
                throw new MessageSendException("支付事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送支付事件时发生异常 - 事件类型: {}, 支付ID: {}, 错误: {}",
                    changeType, event.getPaymentId(), e.getMessage(), e);
            throw new MessageSendException("发送支付事件异常", e);
        }
    }

    /**
     * 创建通用消息头
     */
    private Map<String, Object> createMessageHeaders(String tag, String key, String eventType) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_TAGS, tag);
        headers.put(MessageConst.PROPERTY_KEYS, key);
        headers.put("eventType", eventType);
        headers.put("traceId", generateTraceId());
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("serviceName", "payment-service");
        return headers;
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return com.cloud.common.utils.StringUtils.generateTraceId();
    }
}
