package com.cloud.common.lock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 分布式锁信息
 * 封装锁的相关信息，包括锁键、锁值、过期时间等
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockInfo {

    /**
     * 锁的完整键名
     */
    private String lockKey;

    /**
     * 锁的值（用于标识持有锁的线程）
     */
    private String lockValue;

    /**
     * 锁的过期时间
     */
    private Duration timeout;

    /**
     * 锁的获取时间
     */
    private LocalDateTime acquireTime;

    /**
     * 构造函数
     *
     * @param lockKey   锁键
     * @param lockValue 锁值
     * @param timeout   过期时间
     */
    public LockInfo(String lockKey, String lockValue, Duration timeout) {
        this.lockKey = lockKey;
        this.lockValue = lockValue;
        this.timeout = timeout;
        this.acquireTime = LocalDateTime.now();
    }

    /**
     * 检查锁是否已过期
     *
     * @return 是否过期
     */
    public boolean isExpired() {
        if (acquireTime == null || timeout == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(acquireTime.plus(timeout));
    }

    /**
     * 获取锁的剩余有效时间
     *
     * @return 剩余时间，如果已过期返回Duration.ZERO
     */
    public Duration getRemainingTime() {
        if (acquireTime == null || timeout == null) {
            return Duration.ZERO;
        }

        LocalDateTime expireTime = acquireTime.plus(timeout);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(expireTime)) {
            return Duration.ZERO;
        }

        return Duration.between(now, expireTime);
    }

    /**
     * 获取锁的持有时间
     *
     * @return 持有时间
     */
    public Duration getHoldTime() {
        if (acquireTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(acquireTime, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format("LockInfo{lockKey='%s', lockValue='%s', timeout=%s, acquireTime=%s, expired=%s}",
                lockKey, lockValue, timeout, acquireTime, isExpired());
    }
}
