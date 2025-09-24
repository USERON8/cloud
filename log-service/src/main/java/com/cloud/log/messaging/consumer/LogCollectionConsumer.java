package com.cloud.log.messaging.consumer;

import com.cloud.common.domain.event.LogCollectionEvent;
import com.cloud.common.exception.MessageConsumeException;
import com.cloud.log.service.LogCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

/**
 * 日志收集消费者
 * 负责消费各微服务发送的日志消息
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class LogCollectionConsumer {

    private final LogCollectionService logCollectionService;

    /**
     * 日志收集消费者函数
     * 对应绑定名称: logConsumer-in-0
     */
    @Bean
    public Consumer<Message<LogCollectionEvent>> logConsumer() {
        return message -> {
            try {
                LogCollectionEvent event = message.getPayload();
                String traceId = event.getTraceId();
                String serviceName = event.getServiceName();
                String operation = event.getOperation();

                log.info("📥 接收到日志收集消息 - 服务: {}, 操作: {}, 追踪ID: {}",
                        serviceName, operation, traceId);

                // 1. 幂等性检查
                if (logCollectionService.isLogProcessed(traceId)) {
                    log.warn("⚠️ 日志已处理，跳过 - 追踪ID: {}", traceId);
                    return;
                }

                // 2. 数据脱敏处理
                LogCollectionEvent sanitizedEvent = sanitizeLogEvent(event);

                // 3. 保存日志到Elasticsearch
                boolean saved = logCollectionService.saveLogEvent(sanitizedEvent);

                if (saved) {
                    log.info("✅ 日志保存成功 - 服务: {}, 操作: {}, 追踪ID: {}",
                            serviceName, operation, traceId);

                    // 4. 标记已处理
                    logCollectionService.markLogProcessed(traceId);
                } else {
                    log.error("❌ 日志保存失败 - 服务: {}, 操作: {}, 追踪ID: {}",
                            serviceName, operation, traceId);
                    throw new MessageConsumeException("日志保存失败", null);
                }

            } catch (Exception e) {
                log.error("❌ 处理日志收集消息时发生异常: {}", e.getMessage(), e);
                throw new MessageConsumeException("处理日志收集消息异常", e);
            }
        };
    }

    /**
     * 数据脱敏处理
     * 对敏感信息进行脱敏处理
     */
    private LogCollectionEvent sanitizeLogEvent(LogCollectionEvent event) {
        LogCollectionEvent sanitized = LogCollectionEvent.builder()
                .logId(event.getLogId())
                .serviceName(event.getServiceName())
                .logLevel(event.getLogLevel())
                .logType(event.getLogType())
                .module(event.getModule())
                .operation(event.getOperation())
                .description(event.getDescription())
                .userId(event.getUserId())
                .userName(sanitizeUserName(event.getUserName()))
                .userType(event.getUserType())
                .businessId(event.getBusinessId())
                .businessType(event.getBusinessType())
                .clientIp(sanitizeIp(event.getClientIp()))
                .userAgent(event.getUserAgent())
                .deviceId(event.getDeviceId())
                .requestUri(event.getRequestUri())
                .requestMethod(event.getRequestMethod())
                .requestParams(sanitizeRequestParams(event.getRequestParams()))
                .responseStatus(event.getResponseStatus())
                .responseTime(event.getResponseTime())
                .exceptionMessage(event.getExceptionMessage())
                .exceptionStack(sanitizeStackTrace(event.getExceptionStack()))
                .extendedFields(event.getExtendedFields())
                .content(sanitizeContent(event.getContent()))
                .createTime(event.getCreateTime())
                .traceId(event.getTraceId())
                .sessionId(event.getSessionId())
                .requestId(event.getRequestId())
                .result(event.getResult())
                .duration(event.getDuration())
                .beforeData(sanitizeData(event.getBeforeData()))
                .afterData(sanitizeData(event.getAfterData()))
                .remark(event.getRemark())
                .build();

        return sanitized;
    }

    /**
     * 脱敏用户名
     */
    private String sanitizeUserName(String userName) {
        if (userName == null || userName.length() <= 2) {
            return userName;
        }
        return userName.charAt(0) + "***" + userName.charAt(userName.length() - 1);
    }

    /**
     * 脱敏IP地址
     */
    private String sanitizeIp(String ip) {
        if (ip == null || !ip.contains(".")) {
            return ip;
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***." + parts[3];
        }
        return ip;
    }

    /**
     * 脱敏请求参数
     */
    private String sanitizeRequestParams(String params) {
        if (params == null) {
            return null;
        }
        // 简单的脱敏处理，实际项目中可以更复杂
        return params.replaceAll("(password|pwd|token|secret)=[^&]*", "$1=***")
                .replaceAll("(phone|mobile)=\\d{11}", "$1=***")
                .replaceAll("(email)=[^&]*@[^&]*", "$1=***@***");
    }

    /**
     * 脱敏异常堆栈
     */
    private String sanitizeStackTrace(String stackTrace) {
        return com.cloud.common.utils.StringUtils.truncate(stackTrace, 500);
    }

    /**
     * 脱敏日志内容
     */
    private String sanitizeContent(String content) {
        return com.cloud.common.utils.StringUtils.sanitizeContent(content);
    }

    /**
     * 脱敏数据
     */
    private String sanitizeData(String data) {
        if (data == null) {
            return null;
        }
        // 对JSON数据进行脱敏
        return data.replaceAll("\"(password|pwd|token|secret)\"\\s*:\\s*\"[^\"]*\"", "\"$1\":\"***\"")
                .replaceAll("\"(phone|mobile)\"\\s*:\\s*\"\\d{11}\"", "\"$1\":\"***\"")
                .replaceAll("\"(email)\"\\s*:\\s*\"[^\"]*@[^\"]*\"", "\"$1\":\"***@***.com\"");
    }
}
