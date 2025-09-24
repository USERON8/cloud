package com.cloud.order.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;

/**
 * 订单服务业务异常基类
 * 所有订单服务特定的业务异常都应该继承此类
 *
 * @author cloud
 * @since 1.0.0
 */
public class OrderServiceException extends BusinessException {

    /**
     * 使用指定的错误码和消息创建异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public OrderServiceException(int code, String message) {
        super(code, message);
    }

    /**
     * 使用默认错误码（ORDER_CREATE_FAILED）和指定消息创建异常
     *
     * @param message 错误消息
     */
    public OrderServiceException(String message) {
        this(ResultCode.ORDER_CREATE_FAILED, message);
    }

    /**
     * 使用指定的错误码创建异常（无消息）
     *
     * @param resultCode 错误码
     */
    public OrderServiceException(ResultCode resultCode) {
        this(resultCode, null);
    }

    /**
     * 使用指定的错误码和消息创建异常
     *
     * @param resultCode 错误码
     * @param message    错误消息
     */
    public OrderServiceException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    /**
     * 使用消息和异常原因创建异常
     *
     * @param message 错误消息
     * @param cause   异常原因
     */
    public OrderServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}