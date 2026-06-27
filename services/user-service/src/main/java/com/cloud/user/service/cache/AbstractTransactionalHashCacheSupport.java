package com.cloud.user.service.cache;

import com.cloud.common.util.CacheTtlSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;

abstract class AbstractTransactionalHashCacheSupport {

  protected Long parseLong(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  protected Integer parseInteger(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  protected String parseString(Object value) {
    return value == null ? null : value.toString();
  }

  protected LocalDateTime parseTime(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return LocalDateTime.parse(value.toString());
    } catch (Exception ex) {
      return null;
    }
  }

  protected String formatTime(LocalDateTime value) {
    return value == null ? null : value.toString();
  }

  protected void putIfNotBlank(Map<String, String> map, String key, String value) {
    if (map == null || key == null || value == null || value.isBlank()) {
      return;
    }
    map.put(key, value);
  }

  protected Duration ttl(long ttlSeconds) {
    return CacheTtlSupport.ttlDuration(ttlSeconds);
  }

  protected void deleteByPattern(
      StringRedisTemplate redisTemplate, String pattern, Logger log, String cacheName) {
    try {
      java.util.Set<String> keys =
          com.cloud.common.security.RedisKeyScanSupport.scanKeys(redisTemplate, pattern);
      if (!keys.isEmpty()) {
        redisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Clear {} cache by pattern failed: pattern={}", cacheName, pattern, ex);
    }
  }
}
