package com.cloud.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

public final class RedisKeyScanSupport {

  private RedisKeyScanSupport() {}

  public static Set<String> scanKeys(RedisTemplate<String, ?> redisTemplate, String pattern) {
    if (redisTemplate == null || pattern == null || pattern.isBlank()) {
      return Collections.emptySet();
    }
    Set<String> keys =
        redisTemplate.execute(
            (RedisCallback<Set<String>>)
                connection -> {
                  Set<String> scanned = new HashSet<>();
                  ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
                  try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    while (cursor.hasNext()) {
                      scanned.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                  }
                  return scanned;
                });
    return keys == null ? Collections.emptySet() : keys;
  }

  public static long countKeysByPattern(RedisTemplate<String, ?> redisTemplate, String pattern) {
    return scanKeys(redisTemplate, pattern).size();
  }
}
