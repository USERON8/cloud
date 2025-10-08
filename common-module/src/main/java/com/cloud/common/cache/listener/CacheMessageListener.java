package com.cloud.common.cache.listener;

import com.cloud.common.cache.core.MultiLevelCacheManager;
import com.cloud.common.cache.message.CacheMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * 缓存一致性消息监听器
 * <p>
 * 监听Redis Pub/Sub消息，处理跨节点的缓存一致性事件�?
 * 当接收到其他节点发送的缓存变更消息时，清除对应的本地缓存，
 * 确保多节点环境下缓存数据的一致性�?
 *
 * @author CloudDevAgent
 * @version 2.0
 * @since 2025-09-26
 */
@Slf4j
// @Component  // 暂时禁用以避免循环依赖
@RequiredArgsConstructor
public class CacheMessageListener implements MessageListener {

    private final MultiLevelCacheManager cacheManager;
    private final ObjectMapper objectMapper;

    /**
     * 处理Redis Pub/Sub消息
     *
     * @param message Redis消息
     * @param pattern 订阅模式（可为null�?
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 反序列化缓存消息
            CacheMessage cacheMessage = objectMapper.readValue(message.getBody(), CacheMessage.class);

            // 验证消息有效�?
            if (!isValidMessage(cacheMessage)) {
                log.warn("接收到无效的缓存消息: {}", cacheMessage);
                return;
            }

            // 记录消息接收日志
            log.debug("接收到缓存一致性消�? cacheName={}, key={}, operationType={}, nodeId={}",
                    cacheMessage.getCacheName(), cacheMessage.getKey(),
                    cacheMessage.getOperationType(), cacheMessage.getNodeId());

            // 委托给缓存管理器处理
            cacheManager.handleCacheMessage(cacheMessage.getCacheName(), cacheMessage);

        } catch (Exception e) {
            log.error("处理缓存一致性消息异�? messageBody={}, error={}",
                    new String(message.getBody()), e.getMessage(), e);
        }
    }

    /**
     * 验证缓存消息的有效�?
     *
     * @param message 缓存消息
     * @return true-有效，false-无效
     */
    private boolean isValidMessage(CacheMessage message) {
        if (message == null) {
            return false;
        }

        // 检查必要字�?
        if (message.getCacheName() == null || message.getCacheName().trim().isEmpty()) {
            log.warn("缓存消息缺少cacheName: {}", message);
            return false;
        }

        if (message.getOperationType() == null) {
            log.warn("缓存消息缺少operationType: {}", message);
            return false;
        }

        if (message.getNodeId() == null || message.getNodeId().trim().isEmpty()) {
            log.warn("缓存消息缺少nodeId: {}", message);
            return false;
        }

        // 对于UPDATE和DELETE操作，key不能为空
        if ((message.getOperationType() == CacheMessage.OperationType.UPDATE ||
                message.getOperationType() == CacheMessage.OperationType.DELETE) &&
                message.getKey() == null) {
            log.warn("UPDATE/DELETE操作的缓存消息缺少key: {}", message);
            return false;
        }

        return true;
    }

    /**
     * 获取当前监听器状态信�?
     *
     * @return 状态信�?
     */
    public String getListenerStatus() {
        return String.format("CacheMessageListener[nodeId=%s, status=ACTIVE]",
                cacheManager.getNodeId());
    }
}
