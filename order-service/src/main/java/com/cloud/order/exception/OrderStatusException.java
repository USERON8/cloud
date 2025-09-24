package com.cloud.order.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 订单状态异常
 * 当订单状态不满足操作条件时抛出此异常
 *
 * @author cloud
 */
public class OrderStatusException extends BusinessException {

    private static final int ORDER_STATUS_ERROR_CODE = 40001;

    public OrderStatusException(String message) {
        super(ORDER_STATUS_ERROR_CODE, message);
    }

    public OrderStatusException(Long orderId, String currentStatus, String operation) {
        super(ORDER_STATUS_ERROR_CODE,
                String.format("订单[ID:%d]当前状态为[%s]，无法执行[%s]操作", orderId, currentStatus, operation));
    }

    public OrderStatusException(String message, Throwable cause) {
        super(ORDER_STATUS_ERROR_CODE, message, cause);
    }
}
