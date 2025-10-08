package com.cloud.common.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一事件发送助手类
 * 
 * 提供标准化的消息发送方法，简化各服务事件生产者的实现
 * 自动处理消息头、日志记录、异常处理
 * 
 * @author CloudDevAgent
 * @since 2025-10-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventSender {

    private final StreamBridge streamBridge;

    /**
     * 发送事件到指定绑定
     * 
     * @param event 事件对象
     * @param binding 绑定名称
     * @param tag 消息标签
     * @param key 消息KEY
     * @param <T> 事件类型
     * @return 是否发送成功
     */
    public <T> boolean sendEvent(T event, String binding, String tag, String key) {
        try {
            // 构建消息头
            Map<String, Object> headers = createMessageHeaders(tag, key);
            
            // 构建消息
            Message<T> message = new GenericMessage<>(event, headers);
            
            log.debug("📨 准备发送事件 - Binding: {}, Tag: {}, Key: {}", binding, tag, key);
            
            // 发送消息
            boolean sent = streamBridge.send(binding, message);
            
            if (sent) {
                log.info("✅ 事件发送成功 - Binding: {}, Tag: {}, Key: {}", binding, tag, key);
            } else {
                log.error("❌ 事件发送失败 - Binding: {}, Tag: {}, Key: {}", binding, tag, key);
            }
            
            return sent;
        } catch (Exception e) {
            log.error("❌ 事件发送异常 - Binding: {}, Tag: {}, Key: {}, 错误: {}", 
                    binding, tag, key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送事件到指定绑定（带自定义消息头）
     * 
     * @param event 事件对象
     * @param binding 绑定名称
     * @param headers 自定义消息头
     * @param <T> 事件类型
     * @return 是否发送成功
     */
    public <T> boolean sendEvent(T event, String binding, Map<String, Object> headers) {
        try {
            // 构建消息
            Message<T> message = new GenericMessage<>(event, headers);
            
            log.debug("📨 准备发送事件(自定义头) - Binding: {}", binding);
            
            // 发送消息
            boolean sent = streamBridge.send(binding, message);
            
            if (sent) {
                log.info("✅ 事件发送成功(自定义头) - Binding: {}", binding);
            } else {
                log.error("❌ 事件发送失败(自定义头) - Binding: {}", binding);
            }
            
            return sent;
        } catch (Exception e) {
            log.error("❌ 事件发送异常(自定义头) - Binding: {}, 错误: {}", 
                    binding, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批量发送事件
     * 
     * @param events 事件列表
     * @param binding 绑定名称
     * @param tag 消息标签
     * @param keyPrefix 消息KEY前缀
     * @param <T> 事件类型
     * @return 成功发送的数量
     */
    public <T> int sendBatchEvents(java.util.List<T> events, String binding, String tag, String keyPrefix) {
        if (events == null || events.isEmpty()) {
            log.warn("⚠️ 跳过批量发送 - 事件列表为空");
            return 0;
        }
        
        int successCount = 0;
        for (int i = 0; i < events.size(); i++) {
            String key = keyPrefix + "_" + i;
            if (sendEvent(events.get(i), binding, tag, key)) {
                successCount++;
            }
        }
        
        log.info("📦 批量发送完成 - 总数: {}, 成功: {}, 失败: {}", 
                events.size(), successCount, events.size() - successCount);
        
        return successCount;
    }

    /**
     * 创建标准消息头
     * 
     * @param tag 消息标签
     * @param key 消息KEY
     * @return 消息头Map
     */
    private Map<String, Object> createMessageHeaders(String tag, String key) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_TAGS, tag);
        headers.put(MessageConst.PROPERTY_KEYS, key);
        headers.put("timestamp", System.currentTimeMillis());
        return headers;
    }

    /**
     * 创建带追踪ID的消息头
     * 
     * @param tag 消息标签
     * @param key 消息KEY
     * @param traceId 追踪ID
     * @return 消息头Map
     */
    public Map<String, Object> createMessageHeaders(String tag, String key, String traceId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_TAGS, tag);
        headers.put(MessageConst.PROPERTY_KEYS, key);
        headers.put("traceId", traceId);
        headers.put("timestamp", System.currentTimeMillis());
        return headers;
    }

    /**
     * 创建带事件类型的消息头
     * 
     * @param tag 消息标签
     * @param key 消息KEY
     * @param traceId 追踪ID
     * @param eventType 事件类型
     * @return 消息头Map
     */
    public Map<String, Object> createMessageHeaders(String tag, String key, String traceId, String eventType) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageConst.PROPERTY_TAGS, tag);
        headers.put(MessageConst.PROPERTY_KEYS, key);
        headers.put("traceId", traceId);
        headers.put("eventType", eventType);
        headers.put("timestamp", System.currentTimeMillis());
        return headers;
    }
}

