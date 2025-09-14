package com.cloud.common.exception;

/**
 * 不足异常 - 通用异常类
 * 用于库存不足、余额不足等场景
 *
 * @author what's up
 */
public class InsufficientException extends BusinessException {

    /**
     * 通过资源名称、需求量和可用量构造异常
     */
    public InsufficientException(String resourceName, Object required, Object available) {
        super(String.format("%s不足，需要: %s，可用: %s", resourceName, required, available));
    }

    /**
     * 直接指定消息
     */
    public InsufficientException(String message) {
        super(message);
    }

    /**
     * 带错误码和消息
     */
    public InsufficientException(int code, String message) {
        super(code, message);
    }

    /**
     * 带异常原因
     */
    public InsufficientException(String message, Throwable cause) {
        super(message, cause);
    }

    // 静态工厂方法，用于常见场景
    public static InsufficientException stock(Long productId, Integer required, Integer available) {
        return new InsufficientException(
                String.format("商品[ID:%d]库存不足，需要: %d，可用: %d", productId, required, available)
        );
    }

    public static InsufficientException balance(Long userId, Object required, Object available) {
        return new InsufficientException(
                String.format("用户[ID:%d]余额不足，需要: %s，可用: %s", userId, required, available)
        );
    }

    public static InsufficientException points(Long userId, Integer required, Integer available) {
        return new InsufficientException(
                String.format("用户[ID:%d]积分不足，需要: %d，可用: %d", userId, required, available)
        );
    }

    public static InsufficientException permission(String operation) {
        return new InsufficientException(
                String.format("权限不足，无法执行操作: %s", operation)
        );
    }
}
