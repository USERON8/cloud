package com.cloud.common.messaging;

import com.cloud.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 异步消息生产者
 * 用于发送对一致性要求不高的消息，如日志、统计、通知等
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncMessageProducer {

    private final StreamBridge streamBridge;

    /**
     * 异步发送消息 - 不阻塞主业务流程
     * 适用于日志记录、统计更新、通知发送等场景
     *
     * @param bindingName 绑定名称
     * @param payload     消息载荷
     * @param tag         消息标签
     * @param key         消息键
     * @param eventType   事件类型
     * @param <T>         消息类型
     * @return CompletableFuture<Boolean> 异步结果
     */
    @Async("asyncMessageExecutor")
    public <T> CompletableFuture<Boolean> sendAsync(String bindingName, T payload,
                                                    String tag, String key, String eventType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建消息头
                Map<String, Object> headers = buildHeaders(tag, key, eventType);

                // 构建消息
                Message<T> message = new GenericMessage<>(payload, headers);
                String traceId = (String) headers.get("traceId");

                // 发送消息
                boolean sent = streamBridge.send(bindingName, message);

                if (sent) {
                    log.debug("✅ 异步消息发送成功 - 绑定: {}, 事件类型: {}, Tag: {}, TraceId: {}",
                            bindingName, eventType, tag, traceId);
                } else {
                    log.warn("⚠️ 异步消息发送失败 - 绑定: {}, 事件类型: {}, Tag: {}, TraceId: {}",
                            bindingName, eventType, tag, traceId);
                }

                return sent;

            } catch (Exception e) {
                log.error("❌ 异步消息发送异常 - 绑定: {}, 事件类型: {}, Tag: {}, 错误: {}",
                        bindingName, eventType, tag, e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * 异步发送消息 - 带重试机制
     * 适用于重要但对实时性要求不高的消息
     *
     * @param bindingName 绑定名称
     * @param payload     消息载荷
     * @param tag         消息标签
     * @param key         消息键
     * @param eventType   事件类型
     * @param maxRetries  最大重试次数
     * @param <T>         消息类型
     * @return CompletableFuture<Boolean> 异步结果
     */
    @Async("asyncMessageExecutor")
    public <T> CompletableFuture<Boolean> sendAsyncWithRetry(String bindingName, T payload,
                                                             String tag, String key, String eventType,
                                                             int maxRetries) {
        return CompletableFuture.supplyAsync(() -> {
            int attempts = 0;
            Exception lastException = null;

            while (attempts <= maxRetries) {
                try {
                    // 构建消息头
                    Map<String, Object> headers = buildHeaders(tag, key, eventType);
                    headers.put("retryAttempt", attempts);

                    // 构建消息
                    Message<T> message = new GenericMessage<>(payload, headers);
                    String traceId = (String) headers.get("traceId");

                    // 发送消息
                    boolean sent = streamBridge.send(bindingName, message);

                    if (sent) {
                        if (attempts > 0) {
                            log.info("✅ 异步消息重试发送成功 - 绑定: {}, 事件类型: {}, 重试次数: {}, TraceId: {}",
                                    bindingName, eventType, attempts, traceId);
                        } else {
                            log.debug("✅ 异步消息发送成功 - 绑定: {}, 事件类型: {}, TraceId: {}",
                                    bindingName, eventType, traceId);
                        }
                        return true;
                    }

                    attempts++;
                    if (attempts <= maxRetries) {
                        // 指数退避延迟
                        Thread.sleep(Math.min(1000 * (1L << (attempts - 1)), 10000));
                    }

                } catch (Exception e) {
                    lastException = e;
                    attempts++;

                    if (attempts <= maxRetries) {
                        log.warn("⚠️ 异步消息发送失败，准备重试 - 绑定: {}, 事件类型: {}, 重试次数: {}/{}, 错误: {}",
                                bindingName, eventType, attempts, maxRetries, e.getMessage());

                        try {
                            // 指数退避延迟
                            Thread.sleep(Math.min(1000 * (1L << (attempts - 1)), 10000));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            log.error("❌ 异步消息发送最终失败 - 绑定: {}, 事件类型: {}, 最大重试次数: {}, 最后错误: {}",
                    bindingName, eventType, maxRetries,
                    lastException != null ? lastException.getMessage() : "未知错误");

            return false;
        });
    }

    /**
     * 异步发送消息 - 静默模式（忽略失败）
     * 适用于完全可选的消息，如统计、监控等
     *
     * @param bindingName 绑定名称
     * @param payload     消息载荷
     * @param tag         消息标签
     * @param key         消息键
     * @param eventType   事件类型
     * @param <T>         消息类型
     */
    @Async("asyncMessageExecutor")
    public <T> void sendAsyncSilent(String bindingName, T payload,
                                    String tag, String key, String eventType) {
        try {
            // 构建消息头
            Map<String, Object> headers = buildHeaders(tag, key, eventType);

            // 构建消息
            Message<T> message = new GenericMessage<>(payload, headers);
            String traceId = (String) headers.get("traceId");

            // 发送消息
            boolean sent = streamBridge.send(bindingName, message);

            if (sent) {
                log.debug("✅ 静默异步消息发送成功 - 绑定: {}, 事件类型: {}, TraceId: {}",
                        bindingName, eventType, traceId);
            } else {
                log.debug("⚠️ 静默异步消息发送失败 - 绑定: {}, 事件类型: {}, TraceId: {}",
                        bindingName, eventType, traceId);
            }

        } catch (Exception e) {
            log.debug("❌ 静默异步消息发送异常 - 绑定: {}, 事件类型: {}, 错误: {}",
                    bindingName, eventType, e.getMessage());
            // 静默模式，不抛出异常
        }
    }

    /**
     * 批量异步发送消息
     * 适用于批量操作的消息发送
     *
     * @param bindingName 绑定名称
     * @param payloads    消息载荷列表
     * @param tag         消息标签
     * @param eventType   事件类型
     * @param <T>         消息类型
     * @return CompletableFuture<Integer> 成功发送的消息数量
     */
    @Async("batchProcessExecutor")
    public <T> CompletableFuture<Integer> sendBatchAsync(String bindingName, java.util.List<T> payloads,
                                                         String tag, String eventType) {
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;

            for (int i = 0; i < payloads.size(); i++) {
                try {
                    T payload = payloads.get(i);
                    String key = eventType + "_BATCH_" + i;

                    // 构建消息头
                    Map<String, Object> headers = buildHeaders(tag, key, eventType);
                    headers.put("batchIndex", i);
                    headers.put("batchSize", payloads.size());

                    // 构建消息
                    Message<T> message = new GenericMessage<>(payload, headers);

                    // 发送消息
                    boolean sent = streamBridge.send(bindingName, message);
                    if (sent) {
                        successCount++;
                    }

                } catch (Exception e) {
                    log.warn("❌ 批量异步消息发送失败 - 绑定: {}, 索引: {}, 错误: {}",
                            bindingName, i, e.getMessage());
                }
            }

            log.info("📊 批量异步消息发送完成 - 绑定: {}, 总数: {}, 成功: {}, 失败: {}",
                    bindingName, payloads.size(), successCount, payloads.size() - successCount);

            return successCount;
        });
    }

    /**
     * 构建消息头
     */
    private Map<String, Object> buildHeaders(String tag, String key, String eventType) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_TAGS, tag);
        headers.put(MessageConst.PROPERTY_KEYS, key);
        headers.put("eventType", eventType);
        headers.put("traceId", StringUtils.generateTraceId());
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("async", true);
        return headers;
    }
}
