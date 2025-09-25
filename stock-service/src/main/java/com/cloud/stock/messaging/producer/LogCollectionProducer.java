package com.cloud.stock.messaging.producer;

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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 库存服务日志收集生产者
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
     * 发送库存业务日志
     */
    public void sendStockLog(LogCollectionEvent event) {
        sendLogEvent(event, MessageTopicConstants.LogTags.STOCK_LOG);
    }

    /**
     * 发送库存操作日志
     */
    public void sendStockOperationLog(Long productId, String productName, String operation,
                                      Integer beforeQuantity, Integer afterQuantity, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("stock-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("STOCK_MANAGEMENT")
                .operation(operation)
                .description("库存" + operation + "操作")
                .businessId(productId.toString())
                .businessType("STOCK")
                .beforeData(beforeQuantity != null ? beforeQuantity.toString() : null)
                .afterData(afterQuantity != null ? afterQuantity.toString() : null)
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator + ", 商品: " + productName)
                .build();

        sendStockLog(event);
    }

    /**
     * 发送库存异常日志
     */
    public void sendStockErrorLog(Long productId, String operation,
                                  String exceptionMessage, String exceptionStack) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("stock-service")
                .logLevel("ERROR")
                .logType("SYSTEM")
                .module("STOCK_SERVICE")
                .operation(operation)
                .description("库存服务异常")
                .businessId(productId != null ? productId.toString() : null)
                .businessType("STOCK")
                .exceptionMessage(exceptionMessage)
                .exceptionStack(exceptionStack)
                .result("FAILURE")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        sendStockLog(event);
    }

    /**
     * 统一发送日志事件的内部方法
     */
    private void sendLogEvent(LogCollectionEvent event, String tag) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "STOCK_LOG_" + event.getLogId());
            headers.put("eventType", "LOG_COLLECTION");
            headers.put("serviceName", "stock-service");
            headers.put("logLevel", event.getLogLevel());
            headers.put("traceId", event.getTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            Message<LogCollectionEvent> message = new GenericMessage<>(event, headers);
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.LOG_PRODUCER, message);

            if (sent) {
                log.debug("✅ 库存日志发送成功 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), event.getTraceId());
            } else {
                log.error("❌ 库存日志发送失败 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), event.getTraceId());
                throw new MessageSendException("库存日志发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送库存日志时发生异常 - 操作: {}, 错误: {}",
                    event.getOperation(), e.getMessage(), e);
            throw new MessageSendException("发送库存日志异常", e);
        }
    }

    private String generateLogId() {
        return "STOCK_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
