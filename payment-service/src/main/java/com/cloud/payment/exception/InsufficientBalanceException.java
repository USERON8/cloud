package com.cloud.payment.exception;

/**
 * 余额不足异常
 * 当用户账户余额不足时抛出此异常
 */
public class InsufficientBalanceException extends PaymentServiceException {

    public InsufficientBalanceException(String message) {
        super(40005, "余额不足: " + message);
    }
}