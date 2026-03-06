package com.cloud.common.utils;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RedisKeyScanUtils {

    private RedisKeyScanUtils() {
    }

    public static Set<String> scanKeys(RedisTemplate<String, ?> redisTemplate, String pattern, long scanCount) {
        if (redisTemplate == null || pattern == null || pattern.isBlank()) {
            return Set.of();
        }
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> doScan(connection, pattern, scanCount));
    }

    public static long countKeysByPattern(RedisTemplate<String, ?> redisTemplate, String pattern, long scanCount) {
        Set<String> keys = scanKeys(redisTemplate, pattern, scanCount);
        return keys == null ? 0L : keys.size();
    }

    public static long deleteByPattern(RedisTemplate<String, ?> redisTemplate,
                                       String pattern,
                                       long scanCount,
                                       int pipelineBatchSize) {
        Set<String> keys = scanKeys(redisTemplate, pattern, scanCount);
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return deleteKeysInPipeline(redisTemplate, keys, pipelineBatchSize);
    }

    public static long deleteKeysInPipeline(RedisTemplate<String, ?> redisTemplate,
                                            Collection<String> keys,
                                            int pipelineBatchSize) {
        if (redisTemplate == null || keys == null || keys.isEmpty()) {
            return 0L;
        }

        int batchSize = Math.max(1, pipelineBatchSize);
        List<String> allKeys = new ArrayList<>(keys);
        long deleted = 0L;

        for (int start = 0; start < allKeys.size(); start += batchSize) {
            int end = Math.min(start + batchSize, allKeys.size());
            List<String> batch = allKeys.subList(start, end);
            List<Object> result = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                var keyCommands = connection.keyCommands();
                for (String key : batch) {
                    keyCommands.del(serializeKey(key));
                }
                return null;
            });
            deleted += sumDeleteResult(result);
        }
        return deleted;
    }

    public static Map<String, Long> batchTtlSeconds(RedisTemplate<String, ?> redisTemplate,
                                                    Collection<String> keys,
                                                    int pipelineBatchSize) {
        Map<String, Long> ttlMap = new LinkedHashMap<>();
        if (redisTemplate == null || keys == null || keys.isEmpty()) {
            return ttlMap;
        }

        int batchSize = Math.max(1, pipelineBatchSize);
        List<String> allKeys = new ArrayList<>(keys);

        for (int start = 0; start < allKeys.size(); start += batchSize) {
            int end = Math.min(start + batchSize, allKeys.size());
            List<String> batch = allKeys.subList(start, end);
            List<Object> result = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                var keyCommands = connection.keyCommands();
                for (String key : batch) {
                    keyCommands.ttl(serializeKey(key));
                }
                return null;
            });
            for (int i = 0; i < batch.size(); i++) {
                ttlMap.put(batch.get(i), toLong(result, i));
            }
        }
        return ttlMap;
    }

    private static Set<String> doScan(RedisConnection connection, String pattern, long scanCount) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(Math.max(scanCount, 100))
                .build();
        Set<String> keys = new LinkedHashSet<>();
        try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan redis keys with pattern: " + pattern, e);
        }
        return keys;
    }

    private static long sumDeleteResult(List<Object> result) {
        if (result == null || result.isEmpty()) {
            return 0L;
        }
        long deleted = 0L;
        for (Object item : result) {
            if (item instanceof Number number) {
                deleted += number.longValue();
            } else if (item instanceof Boolean bool && bool) {
                deleted += 1L;
            }
        }
        return deleted;
    }

    private static long toLong(List<Object> result, int index) {
        if (result == null || index < 0 || index >= result.size()) {
            return -2L;
        }
        Object item = result.get(index);
        if (item instanceof Number number) {
            return number.longValue();
        }
        if (item == null) {
            return -2L;
        }
        try {
            return Long.parseLong(item.toString());
        } catch (NumberFormatException ex) {
            return -2L;
        }
    }

    private static byte[] serializeKey(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }
}
