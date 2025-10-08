package com.cloud.common.cache.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存消息类
 * 用于在Redis中传递缓存操作消息
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheMessage {

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存键
     */
    private Object key;

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 静态工厂方法 - 创建PUT操作消息
     */
    public static CacheMessage put(String cacheName, Object key, OperationType operationType,
                                   LocalDateTime timestamp, String nodeId) {
        return new CacheMessage(cacheName, key, operationType, timestamp, nodeId);
    }

    /**
     * 静态工厂方法 - 创建UPDATE操作消息
     */
    public static CacheMessage update(String cacheName, Object key, String nodeId) {
        return new CacheMessage(cacheName, key, OperationType.UPDATE, LocalDateTime.now(), nodeId);
    }

    /**
     * 静态工厂方法 - 创建EVICT操作消息
     */
    public static CacheMessage evict(String cacheName, Object key, OperationType operationType,
                                     LocalDateTime timestamp, String nodeId) {
        return new CacheMessage(cacheName, key, operationType, timestamp, nodeId);
    }

    /**
     * 静态工厂方法 - 创建DELETE操作消息
     */
    public static CacheMessage delete(String cacheName, Object key, String nodeId) {
        return new CacheMessage(cacheName, key, OperationType.DELETE, LocalDateTime.now(), nodeId);
    }

    /**
     * 静态工厂方法 - 创建CLEAR操作消息
     */
    public static CacheMessage clear(String cacheName, OperationType operationType,
                                     LocalDateTime timestamp, String nodeId) {
        return new CacheMessage(cacheName, null, operationType, timestamp, nodeId);
    }

    /**
     * 静态工厂方法 - 简化的CLEAR操作消息
     */
    public static CacheMessage clear(String cacheName, String nodeId) {
        return new CacheMessage(cacheName, null, OperationType.CLEAR, LocalDateTime.now(), nodeId);
    }

    /**
     * 缓存操作类型枚举
     */
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
