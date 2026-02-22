package com.cloud.payment.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;





public class PaymentServiceException extends BusinessException {

    public PaymentServiceException(int code, String message) {
        super(code, message);
    }

    public PaymentServiceException(String message) {
        super(ResultCode.PAYMENT_QUERY_FAILED, message);
    }

    public PaymentServiceException(ResultCode resultCode) {
        super(resultCode);
    }

    public PaymentServiceException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }
}
