package com.cloud.order.messaging.producer;

import com.cloud.common.constant.MessageTopicConstants;
import com.cloud.common.domain.event.order.OrderChangeEvent;
import com.cloud.common.domain.event.order.OrderCompletedEvent;
import com.cloud.common.domain.event.order.OrderCreatedEvent;
import com.cloud.common.domain.event.payment.PaymentRecordCreateEvent;
import com.cloud.common.domain.event.stock.StockConfirmEvent;
import com.cloud.common.domain.event.stock.StockReserveEvent;
import com.cloud.common.domain.event.stock.StockRollbackEvent;
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

    // ================================ 新增专用事件方法 ================================

    /**
     * 发送订单创建事件（新版本）
     * 通知支付服务创建支付记录，通知库存服务冻结库存
     *
     * @param event 订单创建事件
     */
    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            // 构建消息头
            Map<String, Object> headers = createMessageHeaders(
                    MessageTopicConstants.OrderTags.ORDER_CREATED,
                    "ORDER_CREATED_" + event.getOrderId(),
                    "ORDER_CREATED"
            );

            // 使用GenericMessage构建消息
            Message<OrderCreatedEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            log.info("📨 准备发送订单创建事件 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                    event.getOrderId(), event.getOrderNo(), traceId);

            // 发送消息
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.ORDER_CREATED_PRODUCER, message);

            if (sent) {
                log.info("✅ 订单创建事件发送成功 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
            } else {
                log.error("❌ 订单创建事件发送失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
                throw new MessageSendException("订单创建事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送订单创建事件时发生异常 - 订单ID: {}, 错误: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw new MessageSendException("发送订单创建事件异常", e);
        }
    }

    /**
     * 发送订单完成事件（新版本）
     * 通知库存服务解冻并扣减库存
     *
     * @param event 订单完成事件
     */
    public void sendOrderCompletedEvent(OrderCompletedEvent event) {
        try {
            // 构建消息头
            Map<String, Object> headers = createMessageHeaders(
                    MessageTopicConstants.OrderTags.ORDER_COMPLETED,
                    "ORDER_COMPLETED_" + event.getOrderId(),
                    "ORDER_COMPLETED"
            );

            // 使用GenericMessage构建消息
            Message<OrderCompletedEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            log.info("📨 准备发送订单完成事件 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                    event.getOrderId(), event.getOrderNo(), traceId);

            // 发送消息
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.ORDER_COMPLETED_PRODUCER, message);

            if (sent) {
                log.info("✅ 订单完成事件发送成功 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
            } else {
                log.error("❌ 订单完成事件发送失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
                throw new MessageSendException("订单完成事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送订单完成事件时发生异常 - 订单ID: {}, 错误: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw new MessageSendException("发送订单完成事件异常", e);
        }
    }

    /**
     * 发送库存预扣减事件
     * 通知库存服务预扣减库存
     *
     * @param event 库存预扣减事件
     */
    public void sendStockReserveEvent(StockReserveEvent event) {
        try {
            // 构庺消息头
            Map<String, Object> headers = createMessageHeaders(
                    "stock-reserve",
                    "STOCK_RESERVE_" + event.getOrderId(),
                    "STOCK_RESERVE"
            );

            // 使用GenericMessage构庺消息
            Message<StockReserveEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            log.info("📨 准备发送库存预扣减事件 - 订单ID: {}, 订单号: {}, 商品数量: {}, 追踪ID: {}",
                    event.getOrderId(), event.getOrderNo(), event.getReserveItems().size(), traceId);

            // 发送消息到库存服务
            boolean sent = streamBridge.send("stockReserve-out-0", message);

            if (sent) {
                log.info("✅ 库存预扣减事件发送成功 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
            } else {
                log.error("❌ 库存预扣减事件发送失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
                throw new MessageSendException("库存预扣减事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送库存预扣减事件时发生异常 - 订单ID: {}, 错误: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw new MessageSendException("发送库存预扣减事件异常", e);
        }
    }

    /**
     * 发送库存确认扣减事件
     * 通知库存服务确认扣减库存
     *
     * @param event 库存确认扣减事件
     */
    public void sendStockConfirmEvent(StockConfirmEvent event) {
        try {
            // 构庺消息头
            Map<String, Object> headers = createMessageHeaders(
                    "stock-confirm",
                    "STOCK_CONFIRM_" + event.getOrderId(),
                    "STOCK_CONFIRM"
            );

            // 使用GenericMessage构庺消息
            Message<StockConfirmEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            log.info("📨 准备发送库存确认扣减事件 - 订单ID: {}, 订单号: {}, 商品数量: {}, 追踪ID: {}",
                    event.getOrderId(), event.getOrderNo(), event.getConfirmItems().size(), traceId);

            // 发送消息到库存服务
            boolean sent = streamBridge.send("stockConfirm-out-0", message);

            if (sent) {
                log.info("✅ 库存确认扣减事件发送成功 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
            } else {
                log.error("❌ 库存确认扣减事件发送失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
                throw new MessageSendException("库存确认扣减事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送库存确认扣减事件时发生异常 - 订单ID: {}, 错误: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw new MessageSendException("发送库存确认扣减事件异常", e);
        }
    }

    /**
     * 发送库存回滚事件
     * 通知库存服务回滚库存
     *
     * @param event 库存回滚事件
     */
    public void sendStockRollbackEvent(StockRollbackEvent event) {
        try {
            // 构庺消息头
            Map<String, Object> headers = createMessageHeaders(
                    "stock-rollback",
                    "STOCK_ROLLBACK_" + event.getOrderId(),
                    "STOCK_ROLLBACK"
            );

            // 使用GenericMessage构庺消息
            Message<StockRollbackEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            log.info("📨 准备发送库存回滚事件 - 订单ID: {}, 订单号: {}, 回滚类型: {}, 商品数量: {}, 追踪ID: {}",
                    event.getOrderId(), event.getOrderNo(), event.getRollbackType(),
                    event.getRollbackItems().size(), traceId);

            // 发送消息到库存服务
            boolean sent = streamBridge.send("stockRollback-out-0", message);

            if (sent) {
                log.info("✅ 库存回滚事件发送成功 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
            } else {
                log.error("❌ 库存回滚事件发送失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
                throw new MessageSendException("库存回滚事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送库存回滚事件时发生异常 - 订单ID: {}, 错误: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw new MessageSendException("发送库存回滚事件异常", e);
        }
    }

    /**
     * 发送支付记录创建事件
     * 通知支付服务创建支付记录
     *
     * @param event 支付记录创建事件
     */
    public void sendPaymentRecordCreateEvent(PaymentRecordCreateEvent event) {
        try {
            // 构庺消息头
            Map<String, Object> headers = createMessageHeaders(
                    "payment-record-create",
                    "PAYMENT_RECORD_CREATE_" + event.getOrderId(),
                    "PAYMENT_RECORD_CREATE"
            );

            // 使用GenericMessage构庺消息
            Message<PaymentRecordCreateEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            log.info("📨 准备发送支付记录创建事件 - 订单ID: {}, 订单号: {}, 支付金额: {}, 追踪ID: {}",
                    event.getOrderId(), event.getOrderNo(), event.getPaymentAmount(), traceId);

            // 发送消息到支付服务
            boolean sent = streamBridge.send("paymentRecordCreate-out-0", message);

            if (sent) {
                log.info("✅ 支付记录创建事件发送成功 - 订单ID: {}, 订单号: {}, 支付金额: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), event.getPaymentAmount(), traceId);
            } else {
                log.error("❌ 支付记录创建事件发送失败 - 订单ID: {}, 订单号: {}, 追踪ID: {}",
                        event.getOrderId(), event.getOrderNo(), traceId);
                throw new MessageSendException("支付记录创建事件发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送支付记录创建事件时发生异常 - 订单ID: {}, 错误: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw new MessageSendException("发送支付记录创建事件异常", e);
        }
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
     * 创建通用消息头
     */
    private Map<String, Object> createMessageHeaders(String tag, String key, String eventType) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_TAGS, tag);
        headers.put(MessageConst.PROPERTY_KEYS, key);
        headers.put("eventType", eventType);
        headers.put("traceId", generateTraceId());
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("serviceName", "order-service");
        return headers;
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return com.cloud.common.utils.StringUtils.generateTraceId();
    }
}
