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
import java.util.stream.Collectors;







@Slf4j
public class MessageUtils {

    


    public static <T> Message<T> buildSecureMessage(T payload, String eventType) {
        Map<String, Object> headers = new HashMap<>();

        
        headers.put("eventType", eventType);

        
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("operateTime", LocalDateTime.now().toString());

        
        headers.put("traceId", generateTraceId());

        
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

    


    public static <T> Message<T> buildUserChangeMessage(T payload, String changeType, String tag) {
        Map<String, Object> headers = new HashMap<>();

        
        headers.put("eventType", "USER_CHANGE");
        headers.put("changeType", changeType);
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("traceId", generateTraceId());

        
        headers.put("rocketmq_TAGS", tag);

        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            headers.put("operator", auth.getName());
        }

        return MessageBuilder.createMessage(payload,
                new org.springframework.messaging.MessageHeaders(headers));
    }

    




    @Deprecated
    public static String generateTraceId() {
        return StringUtils.generateTraceId();
    }

    


    public static String getTraceId(Message<?> message) {
        return message.getHeaders().get("traceId", String.class);
    }

    


    public static String getOperator(Message<?> message) {
        return message.getHeaders().get("operator", String.class);
    }

    


    public static String getEventType(Message<?> message) {
        return message.getHeaders().get("eventType", String.class);
    }

    


    public static boolean isDuplicateMessage(String traceId, String cacheKey) {
        
        
        return false;
    }

    


    public static void logMessageSend(String topic, Object payload, String traceId) {
        

    }

    


    public static void logMessageReceive(String topic, Object payload, String traceId) {
        

    }

    


    public static void logMessageProcessSuccess(Object payload, String traceId) {
        

    }

    


    public static void logMessageProcessError(Object payload, String traceId, String error) {
        log.error("? - TraceId: {}, Payload: {}, Error: {}",
                traceId, payload.getClass().getSimpleName(), error);
    }
}
