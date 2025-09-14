package com.cloud.common.exception;

/**
 * 无效状态异常 - 通用异常类
 * 用于替代各服务中的 *StatusException
 *
 * @author what's up
 */
public class InvalidStatusException extends BusinessException {

    /**
     * 通过实体名称、当前状态和操作构造异常
     */
    public InvalidStatusException(String entityName, String currentStatus, String operation) {
        super(String.format("%s当前状态为[%s]，无法执行[%s]操作", entityName, currentStatus, operation));
    }

    /**
     * 直接指定消息
     */
    public InvalidStatusException(String message) {
        super(message);
    }

    /**
     * 带错误码和消息
     */
    public InvalidStatusException(int code, String message) {
        super(code, message);
    }

    /**
     * 带异常原因
     */
    public InvalidStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    // 静态工厂方法，用于常见场景
    public static InvalidStatusException order(String currentStatus, String operation) {
        return new InvalidStatusException("订单", currentStatus, operation);
    }

    public static InvalidStatusException payment(String currentStatus, String operation) {
        return new InvalidStatusException("支付记录", currentStatus, operation);
    }

    public static InvalidStatusException product(String currentStatus, String operation) {
        return new InvalidStatusException("商品", currentStatus, operation);
    }

    public static InvalidStatusException stock(String currentStatus, String operation) {
        return new InvalidStatusException("库存", currentStatus, operation);
    }

    public static InvalidStatusException user(String currentStatus, String operation) {
        return new InvalidStatusException("用户", currentStatus, operation);
    }
}
