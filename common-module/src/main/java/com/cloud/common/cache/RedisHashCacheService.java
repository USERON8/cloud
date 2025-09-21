package com.cloud.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Hash类型缓存服务
 * 优化高频字段的存储和访问性能
 * 适用于对象属性频繁单独访问的场景
 *
 * @author what's up
 */
@Slf4j
@Component
public class RedisHashCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Object> hashOperations;

    public RedisHashCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    /**
     * 设置Hash中的单个字段
     *
     * @param key   缓存键
     * @param field 字段名
     * @param value 字段值
     */
    public void hSet(String key, String field, Object value) {
        try {
            hashOperations.put(key, field, value);
            log.debug("设置Hash字段成功: key={}, field={}", key, field);
        } catch (Exception e) {
            log.error("设置Hash字段失败: key={}, field={}", key, field, e);
        }
    }

    /**
     * 设置Hash中的单个字段，并设置过期时间
     *
     * @param key     缓存键
     * @param field   字段名
     * @param value   字段值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void hSet(String key, String field, Object value, long timeout, TimeUnit unit) {
        try {
            hashOperations.put(key, field, value);
            redisTemplate.expire(key, timeout, unit);
            log.debug("设置Hash字段并设置过期时间成功: key={}, field={}, timeout={}", key, field, timeout);
        } catch (Exception e) {
            log.error("设置Hash字段并设置过期时间失败: key={}, field={}", key, field, e);
        }
    }

    /**
     * 批量设置Hash字段
     *
     * @param key    缓存键
     * @param fields 字段映射
     */
    public void hMSet(String key, Map<String, Object> fields) {
        try {
            hashOperations.putAll(key, fields);
            log.debug("批量设置Hash字段成功: key={}, fieldsCount={}", key, fields.size());
        } catch (Exception e) {
            log.error("批量设置Hash字段失败: key={}", key, e);
        }
    }

    /**
     * 批量设置Hash字段，并设置过期时间
     *
     * @param key     缓存键
     * @param fields  字段映射
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void hMSet(String key, Map<String, Object> fields, long timeout, TimeUnit unit) {
        try {
            hashOperations.putAll(key, fields);
            redisTemplate.expire(key, timeout, unit);
            log.debug("批量设置Hash字段并设置过期时间成功: key={}, fieldsCount={}, timeout={}",
                    key, fields.size(), timeout);
        } catch (Exception e) {
            log.error("批量设置Hash字段并设置过期时间失败: key={}", key, e);
        }
    }

    /**
     * 获取Hash中的单个字段
     *
     * @param key   缓存键
     * @param field 字段名
     * @return 字段值
     */
    public Object hGet(String key, String field) {
        try {
            Object value = hashOperations.get(key, field);
            log.debug("获取Hash字段: key={}, field={}, value={}", key, field, value != null);
            return value;
        } catch (Exception e) {
            log.error("获取Hash字段失败: key={}, field={}", key, field, e);
            return null;
        }
    }

    /**
     * 获取Hash中的多个字段
     *
     * @param key    缓存键
     * @param fields 字段名列表
     * @return 字段值列表
     */
    public Map<String, Object> hMGet(String key, Set<String> fields) {
        try {
            Map<String, Object> result = hashOperations.entries(key);
            result.entrySet().removeIf(entry -> !fields.contains(entry.getKey()));
            log.debug("获取Hash多个字段: key={}, fieldsCount={}", key, fields.size());
            return result;
        } catch (Exception e) {
            log.error("获取Hash多个字段失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 获取Hash中的所有字段
     *
     * @param key 缓存键
     * @return 所有字段映射
     */
    public Map<String, Object> hGetAll(String key) {
        try {
            Map<String, Object> result = hashOperations.entries(key);
            log.debug("获取Hash所有字段: key={}, fieldsCount={}", key, result.size());
            return result;
        } catch (Exception e) {
            log.error("获取Hash所有字段失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 判断Hash中是否存在指定字段
     *
     * @param key   缓存键
     * @param field 字段名
     * @return 是否存在
     */
    public Boolean hExists(String key, String field) {
        try {
            Boolean exists = hashOperations.hasKey(key, field);
            log.debug("检查Hash字段存在性: key={}, field={}, exists={}", key, field, exists);
            return exists;
        } catch (Exception e) {
            log.error("检查Hash字段存在性失败: key={}, field={}", key, field, e);
            return false;
        }
    }

    /**
     * 删除Hash中的指定字段
     *
     * @param key    缓存键
     * @param fields 字段名列表
     * @return 删除的字段数量
     */
    public Long hDel(String key, String... fields) {
        try {
            Long count = hashOperations.delete(key, (Object[]) fields);
            log.debug("删除Hash字段: key={}, fieldsCount={}, deletedCount={}", key, fields.length, count);
            return count;
        } catch (Exception e) {
            log.error("删除Hash字段失败: key={}", key, e);
            return 0L;
        }
    }

    /**
     * 获取Hash中字段的数量
     *
     * @param key 缓存键
     * @return 字段数量
     */
    public Long hLen(String key) {
        try {
            Long size = hashOperations.size(key);
            log.debug("获取Hash字段数量: key={}, size={}", key, size);
            return size;
        } catch (Exception e) {
            log.error("获取Hash字段数量失败: key={}", key, e);
            return 0L;
        }
    }

    /**
     * Hash字段自增
     *
     * @param key   缓存键
     * @param field 字段名
     * @param delta 增量
     * @return 自增后的值
     */
    public Long hIncr(String key, String field, long delta) {
        try {
            Long result = hashOperations.increment(key, field, delta);
            log.debug("Hash字段自增: key={}, field={}, delta={}, result={}", key, field, delta, result);
            return result;
        } catch (Exception e) {
            log.error("Hash字段自增失败: key={}, field={}", key, field, e);
            return null;
        }
    }

    /**
     * Hash字段浮点数自增
     *
     * @param key   缓存键
     * @param field 字段名
     * @param delta 增量
     * @return 自增后的值
     */
    public Double hIncrByFloat(String key, String field, double delta) {
        try {
            Double result = hashOperations.increment(key, field, delta);
            log.debug("Hash字段浮点数自增: key={}, field={}, delta={}, result={}", key, field, delta, result);
            return result;
        } catch (Exception e) {
            log.error("Hash字段浮点数自增失败: key={}, field={}", key, field, e);
            return null;
        }
    }
}
