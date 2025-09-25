package com.cloud.payment.messaging.producer;

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
 * 支付服务日志收集生产者
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
     * 发送支付业务日志
     */
    public void sendPaymentLog(LogCollectionEvent event) {
        sendLogEvent(event, MessageTopicConstants.LogTags.PAYMENT_LOG);
    }

    /**
     * 发送支付操作日志
     */
    public void sendPaymentOperationLog(Long paymentId, Long orderId, String operation,
                                        BigDecimal amount, String paymentMethod, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("payment-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("PAYMENT_MANAGEMENT")
                .operation(operation)
                .description("支付" + operation + "操作")
                .businessId(paymentId.toString())
                .businessType("PAYMENT")
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator + ", 订单ID: " + orderId +
                        ", 金额: " + amount + ", 支付方式: " + paymentMethod)
                .build();

        sendPaymentLog(event);
    }

    /**
     * 发送支付异常日志
     */
    public void sendPaymentErrorLog(Long paymentId, String operation,
                                    String exceptionMessage, String exceptionStack) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("payment-service")
                .logLevel("ERROR")
                .logType("SYSTEM")
                .module("PAYMENT_SERVICE")
                .operation(operation)
                .description("支付服务异常")
                .businessId(paymentId != null ? paymentId.toString() : null)
                .businessType("PAYMENT")
                .exceptionMessage(exceptionMessage)
                .exceptionStack(exceptionStack)
                .result("FAILURE")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        sendPaymentLog(event);
    }

    /**
     * 统一发送日志事件的内部方法
     */
    private void sendLogEvent(LogCollectionEvent event, String tag) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "PAYMENT_LOG_" + event.getLogId());
            headers.put("eventType", "LOG_COLLECTION");
            headers.put("serviceName", "payment-service");
            headers.put("logLevel", event.getLogLevel());
            headers.put("traceId", event.getTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            Message<LogCollectionEvent> message = new GenericMessage<>(event, headers);
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.LOG_PRODUCER, message);

            if (sent) {
                log.debug("✅ 支付日志发送成功 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), event.getTraceId());
            } else {
                log.error("❌ 支付日志发送失败 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), event.getTraceId());
                throw new MessageSendException("支付日志发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送支付日志时发生异常 - 操作: {}, 错误: {}",
                    event.getOperation(), e.getMessage(), e);
            throw new MessageSendException("发送支付日志异常", e);
        }
    }

    private String generateLogId() {
        return "PAYMENT_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
