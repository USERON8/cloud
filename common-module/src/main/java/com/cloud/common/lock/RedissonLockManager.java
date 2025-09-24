package com.cloud.common.lock;

import com.cloud.common.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson分布式锁管理器
 * 提供基于Redisson的分布式锁编程式API
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RedissonClient.class)
public class RedissonLockManager {

    private final RedissonClient redissonClient;

    /**
     * 执行带锁的操作（可重入锁）
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param timeUnit  时间单位
     * @param supplier  执行的操作
     * @param <T>       返回类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                 TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        return executeWithLock(lock, waitTime, leaseTime, timeUnit, supplier);
    }

    /**
     * 执行带锁的操作（可重入锁，默认时间）
     *
     * @param lockKey  锁键
     * @param supplier 执行的操作
     * @param <T>      返回类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, supplier);
    }

    /**
     * 执行带公平锁的操作
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param timeUnit  时间单位
     * @param supplier  执行的操作
     * @param <T>       返回类型
     * @return 操作结果
     */
    public <T> T executeWithFairLock(String lockKey, long waitTime, long leaseTime,
                                     TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getFairLock(lockKey);
        return executeWithLock(lock, waitTime, leaseTime, timeUnit, supplier);
    }

    /**
     * 执行带读锁的操作
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param timeUnit  时间单位
     * @param supplier  执行的操作
     * @param <T>       返回类型
     * @return 操作结果
     */
    public <T> T executeWithReadLock(String lockKey, long waitTime, long leaseTime,
                                     TimeUnit timeUnit, Supplier<T> supplier) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock lock = readWriteLock.readLock();
        return executeWithLock(lock, waitTime, leaseTime, timeUnit, supplier);
    }

    /**
     * 执行带写锁的操作
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param timeUnit  时间单位
     * @param supplier  执行的操作
     * @param <T>       返回类型
     * @return 操作结果
     */
    public <T> T executeWithWriteLock(String lockKey, long waitTime, long leaseTime,
                                      TimeUnit timeUnit, Supplier<T> supplier) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock lock = readWriteLock.writeLock();
        return executeWithLock(lock, waitTime, leaseTime, timeUnit, supplier);
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param timeUnit  时间单位
     * @return 锁信息，获取失败返回null
     */
    public RedissonLockInfo tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (acquired) {
                return new RedissonLockInfo(lockKey, lock, System.currentTimeMillis());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 获取锁被中断 - 锁键: {}", lockKey, e);
        }
        return null;
    }

    /**
     * 释放锁
     *
     * @param lockInfo 锁信息
     * @return 是否释放成功
     */
    public boolean unlock(RedissonLockInfo lockInfo) {
        if (lockInfo == null || lockInfo.getLock() == null) {
            return false;
        }

        try {
            if (lockInfo.getLock().isHeldByCurrentThread()) {
                lockInfo.getLock().unlock();
                log.debug("🔓 释放锁成功 - 锁键: {}", lockInfo.getLockKey());
                return true;
            }
        } catch (Exception e) {
            log.warn("⚠️ 释放锁异常 - 锁键: {}, 异常: {}", lockInfo.getLockKey(), e.getMessage());
        }
        return false;
    }

    /**
     * 检查锁是否被当前线程持有
     *
     * @param lockKey 锁键
     * @return 是否持有
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 检查锁是否被锁定
     *
     * @param lockKey 锁键
     * @return 是否被锁定
     */
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * 获取锁的剩余持有时间
     *
     * @param lockKey 锁键
     * @return 剩余时间（毫秒），-1表示永久持有，-2表示未锁定
     */
    public long remainTimeToLive(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.remainTimeToLive();
    }

    /**
     * 执行带锁的操作（通用方法）
     *
     * @param lock      锁对象
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param timeUnit  时间单位
     * @param supplier  执行的操作
     * @param <T>       返回类型
     * @return 操作结果
     */
    private <T> T executeWithLock(RLock lock, long waitTime, long leaseTime,
                                  TimeUnit timeUnit, Supplier<T> supplier) {
        String lockKey = lock.getName();
        long startTime = System.currentTimeMillis();

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired) {
                throw new LockException("LOCK_ACQUIRE_FAILED",
                        "获取分布式锁失败 - 锁键: " + lockKey);
            }

            long lockWaitTime = System.currentTimeMillis() - startTime;
            log.debug("🔒 获取分布式锁成功 - 锁键: {}, 等待时间: {}ms", lockKey, lockWaitTime);

            // 执行业务逻辑
            long methodStartTime = System.currentTimeMillis();
            T result = supplier.get();
            long methodExecutionTime = System.currentTimeMillis() - methodStartTime;

            log.debug("✅ 业务逻辑执行完成 - 锁键: {}, 执行时间: {}ms", lockKey, methodExecutionTime);

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockException("LOCK_INTERRUPTED",
                    "获取分布式锁被中断 - 锁键: " + lockKey, e);
        } catch (Exception e) {
            log.error("❌ 分布式锁执行异常 - 锁键: {}", lockKey, e);
            throw e;
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    long totalTime = System.currentTimeMillis() - startTime;
                    log.debug("🔓 释放分布式锁 - 锁键: {}, 总耗时: {}ms", lockKey, totalTime);
                } catch (Exception e) {
                    log.warn("⚠️ 释放分布式锁异常 - 锁键: {}, 异常: {}", lockKey, e.getMessage());
                }
            }
        }
    }

    /**
     * Redisson锁信息
     */
    public static class RedissonLockInfo {
        private final String lockKey;
        private final RLock lock;
        private final long acquireTime;

        public RedissonLockInfo(String lockKey, RLock lock, long acquireTime) {
            this.lockKey = lockKey;
            this.lock = lock;
            this.acquireTime = acquireTime;
        }

        public String getLockKey() {
            return lockKey;
        }

        public RLock getLock() {
            return lock;
        }

        public long getAcquireTime() {
            return acquireTime;
        }

        public long getHoldTime() {
            return System.currentTimeMillis() - acquireTime;
        }
    }
}
