package com.cloud.user.event;

import com.cloud.common.domain.event.user.UserChangeEvent;
import com.cloud.user.module.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 用户事件生产者
 * 负责生产用户相关的事件消息到消息队列
 *
 * @author cloud
 * @since 2025-09-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final StreamBridge streamBridge;
    private static final String USER_BINDING_NAME = "user-producer-out-0";

    /**
     * 事件类型枚举
     */
    public enum EventType {
        CREATED("用户创建", "USER_CREATED"),
        UPDATED("用户更新", "USER_UPDATED"),
        DELETED("用户删除", "USER_DELETED"),
        STATUS_CHANGED("状态变更", "STATUS_CHANGED"),
        LOGIN("用户登录", "USER_LOGIN"),
        LOGOUT("用户登出", "USER_LOGOUT"),
        PASSWORD_CHANGED("密码变更", "PASSWORD_CHANGED"),
        OAUTH_LOGIN("OAuth登录", "OAUTH_LOGIN");

        private final String description;
        private final String tag;

        EventType(String description, String tag) {
            this.description = description;
            this.tag = tag;
        }

        public String getDescription() {
            return description;
        }

        public String getTag() {
            return tag;
        }
    }

    /**
     * 生产用户创建事件
     */
    public void produceCreated(User user) {
        UserChangeEvent event = buildEvent(user, EventType.CREATED);
        sendEvent(event, EventType.CREATED);
    }

    /**
     * 生产用户更新事件
     */
    public void produceUpdated(User user) {
        UserChangeEvent event = buildEvent(user, EventType.UPDATED);
        sendEvent(event, EventType.UPDATED);
    }

    /**
     * 生产用户删除事件
     */
    public void produceDeleted(User user) {
        UserChangeEvent event = buildEvent(user, EventType.DELETED);
        sendEvent(event, EventType.DELETED);
    }

    /**
     * 生产状态变更事件
     */
    public void produceStatusChanged(User user, Integer oldStatus) {
        UserChangeEvent event = buildEventWithStatus(user, EventType.STATUS_CHANGED, oldStatus, user.getStatus());
        sendEvent(event, EventType.STATUS_CHANGED);
    }

    /**
     * 生产用户登录事件
     */
    public void produceLogin(User user) {
        UserChangeEvent event = buildEvent(user, EventType.LOGIN);
        sendEvent(event, EventType.LOGIN);
    }

    /**
     * 生产用户登出事件
     */
    public void produceLogout(User user) {
        UserChangeEvent event = buildEvent(user, EventType.LOGOUT);
        sendEvent(event, EventType.LOGOUT);
    }

    /**
     * 生产OAuth登录事件
     */
    public void publishOAuthLogin(User user, String provider) {
        UserChangeEvent event = buildEvent(user, EventType.OAUTH_LOGIN);
        if (event.getMetadata() == null) {
            event.setMetadata("{\"provider\":\"" + provider + "\"}");
        }
        sendEvent(event, EventType.OAUTH_LOGIN);
    }

    /**
     * 通用事件发布方法（保留兼容性）
     */
    public void publishEvent(User user, EventType eventType) {
        publishEvent(user, eventType, null);
    }

    /**
     * 通用事件发布方法（保留兼容性 - 带metadata）
     */
    public void publishEvent(User user, EventType eventType, String metadata) {
        UserChangeEvent event = UserChangeEvent.builder()
                .userId(user.getId())
                .eventType(eventType.getTag())
                .timestamp(java.time.LocalDateTime.now())
                .traceId(java.util.UUID.randomUUID().toString().replace("-", ""))
                .metadata(metadata)
                .build();
        sendEvent(event, eventType);
    }

    /**
     * 构建基本事件
     */
    private UserChangeEvent buildEvent(User user, EventType eventType) {
        return UserChangeEvent.builder()
                .userId(user.getId())
                .eventType(eventType.getTag())
                .timestamp(java.time.LocalDateTime.now())
                .traceId(java.util.UUID.randomUUID().toString().replace("-", ""))
                .build();
    }

    /**
     * 构建帧状态的事件
     */
    private UserChangeEvent buildEventWithStatus(User user, EventType eventType, Integer beforeStatus, Integer afterStatus) {
        String metadata = String.format("{\"beforeStatus\":%d,\"afterStatus\":%d}", 
                beforeStatus != null ? beforeStatus : 0, 
                afterStatus != null ? afterStatus : 0);
        return UserChangeEvent.builder()
                .userId(user.getId())
                .eventType(eventType.getTag())
                .timestamp(java.time.LocalDateTime.now())
                .traceId(java.util.UUID.randomUUID().toString().replace("-", ""))
                .metadata(metadata)
                .build();
    }

    /**
     * 发送事件到消息队列
     */
    private void sendEvent(UserChangeEvent event, EventType eventType) {
        try {
            Message<UserChangeEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader("eventType", eventType.getTag())
                    .setHeader("messageKey", "USER_" + event.getUserId())
                    .build();

            boolean sent = streamBridge.send(USER_BINDING_NAME, message);

            if (sent) {
                log.info("✅ 用户事件生产成功 - 事件类型: {}, 用户ID: {}, TraceId: {}",
                        eventType.getDescription(), event.getUserId(), event.getTraceId());
            } else {
                log.error("❌ 用户事件生产失败 - 事件类型: {}, 用户ID: {}",
                        eventType.getDescription(), event.getUserId());
            }
        } catch (Exception e) {
            log.error("❌ 生产用户事件时发生异常 - 事件类型: {}, 用户ID: {}, 错误: {}",
                    eventType.getDescription(), event.getUserId(), e.getMessage(), e);
        }
    }
}

