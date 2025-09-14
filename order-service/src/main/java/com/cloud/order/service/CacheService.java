package com.cloud.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务类
 * 提供订单相关的缓存操作方法
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存值
     *
     * @param key   键
     * @param value 值
     * @param <T>   值的类型
     */
    public <T> void set(String key, T value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("设置缓存成功，key: {}", key);
        } catch (Exception e) {
            log.error("设置缓存失败，key: {}", key, e);
        }
    }

    /**
     * 设置缓存值并指定过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @param <T>     值的类型
     */
    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("设置缓存成功，key: {}，过期时间: {} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("设置缓存失败，key: {}", key, e);
        }
    }

    /**
     * 获取缓存值
     *
     * @param key 键
     * @param <T> 值的类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            T value = (T) redisTemplate.opsForValue().get(key);
            log.debug("获取缓存成功，key: {}", key);
            return value;
        } catch (Exception e) {
            log.error("获取缓存失败，key: {}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("删除缓存成功，key: {}，结果: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("删除缓存失败，key: {}", key, e);
            return false;
        }
    }

    /**
     * 批量删除缓存
     *
     * @param keys 键集合
     * @return 删除的键数量
     */
    public Long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            log.debug("批量删除缓存成功，keys数量: {}，删除数量: {}", keys.size(), count);
            return count;
        } catch (Exception e) {
            log.error("批量删除缓存失败，keys数量: {}", keys.size(), e);
            return 0L;
        }
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            log.debug("检查缓存键是否存在，key: {}，结果: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("检查缓存键是否存在失败，key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存过期时间
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.expire(key, timeout, unit);
            log.debug("设置缓存过期时间成功，key: {}，过期时间: {} {}", key, timeout, unit);
            return result;
        } catch (Exception e) {
            log.error("设置缓存过期时间失败，key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取缓存剩余过期时间
     *
     * @param key 键
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示键不存在
     */
    public Long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key);
            log.debug("获取缓存剩余过期时间，key: {}，剩余时间: {}", key, expire);
            return expire;
        } catch (Exception e) {
            log.error("获取缓存剩余过期时间失败，key: {}", key, e);
            return -2L;
        }
    }

    /**
     * 自增操作
     *
     * @param key   键
     * @param delta 增量
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            log.debug("缓存值自增成功，key: {}，增量: {}，结果: {}", key, delta, value);
            return value;
        } catch (Exception e) {
            log.error("缓存值自增失败，key: {}，增量: {}", key, delta, e);
            return null;
        }
    }

    /**
     * 自减操作
     *
     * @param key   键
     * @param delta 减量
     * @return 自减后的值
     */
    public Long decrement(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().decrement(key, delta);
            log.debug("缓存值自减成功，key: {}，减量: {}，结果: {}", key, delta, value);
            return value;
        } catch (Exception e) {
            log.error("缓存值自减失败，key: {}，减量: {}", key, delta, e);
            return null;
        }
    }

    /**
     * 向List尾部添加元素
     *
     * @param key   键
     * @param value 值
     * @param <T>   值的类型
     * @return List的长度
     */
    public <T> Long listRightPush(String key, T value) {
        try {
            Long size = redisTemplate.opsForList().rightPush(key, value);
            log.debug("向List尾部添加元素成功，key: {}，值: {}，List长度: {}", key, value, size);
            return size;
        } catch (Exception e) {
            log.error("向List尾部添加元素失败，key: {}，值: {}", key, value, e);
            return null;
        }
    }

    /**
     * 从List头部获取并移除元素
     *
     * @param key 键
     * @param <T> 值的类型
     * @return 元素值
     */
    @SuppressWarnings("unchecked")
    public <T> T listLeftPop(String key) {
        try {
            T value = (T) redisTemplate.opsForList().leftPop(key);
            log.debug("从List头部获取并移除元素成功，key: {}，值: {}", key, value);
            return value;
        } catch (Exception e) {
            log.error("从List头部获取并移除元素失败，key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取List指定范围的元素
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @param <T>   值的类型
     * @return 元素列表
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> listRange(String key, long start, long end) {
        try {
            List<T> list = (List<T>) redisTemplate.opsForList().range(key, start, end);
            log.debug("获取List指定范围元素成功，key: {}，范围: {}-{}，元素数量: {}", key, start, end, list != null ? list.size() : 0);
            return list;
        } catch (Exception e) {
            log.error("获取List指定范围元素失败，key: {}，范围: {}-{}", key, start, end, e);
            return null;
        }
    }

    /**
     * 向Set集合添加元素
     *
     * @param key    键
     * @param values 值集合
     * @param <T>    值的类型
     * @return 添加成功的元素数量
     */
    @SafeVarargs
    public final <T> Long setAdd(String key, T... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            log.debug("向Set集合添加元素成功，key: {}，元素数量: {}，添加数量: {}", key, values.length, count);
            return count;
        } catch (Exception e) {
            log.error("向Set集合添加元素失败，key: {}，元素数量: {}", key, values.length, e);
            return 0L;
        }
    }

    /**
     * 获取Set集合的所有元素
     *
     * @param key 键
     * @param <T> 值的类型
     * @return 元素集合
     */
    @SuppressWarnings("unchecked")
    public <T> java.util.Set<T> setMembers(String key) {
        try {
            java.util.Set<T> members = (java.util.Set<T>) redisTemplate.opsForSet().members(key);
            log.debug("获取Set集合所有元素成功，key: {}，元素数量: {}", key, members != null ? members.size() : 0);
            return members;
        } catch (Exception e) {
            log.error("获取Set集合所有元素失败，key: {}", key, e);
            return null;
        }
    }
}