package com.cloud.common.exception;

/**
 * 分布式锁异常
 * 用于处理分布式锁相关的异常情况
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>获取锁超时</li>
 *   <li>锁释放失败</li>
 *   <li>锁续期失败</li>
 *   <li>锁操作异常</li>
 * </ul>
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public class LockException extends BusinessException {

    /**
     * 锁获取超时异常码
     */
    public static final String LOCK_ACQUIRE_TIMEOUT = "LOCK_ACQUIRE_TIMEOUT";

    /**
     * 锁释放失败异常码
     */
    public static final String LOCK_RELEASE_FAILED = "LOCK_RELEASE_FAILED";

    /**
     * 锁续期失败异常码
     */
    public static final String LOCK_RENEW_FAILED = "LOCK_RENEW_FAILED";

    /**
     * 锁操作异常码
     */
    public static final String LOCK_OPERATION_ERROR = "LOCK_OPERATION_ERROR";

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public LockException(String message) {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param code    错误码
     * @param message 异常消息
     */
    public LockException(String code, String message) {
        super(message);
        // Note: BusinessException doesn't have a constructor that takes (String, String)
        // We'll use the message constructor and the code will be the default business error code
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public LockException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     *
     * @param code    错误码
     * @param message 异常消息
     * @param cause   异常原因
     */
    public LockException(String code, String message, Throwable cause) {
        super(message, cause);
        // Note: BusinessException doesn't have a constructor that takes (String, String, Throwable)
        // We'll use the (String, Throwable) constructor
    }

    /**
     * 创建锁获取超时异常
     *
     * @param lockKey  锁键
     * @param waitTime 等待时间
     * @return 锁异常
     */
    public static LockException acquireTimeout(String lockKey, long waitTime) {
        return new LockException(LOCK_ACQUIRE_TIMEOUT,
                String.format("获取分布式锁超时，锁键: %s, 等待时间: %dms", lockKey, waitTime));
    }

    /**
     * 创建锁释放失败异常
     *
     * @param lockKey 锁键
     * @return 锁异常
     */
    public static LockException releaseFailed(String lockKey) {
        return new LockException(LOCK_RELEASE_FAILED,
                String.format("释放分布式锁失败，锁键: %s", lockKey));
    }

    /**
     * 创建锁续期失败异常
     *
     * @param lockKey 锁键
     * @return 锁异常
     */
    public static LockException renewFailed(String lockKey) {
        return new LockException(LOCK_RENEW_FAILED,
                String.format("续期分布式锁失败，锁键: %s", lockKey));
    }

    /**
     * 创建锁操作异常
     *
     * @param lockKey   锁键
     * @param operation 操作类型
     * @param cause     异常原因
     * @return 锁异常
     */
    public static LockException operationError(String lockKey, String operation, Throwable cause) {
        return new LockException(LOCK_OPERATION_ERROR,
                String.format("分布式锁操作异常，锁键: %s, 操作: %s", lockKey, operation), cause);
    }
}
