package com.cloud.common.cache.listener;

import com.cloud.common.cache.core.MultiLevelCacheManager;
import com.cloud.common.cache.message.CacheMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;












@Slf4j

@RequiredArgsConstructor
public class CacheMessageListener implements MessageListener {

    private final MultiLevelCacheManager cacheManager;
    private final ObjectMapper objectMapper;

    





    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            
            CacheMessage cacheMessage = objectMapper.readValue(message.getBody(), CacheMessage.class);

            
            if (!isValidMessage(cacheMessage)) {
                log.warn("W messagearn", cacheMessage);
                return;
            }

            
            log.debug("? cacheName={}, key={}, operationType={}, nodeId={}",
                    cacheMessage.getCacheName(), cacheMessage.getKey(),
                    cacheMessage.getOperationType(), cacheMessage.getNodeId());

            
            cacheManager.handleCacheMessage(cacheMessage.getCacheName(), cacheMessage);

        } catch (Exception e) {
            log.error("? messageBody={}, error={}",
                    new String(message.getBody()), e.getMessage(), e);
        }
    }

    





    private boolean isValidMessage(CacheMessage message) {
        if (message == null) {
            return false;
        }

        
        if (message.getCacheName() == null || message.getCacheName().trim().isEmpty()) {
            log.warn("cacheName: {}", message);
            return false;
        }

        if (message.getOperationType() == null) {
            log.warn("operationType: {}", message);
            return false;
        }

        if (message.getNodeId() == null || message.getNodeId().trim().isEmpty()) {
            log.warn("nodeId: {}", message);
            return false;
        }

        
        if ((message.getOperationType() == CacheMessage.OperationType.UPDATE ||
                message.getOperationType() == CacheMessage.OperationType.DELETE) &&
                message.getKey() == null) {
            log.warn("UPDATE/DELETEey: {}", message);
            return false;
        }

        return true;
    }

    




    public String getListenerStatus() {
        return String.format("CacheMessageListener[nodeId=%s, status=ACTIVE]",
                cacheManager.getNodeId());
    }
}
