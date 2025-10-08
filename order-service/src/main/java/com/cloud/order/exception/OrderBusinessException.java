package com.cloud.order.exception;

/**
 * 订单业务异常
 * 用于订单业务流程中的异常处理
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
public class OrderBusinessException extends RuntimeException {

    /**
     * 订单ID
     */
    private final Long orderId;

    /**
     * 错误代码
     */
    private final String errorCode;

    public OrderBusinessException(String message) {
        super(message);
        this.orderId = null;
        this.errorCode = null;
    }

    public OrderBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.orderId = null;
        this.errorCode = null;
    }

    public OrderBusinessException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
        this.errorCode = null;
    }

    public OrderBusinessException(Long orderId, String errorCode, String message) {
        super(message);
        this.orderId = orderId;
        this.errorCode = errorCode;
    }

    public OrderBusinessException(Long orderId, String message, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
        this.errorCode = null;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
