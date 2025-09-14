package com.cloud.order.exception;

import com.cloud.common.enums.ResultCode;

/**
 * 订单状态异常
 *
 * @author cloud
 * @since 1.0.0
 * @deprecated 推荐使用通用异常类 InvalidStatusException.order(currentStatus, operation)
 */
@Deprecated
public class OrderStatusException extends OrderServiceException {

    public OrderStatusException(Long orderId, Integer currentStatus, String operation) {
        super(ResultCode.ORDER_STATUS_ERROR,
                String.format("订单状态不正确，订单ID: %d，当前状态: %d，无法执行%s操作",
                        orderId, currentStatus, operation));
    }

    public OrderStatusException(String message) {
        super(ResultCode.ORDER_STATUS_ERROR, message);
    }
}