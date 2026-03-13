package com.cloud.order.service.support;

import com.cloud.order.dto.OrderAggregateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderAggregateCacheService {

    private static final String KEY_PREFIX = "order:aggregate:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${order.cache.aggregate-ttl-seconds:3600}")
    private long aggregateTtlSeconds;

    public OrderAggregateCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public OrderAggregateResponse get(Long mainOrderId) {
        if (mainOrderId == null) {
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(buildKey(mainOrderId));
            if (value instanceof OrderAggregateResponse response) {
                return response;
            }
        } catch (Exception ex) {
            log.warn("Read order aggregate cache failed: mainOrderId={}", mainOrderId, ex);
        }
        return null;
    }

    public void put(Long mainOrderId, OrderAggregateResponse response) {
        if (mainOrderId == null || response == null) {
            return;
        }
        try {
            long ttlSeconds = Math.max(60L, aggregateTtlSeconds);
            redisTemplate.opsForValue().set(buildKey(mainOrderId), response, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("Write order aggregate cache failed: mainOrderId={}", mainOrderId, ex);
        }
    }

    public void evict(Long mainOrderId) {
        if (mainOrderId == null) {
            return;
        }
        try {
            redisTemplate.delete(buildKey(mainOrderId));
        } catch (Exception ex) {
            log.warn("Evict order aggregate cache failed: mainOrderId={}", mainOrderId, ex);
        }
    }

    private String buildKey(Long mainOrderId) {
        return KEY_PREFIX + mainOrderId;
    }
}
