package com.cloud.order.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 订单服务业务异常基类
 * 所有订单服务特定的业务异常都应该继承此类
 */
public class OrderServiceException extends BusinessException {
    
    public OrderServiceException(int code, String message) {
        super(code, message);
    }
    
    public OrderServiceException(String message) {
        super(message);
    }
}