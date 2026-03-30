package com.cloud.order.service.support;

import com.cloud.order.dto.OrderAggregateResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderAggregateCacheService {

  private static final String KEY_PREFIX = "order:aggregate:";

  private final RedisTemplate<String, Object> redisTemplate;
  private Cache<Long, OrderAggregateResponse> localCache;

  @Value("${order.cache.aggregate-ttl-seconds:3600}")
  private long aggregateTtlSeconds;

  @Value("${order.cache.aggregate-l1-max-size:2000}")
  private long aggregateL1MaxSize;

  @Value("${order.cache.aggregate-l1-ttl-seconds:15}")
  private long aggregateL1TtlSeconds;

  public OrderAggregateCacheService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @PostConstruct
  void init() {
    localCache =
        Caffeine.newBuilder()
            .maximumSize(Math.max(100L, aggregateL1MaxSize))
            .expireAfterWrite(Duration.ofSeconds(Math.max(1L, aggregateL1TtlSeconds)))
            .build();
  }

  public OrderAggregateResponse get(Long mainOrderId) {
    if (mainOrderId == null) {
      return null;
    }
    OrderAggregateResponse local = localCache == null ? null : localCache.getIfPresent(mainOrderId);
    if (local != null) {
      return local;
    }
    try {
      Object value = redisTemplate.opsForValue().get(buildKey(mainOrderId));
      if (value instanceof OrderAggregateResponse response) {
        if (localCache != null) {
          localCache.put(mainOrderId, response);
        }
        return response;
      }
    } catch (Exception ex) {
      log.warn("Read order aggregate cache failed: mainOrderId={}", mainOrderId, ex);
      if (localCache != null) {
        localCache.invalidate(mainOrderId);
      }
    }
    return null;
  }

  public void put(Long mainOrderId, OrderAggregateResponse response) {
    if (mainOrderId == null || response == null) {
      return;
    }
    try {
      long ttlSeconds = Math.max(60L, aggregateTtlSeconds);
      if (localCache != null) {
        localCache.put(mainOrderId, response);
      }
      redisTemplate
          .opsForValue()
          .set(buildKey(mainOrderId), response, ttlSeconds, TimeUnit.SECONDS);
    } catch (Exception ex) {
      log.warn("Write order aggregate cache failed: mainOrderId={}", mainOrderId, ex);
      if (localCache != null) {
        localCache.invalidate(mainOrderId);
      }
    }
  }

  public void evict(Long mainOrderId) {
    if (mainOrderId == null) {
      return;
    }
    if (localCache != null) {
      localCache.invalidate(mainOrderId);
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
