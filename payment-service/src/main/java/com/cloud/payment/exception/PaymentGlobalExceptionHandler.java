package com.cloud.payment.exception;

import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;









@Slf4j
@RestControllerAdvice(basePackages = "com.cloud")
public class PaymentGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    


    @ExceptionHandler(PaymentServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handlePaymentServiceException(PaymentServiceException ex) {
        log.warn("鏀粯鏈嶅姟寮傚父: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }


    


    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleInsufficientBalanceException(InsufficientBalanceException ex) {
        log.warn("浣欓涓嶈冻寮傚父: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }
}
