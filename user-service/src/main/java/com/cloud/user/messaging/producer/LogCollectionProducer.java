package com.cloud.user.messaging.producer;

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
 * 用户服务日志收集生产者
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
     * 发送用户业务日志
     *
     * @param event 日志收集事件
     */
    public void sendUserLog(LogCollectionEvent event) {
        sendLogEvent(event, MessageTopicConstants.LogTags.USER_LOG);
    }

    /**
     * 发送用户登录日志
     *
     * @param userId     用户ID
     * @param userName   用户名
     * @param operation  操作类型
     * @param clientIp   客户端IP
     * @param userAgent  用户代理
     * @param result     操作结果
     */
    public void sendUserLoginLog(Long userId, String userName, String operation, 
                                String clientIp, String userAgent, String result) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("user-service")
                .logLevel("INFO")
                .logType("SECURITY")
                .module("USER_AUTH")
                .operation(operation)
                .description("用户" + operation + "操作")
                .userId(userId)
                .userName(userName)
                .userType("CUSTOMER")
                .clientIp(clientIp)
                .userAgent(userAgent)
                .result(result)
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        sendUserLog(event);
    }

    /**
     * 发送用户信息变更日志
     *
     * @param userId      用户ID
     * @param userName    用户名
     * @param operation   操作类型
     * @param beforeData  变更前数据
     * @param afterData   变更后数据
     * @param operator    操作人
     */
    public void sendUserChangeLog(Long userId, String userName, String operation,
                                 String beforeData, String afterData, String operator) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("user-service")
                .logLevel("INFO")
                .logType("BUSINESS")
                .module("USER_MANAGEMENT")
                .operation(operation)
                .description("用户信息" + operation)
                .userId(userId)
                .userName(userName)
                .userType("CUSTOMER")
                .businessId(userId.toString())
                .businessType("USER")
                .beforeData(beforeData)
                .afterData(afterData)
                .result("SUCCESS")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .remark("操作人: " + operator)
                .build();

        sendUserLog(event);
    }

    /**
     * 发送用户异常日志
     *
     * @param userId          用户ID
     * @param operation       操作类型
     * @param exceptionMessage 异常信息
     * @param exceptionStack  异常堆栈
     */
    public void sendUserErrorLog(Long userId, String operation, 
                                String exceptionMessage, String exceptionStack) {
        LogCollectionEvent event = LogCollectionEvent.builder()
                .logId(generateLogId())
                .serviceName("user-service")
                .logLevel("ERROR")
                .logType("SYSTEM")
                .module("USER_SERVICE")
                .operation(operation)
                .description("用户服务异常")
                .userId(userId)
                .businessId(userId != null ? userId.toString() : null)
                .businessType("USER")
                .exceptionMessage(exceptionMessage)
                .exceptionStack(exceptionStack)
                .result("FAILURE")
                .createTime(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        sendUserLog(event);
    }

    /**
     * 统一发送日志事件的内部方法
     */
    private void sendLogEvent(LogCollectionEvent event, String tag) {
        try {
            // 构建消息头
            Map<String, Object> headers = new HashMap<>();
            headers.put(MessageConst.PROPERTY_TAGS, tag);
            headers.put(MessageConst.PROPERTY_KEYS, "USER_LOG_" + event.getLogId());
            headers.put("eventType", "LOG_COLLECTION");
            headers.put("serviceName", "user-service");
            headers.put("logLevel", event.getLogLevel());
            headers.put("traceId", event.getTraceId());
            headers.put("timestamp", System.currentTimeMillis());

            // 使用GenericMessage构建消息
            Message<LogCollectionEvent> message = new GenericMessage<>(event, headers);
            String traceId = event.getTraceId();

            // 发送消息
            boolean sent = streamBridge.send(MessageTopicConstants.ProducerBindings.LOG_PRODUCER, message);

            if (sent) {
                log.debug("✅ 用户日志发送成功 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), traceId);
            } else {
                log.error("❌ 用户日志发送失败 - 日志类型: {}, 操作: {}, 追踪ID: {}",
                        event.getLogType(), event.getOperation(), traceId);
                throw new MessageSendException("用户日志发送失败");
            }

        } catch (Exception e) {
            log.error("❌ 发送用户日志时发生异常 - 操作: {}, 错误: {}",
                    event.getOperation(), e.getMessage(), e);
            throw new MessageSendException("发送用户日志异常", e);
        }
    }

    /**
     * 生成日志ID
     */
    private String generateLogId() {
        return "USER_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
