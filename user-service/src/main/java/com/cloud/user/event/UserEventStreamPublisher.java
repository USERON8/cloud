package com.cloud.user.event;

import com.cloud.common.domain.event.UserChangeEvent;
import com.cloud.user.module.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 用户事件流发布器
 * 采用函数式编程风格，简化冗余代码，支持发送到日志服务
 * 基于Stream和函数式接口实现，支持链式调用和组合操作
 *
 * @author what's up
 * @since 2025-09-20
 * @version 2.0 - 重构为统一命名规范
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class UserEventStreamPublisher {

    private final UserEventStreamProducer eventProducer;

    // 函数式事件类型定义
    public enum EventType {
        CREATED("用户创建", "user-created"),
        UPDATED("用户更新", "user-updated"),
        DELETED("用户删除", "user-deleted"),
        STATUS_CHANGED("状态变更", "user-status-changed"),
        LOGIN("用户登录", "user-login"),
        LOGOUT("用户登出", "user-logout"),
        OAUTH_LOGIN("OAuth登录", "user-oauth-login");

        private final String description;
        private final String tag;

        EventType(String description, String tag) {
            this.description = description;
            this.tag = tag;
        }

        public String getDescription() { return description; }
        public String getTag() { return tag; }
    }

    // 主要的函数式发布方法
    public void publishEvent(User user, EventType eventType) {
        publishEvent(user, eventType, null);
    }

    public void publishEvent(User user, EventType eventType, String metadata) {
        Optional.ofNullable(user)
                .map(buildEventFunction(eventType, metadata))
                .ifPresentOrElse(
                    publishEventConsumer(eventType),
                    () -> log.warn("⚠️ 跳过事件发布 - 用户为空, 事件类型: {}", eventType.getDescription())
                );
    }

    // 简化的特定事件发布方法
    public void publishCreated(User user) {
        publishEvent(user, EventType.CREATED);
    }

    public void publishUpdated(User user) {
        publishEvent(user, EventType.UPDATED);
    }

    public void publishDeleted(User user) {
        publishEvent(user, EventType.DELETED);
    }

    public void publishStatusChanged(User user, Integer oldStatus) {
        String metadata = Optional.ofNullable(oldStatus)
                .map(status -> String.format("{\"oldStatus\":%d,\"newStatus\":%d}", status, user.getStatus()))
                .orElse(null);
        publishEvent(user, EventType.STATUS_CHANGED, metadata);
    }

    public void publishLogin(User user) {
        publishEvent(user, EventType.LOGIN);
    }

    public void publishLogout(User user) {
        publishEvent(user, EventType.LOGOUT);
    }

    public void publishOAuthLogin(User user, String provider) {
        String metadata = String.format("{\"oauthProvider\":\"%s\"}", provider);
        publishEvent(user, EventType.OAUTH_LOGIN, metadata);
    }

    // 函数式构建器
    private Function<User, UserChangeEvent> buildEventFunction(EventType eventType, String metadata) {
        return user -> UserChangeEvent.builder()
                .userId(user.getId())
                .eventType(eventType.name())
                .status(user.getStatus())
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .metadata(metadata)
                .build();
    }

    // 函数式事件发布消费者
    private Consumer<UserChangeEvent> publishEventConsumer(EventType eventType) {
        return event -> {
            try {
                // 发送到日志服务队列
                eventProducer.sendToLogService(event, eventType.getTag());
                log.info("📢 {} 事件已发送到日志服务 - 用户ID: {}, TraceId: {}", 
                        eventType.getDescription(), event.getUserId(), event.getTraceId());
            } catch (Exception e) {
                log.error("❌ {} 事件发送失败 - 用户ID: {}, TraceId: {}, 错误: {}", 
                        eventType.getDescription(), event.getUserId(), event.getTraceId(), e.getMessage(), e);
                // 可以在这里添加重试逻辑或发送到死信队列
            }
        };
    }

    // 追踪ID生成器 - 函数式风格
    private static final Supplier<String> TRACE_ID_GENERATOR = 
            () -> UUID.randomUUID().toString().replace("-", "").substring(0, 16);

    private String generateTraceId() {
        return TRACE_ID_GENERATOR.get();
    }

    // 批量事件发布 - 函数式风格
    public void publishBatchEvents(java.util.List<User> users, EventType eventType) {
        Optional.ofNullable(users)
                .filter(list -> !list.isEmpty())
                .ifPresentOrElse(
                    userList -> {
                        userList.stream()
                                .filter(java.util.Objects::nonNull)
                                .forEach(user -> publishEvent(user, eventType));
                        log.info("📦 批量发布 {} 事件完成 - 数量: {}", eventType.getDescription(), userList.size());
                    },
                    () -> log.warn("⚠️ 跳过批量事件发布 - 用户列表为空或null")
                );
    }

    // 条件发布 - 函数式风格
    public void publishConditionally(User user, EventType eventType, 
                                   java.util.function.Predicate<User> condition) {
        Optional.ofNullable(user)
                .filter(condition)
                .ifPresentOrElse(
                    u -> publishEvent(u, eventType),
                    () -> log.debug("🔍 条件不满足，跳过事件发布 - 用户ID: {}, 事件: {}", 
                            user != null ? user.getId() : "null", eventType.getDescription())
                );
    }
}
