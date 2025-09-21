package com.cloud.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 消息工具类
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
public class MessageUtils {

    /**
     * 构建包含安全信息的消息
     */
    public static <T> Message<T> buildSecureMessage(T payload, String eventType) {
        Map<String, Object> headers = new HashMap<>();

        // 添加事件类型
        headers.put("eventType", eventType);

        // 添加时间戳
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("operateTime", LocalDateTime.now().toString());

        // 生成追踪ID
        headers.put("traceId", generateTraceId());

        // 添加用户安全信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            headers.put("operator", auth.getName());
            headers.put("authorities", auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
        } else {
            headers.put("operator", "SYSTEM");
        }

        return MessageBuilder.createMessage(payload,
                new org.springframework.messaging.MessageHeaders(headers));
    }

    /**
     * 构建用户变更消息
     */
    public static <T> Message<T> buildUserChangeMessage(T payload, String changeType, String tag) {
        Map<String, Object> headers = new HashMap<>();

        // 消息头设置
        headers.put("eventType", "USER_CHANGE");
        headers.put("changeType", changeType);
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("traceId", generateTraceId());

        // 设置RocketMQ特定的Tag
        headers.put("rocketmq_TAGS", tag);

        // 用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            headers.put("operator", auth.getName());
        }

        return MessageBuilder.createMessage(payload,
                new org.springframework.messaging.MessageHeaders(headers));
    }

    /**
     * 生成追踪ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从消息中提取追踪ID
     */
    public static String getTraceId(Message<?> message) {
        return message.getHeaders().get("traceId", String.class);
    }

    /**
     * 从消息中提取操作者
     */
    public static String getOperator(Message<?> message) {
        return message.getHeaders().get("operator", String.class);
    }

    /**
     * 从消息中提取事件类型
     */
    public static String getEventType(Message<?> message) {
        return message.getHeaders().get("eventType", String.class);
    }

    /**
     * 检查消息是否重复（幂等性检查）
     */
    public static boolean isDuplicateMessage(String traceId, String cacheKey) {
        // 这里可以结合Redis实现幂等性检查
        // 暂时返回false，表示不重复
        return false;
    }

    /**
     * 日志记录消息发送
     */
    public static void logMessageSend(String topic, Object payload, String traceId) {
        log.info("📨 发送消息 - Topic: {}, TraceId: {}, Payload: {}",
                topic, traceId, payload.getClass().getSimpleName());
    }

    /**
     * 日志记录消息接收
     */
    public static void logMessageReceive(String topic, Object payload, String traceId) {
        log.info("📥 接收消息 - Topic: {}, TraceId: {}, Payload: {}",
                topic, traceId, payload.getClass().getSimpleName());
    }

    /**
     * 日志记录消息处理成功
     */
    public static void logMessageProcessSuccess(Object payload, String traceId) {
        log.info("✅ 消息处理成功 - TraceId: {}, Payload: {}",
                traceId, payload.getClass().getSimpleName());
    }

    /**
     * 日志记录消息处理失败
     */
    public static void logMessageProcessError(Object payload, String traceId, String error) {
        log.error("❌ 消息处理失败 - TraceId: {}, Payload: {}, Error: {}",
                traceId, payload.getClass().getSimpleName(), error);
    }
}
