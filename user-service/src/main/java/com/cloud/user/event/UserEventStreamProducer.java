package com.cloud.user.event;

import com.cloud.common.domain.event.UserChangeEvent;
import com.cloud.common.exception.MessageSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 用户事件流生产者
 * 专门负责发送用户事件到日志服务，采用函数式编程风格
 * 基于Stream和异步处理，提供高性能的事件流处理能力
 *
 * @author what's up
 * @since 2025-09-20
 * @version 2.0 - 重构为统一命名规范
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class UserEventStreamProducer {

    // 日志服务绑定名称
    private static final String LOG_SERVICE_BINDING = "log-producer-out-0";
    private static final String USER_LOG_TOPIC = "user-logs";
    
    private final StreamBridge streamBridge;

    /**
     * 发送事件到日志服务 - 主要方法
     */
    public void sendToLogService(UserChangeEvent event, String tag) {
        sendEventWithRetry(event, tag, LOG_SERVICE_BINDING)
                .thenAccept(success -> {
                    if (success) {
                        log.info("✅ 日志服务消息发送成功 - 事件: {}, 用户ID: {}, Tag: {}", 
                                event.getEventType(), event.getUserId(), tag);
                    } else {
                        log.error("❌ 日志服务消息发送失败 - 事件: {}, 用户ID: {}", 
                                event.getEventType(), event.getUserId());
                    }
                })
                .exceptionally(throwable -> {
                    log.error("💥 日志服务消息发送异常 - 事件: {}, 用户ID: {}, 错误: {}", 
                            event.getEventType(), event.getUserId(), throwable.getMessage(), throwable);
                    return null;
                });
    }

    /**
     * 带重试的异步发送 - 函数式风格
     */
    private CompletableFuture<Boolean> sendEventWithRetry(UserChangeEvent event, String tag, String binding) {
        return CompletableFuture.supplyAsync(createMessageSupplier(event, tag, binding))
                .thenCompose(this::sendMessage)
                .handle((success, throwable) -> {
                    if (throwable != null) {
                        log.warn("🔄 消息发送失败，准备重试 - 事件: {}, 错误: {}", 
                                event.getEventType(), throwable.getMessage());
                        // 简单重试一次
                        return retryMessage(event, tag, binding);
                    }
                    return CompletableFuture.completedFuture(success);
                })
                .thenCompose(Function.identity());
    }

    /**
     * 消息构建器 - 函数式风格
     */
    private Supplier<Message<UserChangeEvent>> createMessageSupplier(UserChangeEvent event, String tag, String binding) {
        return () -> {
            Map<String, Object> headers = createLogServiceHeaders(event, tag);
            return new GenericMessage<>(event, headers);
        };
    }

    /**
     * 异步消息发送
     */
    private CompletableFuture<Boolean> sendMessage(Message<UserChangeEvent> message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean sent = streamBridge.send(LOG_SERVICE_BINDING, message);
                if (!sent) {
                    throw new MessageSendException("StreamBridge发送失败");
                }
                return sent;
            } catch (Exception e) {
                throw new RuntimeException("消息发送异常", e);
            }
        });
    }

    /**
     * 重试逻辑
     */
    private CompletableFuture<Boolean> retryMessage(UserChangeEvent event, String tag, String binding) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100); // 简单延迟
                Message<UserChangeEvent> retryMessage = createMessageSupplier(event, tag, binding).get();
                return streamBridge.send(binding, retryMessage);
            } catch (Exception e) {
                log.error("🔄 重试发送失败 - 事件: {}, 错误: {}", event.getEventType(), e.getMessage());
                return false;
            }
        });
    }

    /**
     * 日志服务专用消息头构建器 - 函数式风格
     */
    private Map<String, Object> createLogServiceHeaders(UserChangeEvent event, String tag) {
        return Map.of(
                MessageConst.PROPERTY_TAGS, tag,
                MessageConst.PROPERTY_KEYS, "USER_LOG_" + event.getUserId(),
                "eventType", event.getEventType(),
                "timestamp", event.getTimestamp(),
                "traceId", event.getTraceId(),
                "service", "user-service",
                "logLevel", "INFO",
                "target", "log-service"
        );
    }

    /**
     * 批量发送到日志服务 - 函数式风格
     */
    public void sendBatchToLogService(java.util.List<UserChangeEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("📦 跳过批量发送 - 事件列表为空");
            return;
        }

        java.util.List<CompletableFuture<Boolean>> futures = events.stream()
                .map(event -> sendEventWithRetry(event, "batch-" + event.getEventType().toLowerCase(), LOG_SERVICE_BINDING))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    long successCount = futures.stream()
                            .mapToLong(future -> {
                                try {
                                    return future.get() ? 1 : 0;
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .sum();
                    log.info("📦 批量发送完成 - 总数: {}, 成功: {}", events.size(), successCount);
                })
                .exceptionally(throwable -> {
                    log.error("💥 批量发送异常: {}", throwable.getMessage(), throwable);
                    return null;
                });
    }

    /**
     * 健康检查 - 测试消息发送
     */
    public CompletableFuture<Boolean> healthCheck() {
        UserChangeEvent healthEvent = UserChangeEvent.builder()
                .userId(0L)
                .eventType("HEALTH_CHECK")
                .timestamp(java.time.LocalDateTime.now())
                .traceId("health-" + System.currentTimeMillis())
                .metadata("{\"type\":\"health-check\"}")
                .build();

        return sendEventWithRetry(healthEvent, "health-check", LOG_SERVICE_BINDING)
                .thenApply(success -> {
                    log.info("🏥 日志服务健康检查 - 结果: {}", success ? "正常" : "异常");
                    return success;
                })
                .exceptionally(throwable -> {
                    log.error("🏥 日志服务健康检查异常: {}", throwable.getMessage());
                    return false;
                });
    }
}
