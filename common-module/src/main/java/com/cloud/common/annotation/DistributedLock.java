package com.cloud.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * 基于Redisson实现的声明式分布式锁，支持方法级别的锁控制
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的名称/键
     * 支持SpEL表达式，可以使用方法参数
     * 例如：'user:#{#userId}' 或 'order:#{#orderId}'
     *
     * @return 锁键
     */
    String key();

    /**
     * 锁的前缀
     * 默认为空，如果设置则最终锁键为：prefix:key
     *
     * @return 锁前缀
     */
    String prefix() default "";

    /**
     * 锁等待时间
     * 获取锁的最大等待时间，超过此时间未获取到锁则放弃
     *
     * @return 等待时间
     */
    long waitTime() default 3;

    /**
     * 锁持有时间
     * 锁的最大持有时间，超过此时间自动释放锁
     *
     * @return 持有时间
     */
    long leaseTime() default 10;

    /**
     * 时间单位
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 锁类型
     *
     * @return 锁类型
     */
    LockType lockType() default LockType.REENTRANT;

    /**
     * 获取锁失败时的处理策略
     *
     * @return 失败策略
     */
    LockFailStrategy failStrategy() default LockFailStrategy.THROW_EXCEPTION;

    /**
     * 自定义异常消息
     * 当获取锁失败且策略为抛异常时使用
     *
     * @return 异常消息
     */
    String failMessage() default "获取分布式锁失败";

    /**
     * 是否在方法执行完成后自动释放锁
     *
     * @return 是否自动释放
     */
    boolean autoRelease() default true;

    /**
     * 锁类型枚举
     */
    enum LockType {
        /**
         * 可重入锁（默认）
         */
        REENTRANT,

        /**
         * 公平锁
         */
        FAIR,

        /**
         * 读锁
         */
        READ,

        /**
         * 写锁
         */
        WRITE,

        /**
         * 红锁（多Redis实例）
         */
        RED_LOCK
    }

    /**
     * 锁获取失败策略枚举
     */
    enum LockFailStrategy {
        /**
         * 抛出异常（默认）
         */
        THROW_EXCEPTION,

        /**
         * 返回null
         */
        RETURN_NULL,

        /**
         * 返回默认值
         */
        RETURN_DEFAULT,

        /**
         * 快速失败，不等待
         */
        FAIL_FAST
    }
}
