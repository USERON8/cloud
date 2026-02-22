package com.cloud.payment.exception;





public class InsufficientBalanceException extends PaymentServiceException {

    public InsufficientBalanceException(String message) {
        super(40005, "浣欓涓嶈冻: " + message);
    }
}
