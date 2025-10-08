package com.cloud.payment.exception;

import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 支付服务全局异常处理器
 * 继承公共异常处理器，只处理支付服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 *
 * @author what's up
 * @date 2025-01-15
 */
@Slf4j
@RestControllerAdvice("paymentGlobalExceptionHandler")
public class PaymentGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    /**
     * 处理支付服务特定异常
     */
    @ExceptionHandler(PaymentServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handlePaymentServiceException(PaymentServiceException ex) {
        log.warn("支付服务异常: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }


    /**
     * 处理余额不足异常
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleInsufficientBalanceException(InsufficientBalanceException ex) {
        log.warn("余额不足异常: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }
}
