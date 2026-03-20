package com.cloud.common.messaging;

import cn.hutool.core.util.StrUtil;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageIdempotencyService {

  private static final String KEY_PREFIX = "mq:consumed";
  private static final String STATUS_PROCESSING = "PROCESSING";
  private static final String STATUS_SUCCESS = "SUCCESS";

  private final StringRedisTemplate stringRedisTemplate;
  private final ConcurrentMap<String, LocalIdempotencyState> localFallbackStates =
      new ConcurrentHashMap<>();

  @Value("${app.message.idempotent-enabled:true}")
  private boolean idempotentEnabled;

  @Value("${app.message.idempotent-expire-seconds:86400}")
  private long idempotentExpireSeconds;

  @Value("${app.message.idempotent-processing-expire-seconds:1800}")
  private long processingExpireSeconds;

  public boolean tryAcquire(String namespace, String eventId) {
    if (!idempotentEnabled) {
      return true;
    }
    if (StrUtil.isBlank(namespace) || StrUtil.isBlank(eventId)) {
      return true;
    }

    String key = buildKey(namespace, eventId);
    evictExpiredLocalState(key);
    try {
      boolean acquired = tryAcquireInRedis(key);
      if (acquired) {
        localFallbackStates.remove(key);
      }
      return acquired;
    } catch (Exception e) {
      log.warn(
          "Idempotent key acquire failed in Redis, falling back to local guard: namespace={}, eventId={}",
          namespace,
          eventId,
          e);
      return tryAcquireLocally(key);
    }
  }

  public void release(String namespace, String eventId) {
    if (!idempotentEnabled) {
      return;
    }
    if (StrUtil.isBlank(namespace) || StrUtil.isBlank(eventId)) {
      return;
    }
    String key = buildKey(namespace, eventId);
    localFallbackStates.remove(key);
    try {
      stringRedisTemplate.delete(key);
    } catch (Exception e) {
      log.warn("Idempotent key release failed: namespace={}, eventId={}", namespace, eventId, e);
    }
  }

  public void markSuccess(String namespace, String eventId) {
    if (!idempotentEnabled) {
      return;
    }
    if (StrUtil.isBlank(namespace) || StrUtil.isBlank(eventId)) {
      return;
    }

    String key = buildKey(namespace, eventId);
    evictExpiredLocalState(key);
    try {
      stringRedisTemplate
          .opsForValue()
          .set(key, STATUS_SUCCESS, Duration.ofSeconds(Math.max(60, idempotentExpireSeconds)));
      localFallbackStates.remove(key);
    } catch (Exception e) {
      log.warn(
          "Idempotent mark success failed in Redis, falling back to local success marker: namespace={}, eventId={}",
          namespace,
          eventId,
          e);
      localFallbackStates.put(
          key,
          LocalIdempotencyState.success(
              System.currentTimeMillis()
                  + Duration.ofSeconds(Math.max(60, idempotentExpireSeconds)).toMillis()));
    }
  }

  private String buildKey(String namespace, String eventId) {
    return KEY_PREFIX + ":" + namespace + ":" + eventId;
  }

  private boolean tryAcquireInRedis(String key) {
    Boolean acquired =
        stringRedisTemplate
            .opsForValue()
            .setIfAbsent(
                key, STATUS_PROCESSING, Duration.ofSeconds(Math.max(30, processingExpireSeconds)));
    if (Boolean.TRUE.equals(acquired)) {
      return true;
    }

    String status = stringRedisTemplate.opsForValue().get(key);
    if (STATUS_SUCCESS.equals(status) || STATUS_PROCESSING.equals(status)) {
      return false;
    }

    Boolean reacquired =
        stringRedisTemplate
            .opsForValue()
            .setIfAbsent(
                key, STATUS_PROCESSING, Duration.ofSeconds(Math.max(30, processingExpireSeconds)));
    return Boolean.TRUE.equals(reacquired);
  }

  private boolean tryAcquireLocally(String key) {
    long now = System.currentTimeMillis();
    LocalIdempotencyState candidate =
        LocalIdempotencyState.processing(
            now + Duration.ofSeconds(Math.max(30, processingExpireSeconds)).toMillis());

    while (true) {
      LocalIdempotencyState current = localFallbackStates.get(key);
      if (current != null && !current.isExpired(now) && current.blocksAcquire()) {
        return false;
      }
      if (current == null) {
        if (localFallbackStates.putIfAbsent(key, candidate) == null) {
          return true;
        }
        continue;
      }
      if (localFallbackStates.replace(key, current, candidate)) {
        return true;
      }
    }
  }

  private void evictExpiredLocalState(String key) {
    LocalIdempotencyState state = localFallbackStates.get(key);
    if (state != null && state.isExpired(System.currentTimeMillis())) {
      localFallbackStates.remove(key, state);
    }
  }

  private record LocalIdempotencyState(String status, long expiresAtEpochMillis) {

    private static LocalIdempotencyState processing(long expiresAtEpochMillis) {
      return new LocalIdempotencyState(STATUS_PROCESSING, expiresAtEpochMillis);
    }

    private static LocalIdempotencyState success(long expiresAtEpochMillis) {
      return new LocalIdempotencyState(STATUS_SUCCESS, expiresAtEpochMillis);
    }

    private boolean blocksAcquire() {
      return STATUS_PROCESSING.equals(status) || STATUS_SUCCESS.equals(status);
    }

    private boolean isExpired(long now) {
      return expiresAtEpochMillis <= now;
    }
  }
}
