package com.cloud.auth.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;

public final class RedisKeyHelper {

  private RedisKeyHelper() {}

  public static Set<String> scanKeys(RedisTemplate<String, Object> redisTemplate, String pattern) {
    if (redisTemplate == null || pattern == null || pattern.isBlank()) {
      return Collections.emptySet();
    }
    Set<String> keys = redisTemplate.keys(pattern);
    return keys != null ? keys : Collections.emptySet();
  }

  public static long countKeysByPattern(
      RedisTemplate<String, Object> redisTemplate, String pattern) {
    return scanKeys(redisTemplate, pattern).size();
  }

  public static Map<String, Long> batchTtlSeconds(
      RedisTemplate<String, Object> redisTemplate, Set<String> keys) {
    if (redisTemplate == null || keys == null || keys.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, Long> ttlMap = new HashMap<>();
    for (String key : keys) {
      Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
      ttlMap.put(key, ttl);
    }
    return ttlMap;
  }

  public static long deleteKeys(RedisTemplate<String, Object> redisTemplate, Set<String> keys) {
    if (redisTemplate == null || keys == null || keys.isEmpty()) {
      return 0L;
    }
    Long deleted = redisTemplate.delete(keys);
    return deleted == null ? 0L : deleted;
  }
}
