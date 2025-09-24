package com.cloud.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁管理器
 * 基于Redis实现的分布式锁，提供锁的获取、释放、续期等功能
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>基于Redis的分布式锁实现</li>
 *   <li>支持锁的自动过期和手动释放</li>
 *   <li>使用Lua脚本保证操作的原子性</li>
 *   <li>提供锁的监控和统计功能</li>
 * </ul>
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockManager {

    /**
     * 锁键前缀
     */
    private static final String LOCK_PREFIX = "distributed:lock:";
    /**
     * 默认锁过期时间（秒）
     */
    private static final long DEFAULT_EXPIRE_TIME = 30L;
    /**
     * 默认等待时间（毫秒）
     */
    private static final long DEFAULT_WAIT_TIME = 200L;
    /**
     * 释放锁的Lua脚本
     * 确保只有持有锁的线程才能释放锁
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";
    /**
     * 续期锁的Lua脚本
     * 确保只有持有锁的线程才能续期
     */
    private static final String RENEW_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('expire', KEYS[1], ARGV[2]) " +
                    "else " +
                    "    return 0 " +
                    "end";
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey 锁的键
     * @param timeout 锁的过期时间
     * @return 锁信息，获取失败返回null
     */
    public LockInfo tryLock(String lockKey, Duration timeout) {
        return tryLock(lockKey, timeout, Duration.ofMillis(DEFAULT_WAIT_TIME));
    }

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey  锁的键
     * @param timeout  锁的过期时间
     * @param waitTime 等待时间
     * @return 锁信息，获取失败返回null
     */
    public LockInfo tryLock(String lockKey, Duration timeout, Duration waitTime) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = generateLockValue();

        long startTime = System.currentTimeMillis();
        long waitTimeMs = waitTime.toMillis();

        try {
            do {
                // 尝试获取锁
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(fullKey, lockValue, timeout);

                if (Boolean.TRUE.equals(acquired)) {
                    log.debug("✅ 分布式锁获取成功 - 锁键: {}, 锁值: {}, 过期时间: {}s",
                            lockKey, lockValue, timeout.getSeconds());
                    return new LockInfo(fullKey, lockValue, timeout);
                }

                // 未获取到锁，等待一段时间后重试
                if (waitTimeMs > 0) {
                    Thread.sleep(Math.min(50, waitTimeMs));
                    waitTimeMs -= 50;
                }

            } while (System.currentTimeMillis() - startTime < waitTime.toMillis());

            log.debug("❌ 分布式锁获取失败 - 锁键: {}, 等待时间: {}ms", lockKey, waitTime.toMillis());
            return null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 分布式锁获取被中断 - 锁键: {}", lockKey, e);
            return null;
        } catch (Exception e) {
            log.error("❌ 分布式锁获取异常 - 锁键: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放分布式锁
     *
     * @param lockInfo 锁信息
     * @return 是否释放成功
     */
    public boolean unlock(LockInfo lockInfo) {
        if (lockInfo == null) {
            return false;
        }

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);

            Long result = redisTemplate.execute(script,
                    Collections.singletonList(lockInfo.getLockKey()),
                    lockInfo.getLockValue());

            boolean success = result != null && result > 0;

            if (success) {
                log.debug("✅ 分布式锁释放成功 - 锁键: {}", lockInfo.getLockKey());
            } else {
                log.warn("⚠️ 分布式锁释放失败 - 锁键: {}, 可能已过期或被其他线程持有", lockInfo.getLockKey());
            }

            return success;

        } catch (Exception e) {
            log.error("❌ 分布式锁释放异常 - 锁键: {}", lockInfo.getLockKey(), e);
            return false;
        }
    }

    /**
     * 续期分布式锁
     *
     * @param lockInfo 锁信息
     * @param timeout  新的过期时间
     * @return 是否续期成功
     */
    public boolean renewLock(LockInfo lockInfo, Duration timeout) {
        if (lockInfo == null) {
            return false;
        }

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(RENEW_SCRIPT);
            script.setResultType(Long.class);

            Long result = redisTemplate.execute(script,
                    Collections.singletonList(lockInfo.getLockKey()),
                    lockInfo.getLockValue(),
                    String.valueOf(timeout.getSeconds()));

            boolean success = result != null && result > 0;

            if (success) {
                log.debug("✅ 分布式锁续期成功 - 锁键: {}, 新过期时间: {}s",
                        lockInfo.getLockKey(), timeout.getSeconds());
                lockInfo.setTimeout(timeout);
            } else {
                log.warn("⚠️ 分布式锁续期失败 - 锁键: {}, 可能已过期或被其他线程持有",
                        lockInfo.getLockKey());
            }

            return success;

        } catch (Exception e) {
            log.error("❌ 分布式锁续期异常 - 锁键: {}", lockInfo.getLockKey(), e);
            return false;
        }
    }

    /**
     * 检查锁是否存在
     *
     * @param lockKey 锁的键
     * @return 是否存在
     */
    public boolean isLocked(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
    }

    /**
     * 获取锁的剩余过期时间
     *
     * @param lockKey 锁的键
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示不存在
     */
    public long getLockTtl(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        return redisTemplate.getExpire(fullKey, TimeUnit.SECONDS);
    }

    /**
     * 生成锁值
     * 使用UUID + 线程ID确保唯一性
     *
     * @return 锁值
     */
    private String generateLockValue() {
        return UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();
    }

    /**
     * 构建完整的锁键
     *
     * @param lockKey 业务锁键
     * @return 完整的锁键
     */
    public String buildLockKey(String lockKey) {
        return LOCK_PREFIX + lockKey;
    }
}
