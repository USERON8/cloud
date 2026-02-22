package com.cloud.common.cache.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;







@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheObject<T> {

    


    private T value;

    


    private LocalDateTime createTime;

    


    private Long ttl;

    


    private boolean expired;

    


    public static <T> CacheObject<T> of(T value, LocalDateTime createTime, Long ttl, boolean expired) {
        return new CacheObject<>(value, createTime, ttl, expired);
    }

    


    public static <T> CacheObject<T> of(T value, long ttl) {
        return new CacheObject<>(value, LocalDateTime.now(), ttl * 1000L, false); 
    }

    


    public static <T> CacheObject<T> empty(LocalDateTime createTime, Long ttl, boolean expired) {
        return new CacheObject<>(null, createTime, ttl, expired);
    }

    


    public static <T> CacheObject<T> nullObject(long ttl) {
        return new CacheObject<>(null, LocalDateTime.now(), ttl * 1000L, false); 
    }

    


    public boolean isValid() {
        if (expired) {
            return false;
        }

        if (ttl != null && ttl > 0) {
            LocalDateTime expireTime = createTime.plusNanos(ttl * 1_000_000); 
            return LocalDateTime.now().isBefore(expireTime);
        }

        return true;
    }

    


    public T getRealValue() {
        return this.value;
    }
}
