package com.cloud.order.messaging.producer;

import com.cloud.common.constant.MessageTopicConstants;
import com.cloud.common.domain.event.LogCollectionEvent;
import com.cloud.common.exception.MessageSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 订单服务日志收集生产者
 * 负责发送业务日志到日志服务
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class LogCollectionProducer {

    private final StreamBridge streamBridge;

    /**
     * 发送订单业务日志
     */
    public void sendOrderLog(LogCollectionEvent event) {
        sendLogEvent(event, MessageTopicConstants.LogTags.ORDER_LOG);
    }

    /**
     * 发送订单操作日志
     */
    public void sendOrderOperationLog(Long orderId, String orderNo, Long userId, String operation,
                                      BigDecimal amount, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("order-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("ORDER_MANAGEMENT")
                .operation(operation)
                .description("订单" + operation + "操作")
                .userId(userId)
                .businessId(orderId.toString())
                .businessType("ORDER")
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator + ", 订单号: " + orderNo +
                        ", 金额: " + amount)
                .build();

        sendOrderLog(event);
    }

    /**
     * 发送订单异常日志
     */
    public void sendOrderErrorLog(Long orderId, String operation,
                                  String exceptionMessage, String exceptionStack) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("order-service")
                .logLevel("ERROR")
                .logType("SYSTEM")
                .module("ORDER_SERVICE")
                .operation(operation)
                .description("订单服务异常")
                .businessId(orderId != null ? orderId.toString() : null)
                .businessType("ORDER")
                .exceptionMessage(exceptionMessage)
                .exceptionStack(exceptionStack)
                .result("FAILURE")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        sendOrderLog(event);
    }

    /**
     * 统一发送日志事件的内部方法
     */
    private void sendLogEvent(LogCollectionEvent event, String tag) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "ORDER_LOG_" + event.getLogId());
            headers.put("eventType", "LOG_COLLECTION");
            headers.put("serviceName", "order-service");
            headers.put("logLevel", event.getLogLevel());
            headers.put("traceId", event.getTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            Message<LogCollectionEvent> message = new GenericMessage<>(event, headers);
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.LOG_PRODUCER, message);

            if (sent) {
                log.debug("✅ 订单日志发送成功 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), event.getTraceId());
            } else {
                log.error("❌ 订单日志发送失败 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), event.getTraceId());
                throw new MessageSendException("订单日志发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送订单日志时发生异常 - 操作: {}, 错误: {}",
                    event.getOperation(), e.getMessage(), e);
            throw new MessageSendException("发送订单日志异常", e);
        }
    }

    private String generateLogId() {
        return "ORDER_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
