package com.cloud.payment.exception;

import com.cloud.common.exception.BusinessException;

/**
 * 支付服务业务异常基类
 * 所有支付服务特定的业务异常都应该继承此类
 */
public class PaymentServiceException extends BusinessException {
    
    public PaymentServiceException(int code, String message) {
        super(code, message);
    }
    
    public PaymentServiceException(String message) {
        super(message);
    }
}