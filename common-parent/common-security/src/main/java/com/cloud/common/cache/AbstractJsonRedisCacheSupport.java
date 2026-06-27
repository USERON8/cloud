package com.cloud.common.cache;

import com.cloud.common.security.RedisKeyScanSupport;
import com.cloud.common.util.CacheTtlSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;

public abstract class AbstractJsonRedisCacheSupport {

  protected final StringRedisTemplate stringRedisTemplate;
  protected final ObjectMapper objectMapper;

  protected AbstractJsonRedisCacheSupport(
      StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
  }

  protected void clearAllByPrefix(String prefix, Logger log, String cacheName) {
    try {
      Set<String> keys = RedisKeyScanSupport.scanKeys(stringRedisTemplate, prefix + "*");
      if (!keys.isEmpty()) {
        stringRedisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Clear {} cache failed", cacheName, ex);
    }
  }

  protected <T> T getValue(String key, Class<T> type, Duration ttl, Logger log, String cacheName) {
    try {
      String json = stringRedisTemplate.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return null;
      }
      stringRedisTemplate.expire(key, ttl);
      return objectMapper.readValue(json, type);
    } catch (Exception ex) {
      log.warn("Read {} cache failed: key={}", cacheName, key, ex);
      return null;
    }
  }

  protected <T> T getValue(
      String key, TypeReference<T> typeReference, Duration ttl, Logger log, String cacheName) {
    try {
      String json = stringRedisTemplate.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return null;
      }
      stringRedisTemplate.expire(key, ttl);
      return objectMapper.readValue(json, typeReference);
    } catch (Exception ex) {
      log.warn("Read {} cache failed: key={}", cacheName, key, ex);
      return null;
    }
  }

  protected void putValue(String key, Object value, Duration ttl, Logger log, String cacheName) {
    if (value == null) {
      return;
    }
    try {
      stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
    } catch (Exception ex) {
      log.warn("Write {} cache failed: key={}", cacheName, key, ex);
    }
  }

  protected Duration ttl(long ttlSeconds) {
    return CacheTtlSupport.ttlDuration(ttlSeconds);
  }
}
