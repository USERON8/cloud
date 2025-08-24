package com.cloud.payment.exception;

/**
 * 支付状态异常
 * 当支付状态不满足操作条件时抛出此异常
 */
public class PaymentStatusException extends PaymentServiceException {
    
    public PaymentStatusException(String message) {
        super(40004, "支付状态错误: " + message);
    }
}