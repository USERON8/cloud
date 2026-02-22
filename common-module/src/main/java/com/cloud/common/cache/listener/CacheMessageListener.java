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
                log.warn("鎺ユ敹鍒版棤鏁堢殑缂撳瓨娑堟伅: {}", cacheMessage);
                return;
            }

            
            log.debug("鎺ユ敹鍒扮紦瀛樹竴鑷存€ф秷锟? cacheName={}, key={}, operationType={}, nodeId={}",
                    cacheMessage.getCacheName(), cacheMessage.getKey(),
                    cacheMessage.getOperationType(), cacheMessage.getNodeId());

            
            cacheManager.handleCacheMessage(cacheMessage.getCacheName(), cacheMessage);

        } catch (Exception e) {
            log.error("澶勭悊缂撳瓨涓€鑷存€ф秷鎭紓锟? messageBody={}, error={}",
                    new String(message.getBody()), e.getMessage(), e);
        }
    }

    





    private boolean isValidMessage(CacheMessage message) {
        if (message == null) {
            return false;
        }

        
        if (message.getCacheName() == null || message.getCacheName().trim().isEmpty()) {
            log.warn("缂撳瓨娑堟伅缂哄皯cacheName: {}", message);
            return false;
        }

        if (message.getOperationType() == null) {
            log.warn("缂撳瓨娑堟伅缂哄皯operationType: {}", message);
            return false;
        }

        if (message.getNodeId() == null || message.getNodeId().trim().isEmpty()) {
            log.warn("缂撳瓨娑堟伅缂哄皯nodeId: {}", message);
            return false;
        }

        
        if ((message.getOperationType() == CacheMessage.OperationType.UPDATE ||
                message.getOperationType() == CacheMessage.OperationType.DELETE) &&
                message.getKey() == null) {
            log.warn("UPDATE/DELETE鎿嶄綔鐨勭紦瀛樻秷鎭己灏慿ey: {}", message);
            return false;
        }

        return true;
    }

    




    public String getListenerStatus() {
        return String.format("CacheMessageListener[nodeId=%s, status=ACTIVE]",
                cacheManager.getNodeId());
    }
}
