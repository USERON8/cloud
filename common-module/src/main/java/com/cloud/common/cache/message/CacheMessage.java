package com.cloud.common.cache.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;








@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheMessage {

    


    private String cacheName;

    


    private Object key;

    


    private OperationType operationType;

    


    private LocalDateTime timestamp;

    


    private String nodeId;

    


    public static CacheMessage put(String cacheName, Object key, OperationType operationType,
                                   LocalDateTime timestamp, String nodeId) {
        return new CacheMessage(cacheName, key, operationType, timestamp, nodeId);
    }

    


    public static CacheMessage update(String cacheName, Object key, String nodeId) {
        return new CacheMessage(cacheName, key, OperationType.UPDATE, LocalDateTime.now(), nodeId);
    }

    


    public static CacheMessage evict(String cacheName, Object key, OperationType operationType,
                                     LocalDateTime timestamp, String nodeId) {
        return new CacheMessage(cacheName, key, operationType, timestamp, nodeId);
    }

    


    public static CacheMessage delete(String cacheName, Object key, String nodeId) {
        return new CacheMessage(cacheName, key, OperationType.DELETE, LocalDateTime.now(), nodeId);
    }

    


    public static CacheMessage clear(String cacheName, OperationType operationType,
                                     LocalDateTime timestamp, String nodeId) {
        return new CacheMessage(cacheName, null, operationType, timestamp, nodeId);
    }

    


    public static CacheMessage clear(String cacheName, String nodeId) {
        return new CacheMessage(cacheName, null, OperationType.CLEAR, LocalDateTime.now(), nodeId);
    }

    


    public enum OperationType {
        PUT("PUT"),
        UPDATE("UPDATE"),
        EVICT("EVICT"),
        DELETE("DELETE"),
        CLEAR("CLEAR");

        private final String value;

        OperationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
