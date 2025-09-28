package com.cloud.common.config.base;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息配置基类
 * 提供RocketMQ消息队列的通用配置和工具方法
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public abstract class BaseMessageConfig {

    @Resource
    protected StreamBridge streamBridge;

    public BaseMessageConfig() {
        log.info("✅ {} - RocketMQ集成启用", getServiceName());
    }

    /**
     * 获取服务名称
     *
     * @return 服务名称
     */
    protected abstract String getServiceName();

    /**
     * 创建通用消息头
     *
     * @param tag       消息标签
     * @param key       消息键
     * @param eventType 事件类型
     * @return 消息头Map
     */
    protected Map<String, Object> createMessageHeaders(String tag, String key, String eventType) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_TAGS, tag);
        headers.put(MessageConst.PROPERTY_KEYS, key);
        headers.put("eventType", eventType);
        headers.put("traceId", generateTraceId());
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("serviceName", getServiceName());
        return headers;
    }

    /**
     * 发送消息的通用方法
     *
     * @param bindingName 绑定名称
     * @param payload     消息载荷
     * @param headers     消息头
     * @param <T>         消息类型
     * @return 发送是否成功
     */
    protected <T> boolean sendMessage(String bindingName, T payload, Map<String, Object> headers) {
        try {
            Message<T> message = new GenericMessage<>(payload, headers);
            String traceId = (String) headers.get("traceId");
            String eventType = (String) headers.get("eventType");

            log.info("📨 准备发送消息 - 绑定: {}, 事件类型: {}, 追踪ID: {}",
                    bindingName, eventType, traceId);

            boolean sent = streamBridge.send(bindingName, message);

            if (sent) {
                log.info("✅ 消息发送成功 - 绑定: {}, 事件类型: {}, 追踪ID: {}",
                        bindingName, eventType, traceId);
            } else {
                log.error("❌ 消息发送失败 - 绑定: {}, 事件类型: {}, 追踪ID: {}",
                        bindingName, eventType, traceId);
            }

            return sent;
        } catch (Exception e) {
            log.error("❌ 发送消息时发生异常 - 绑定: {}, 错误: {}", bindingName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成追踪ID
     *
     * @return 追踪ID
     * @deprecated 使用 {@link com.cloud.common.utils.StringUtils#generateTraceId()} 替代
     */
    @Deprecated
    protected String generateTraceId() {
        return com.cloud.common.utils.StringUtils.generateTraceId();
    }

    /**
     * 记录消息处理开始
     *
     * @param eventType 事件类型
     * @param traceId   追踪ID
     */
    protected void logMessageProcessStart(String eventType, String traceId) {
        log.info("🔄 开始处理消息 - 事件类型: {}, 追踪ID: {}, 服务: {}",
                eventType, traceId, getServiceName());
    }

    /**
     * 记录消息处理成功
     *
     * @param eventType 事件类型
     * @param traceId   追踪ID
     */
    protected void logMessageProcessSuccess(String eventType, String traceId) {
        log.info("✅ 消息处理成功 - 事件类型: {}, 追踪ID: {}, 服务: {}",
                eventType, traceId, getServiceName());
    }

    /**
     * 记录消息处理失败
     *
     * @param eventType 事件类型
     * @param traceId   追踪ID
     * @param error     错误信息
     */
    protected void logMessageProcessError(String eventType, String traceId, String error) {
        log.error("❌ 消息处理失败 - 事件类型: {}, 追踪ID: {}, 服务: {}, 错误: {}",
                eventType, traceId, getServiceName(), error);
    }

    /**
     * 检查消息是否已处理（幂等性检查）
     * 子类可以重写此方法实现具体的幂等性检查逻辑
     *
     * @param traceId 追踪ID
     * @return 是否已处理
     */
    protected boolean isMessageProcessed(String traceId) {
        // 默认实现，子类可以重写
        return false;
    }

    /**
     * 标记消息已处理
     * 子类可以重写此方法实现具体的标记逻辑
     *
     * @param traceId 追踪ID
     */
    protected void markMessageProcessed(String traceId) {
        // 默认实现，子类可以重写
        log.debug("标记消息已处理 - 追踪ID: {}", traceId);
    }
}
