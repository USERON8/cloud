package com.cloud.payment.exception;

/**
 * 支付记录不存在异常
 * 当尝试操作一个不存在的支付记录时抛出此异常
 */
public class PaymentNotFoundException extends PaymentServiceException {

    public PaymentNotFoundException(Long paymentId) {
        super(40403, "支付记录不存在: " + paymentId);
    }

    public PaymentNotFoundException(String paymentNo) {
        super(40403, "支付记录不存在: " + paymentNo);
    }
}