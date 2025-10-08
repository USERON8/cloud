package com.cloud.common.cache.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存对象包装类
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheObject<T> {

    /**
     * 缓存的实际数据
     */
    private T value;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * TTL (毫秒)
     */
    private Long ttl;

    /**
     * 是否已过期
     */
    private boolean expired;

    /**
     * 静态工厂方法 - 创建有值的缓存对象
     */
    public static <T> CacheObject<T> of(T value, LocalDateTime createTime, Long ttl, boolean expired) {
        return new CacheObject<>(value, createTime, ttl, expired);
    }

    /**
     * 静态工厂方法 - 简化创建
     */
    public static <T> CacheObject<T> of(T value, long ttl) {
        return new CacheObject<>(value, LocalDateTime.now(), ttl * 1000L, false); // 转换为毫秒
    }

    /**
     * 静态工厂方法 - 创建空的缓存对象
     */
    public static <T> CacheObject<T> empty(LocalDateTime createTime, Long ttl, boolean expired) {
        return new CacheObject<>(null, createTime, ttl, expired);
    }

    /**
     * 静态工厂方法 - 创建null对象
     */
    public static <T> CacheObject<T> nullObject(long ttl) {
        return new CacheObject<>(null, LocalDateTime.now(), ttl * 1000L, false); // 转换为毫秒
    }

    /**
     * 检查缓存对象是否有效
     */
    public boolean isValid() {
        if (expired) {
            return false;
        }

        if (ttl != null && ttl > 0) {
            LocalDateTime expireTime = createTime.plusNanos(ttl * 1_000_000); // ttl是毫秒
            return LocalDateTime.now().isBefore(expireTime);
        }

        return true;
    }

    /**
     * 获取真实值
     */
    public T getRealValue() {
        return this.value;
    }
}
