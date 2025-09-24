package com.cloud.search.messaging.producer;

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

/**
 * 搜索服务日志收集生产者
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
     * 发送搜索业务日志
     */
    public void sendSearchLog(LogCollectionEvent event) {
        sendLogEvent(event, MessageTopicConstants.LogTags.SEARCH_LOG);
    }

    /**
     * 发送搜索操作日志
     */
    public void sendSearchOperationLog(String keyword, Long userId, String operation,
                                       Integer resultCount, Long responseTime) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("search-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("SEARCH_ENGINE")
                .operation(operation)
                .description("搜索" + operation + "操作")
                .userId(userId)
                .businessType("SEARCH")
                .responseTime(responseTime)
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("关键词: " + keyword + ", 结果数量: " + resultCount)
                .build();

        sendSearchLog(event);
    }

    /**
     * 发送搜索异常日志
     */
    public void sendSearchErrorLog(String keyword, String operation,
                                   String exceptionMessage, String exceptionStack) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("search-service")
                .logLevel("ERROR")
                .logType("SYSTEM")
                .module("SEARCH_SERVICE")
                .operation(operation)
                .description("搜索服务异常")
                .businessType("SEARCH")
                .exceptionMessage(exceptionMessage)
                .exceptionStack(exceptionStack)
                .result("FAILURE")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("关键词: " + keyword)
                .build();

        sendSearchLog(event);
    }

    /**
     * 统一发送日志事件的内部方法
     */
    private void sendLogEvent(LogCollectionEvent event, String tag) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "SEARCH_LOG_" + event.getLogId());
            headers.put("eventType", "LOG_COLLECTION");
            headers.put("serviceName", "search-service");
            headers.put("logLevel", event.getLogLevel());
            headers.put("traceId", event.getTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            Message<LogCollectionEvent> message = new GenericMessage<>(event, headers);
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.LOG_PRODUCER, message);

            if (sent) {
                log.debug("✅ 搜索日志发送成功 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), event.getTraceId());
            } else {
                log.error("❌ 搜索日志发送失败 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), event.getTraceId());
                throw new MessageSendException("搜索日志发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送搜索日志时发生异常 - 操作: {}, 错误: {}",
                    event.getOperation(), e.getMessage(), e);
            throw new MessageSendException("发送搜索日志异常", e);
        }
    }

    private String generateLogId() {
        return com.cloud.common.utils.StringUtils.generateLogId("SEARCH");
    }

    private String generateTraceId() {
        return com.cloud.common.utils.StringUtils.generateTraceId();
    }
}
