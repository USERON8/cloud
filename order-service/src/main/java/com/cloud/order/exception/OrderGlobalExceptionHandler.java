package com.cloud.order.exception;

import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 订单服务全局异常处理器
 * 继承公共异常处理器，只处理订单服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 *
 * @author what's up
 * @date 2025-01-15
 */
@Slf4j
@RestControllerAdvice("orderGlobalExceptionHandler")
public class OrderGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    /**
     * 处理订单服务特定异常
     */
    @ExceptionHandler(OrderServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleOrderServiceException(OrderServiceException ex) {
        log.warn("订单服务异常: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }


    /**
     * 处理库存不足异常
     */
    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleInsufficientStockException(InsufficientStockException ex) {
        log.warn("库存不足异常: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }
}
