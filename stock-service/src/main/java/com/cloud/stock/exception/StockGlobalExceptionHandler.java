package com.cloud.stock.exception;

import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 库存服务全局异常处理器
 * 继承公共异常处理器，只处理库存服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 *
 * @author what's up
 * @date 2025-01-15
 */
@Slf4j
@RestControllerAdvice("stockGlobalExceptionHandler")
public class StockGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    // 父类已经处理了MethodArgumentNotValidException和Exception
    // 这里只需要处理库存服务特有的异常

    /**
     * 处理库存操作异常
     */
    @ExceptionHandler(StockOperationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleStockOperationException(StockOperationException ex) {
        log.warn("库存操作异常: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理库存不足异常
     */
    @ExceptionHandler(StockInsufficientException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleStockInsufficientException(StockInsufficientException ex) {
        log.warn("库存不足异常: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }
}
