package com.cloud.stock.exception;

import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;









@Slf4j
@Hidden
@RestControllerAdvice(basePackages = "com.cloud")
public class StockGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    
    

    


    @ExceptionHandler(StockOperationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleStockOperationException(StockOperationException ex) {
        log.warn("搴撳瓨鎿嶄綔寮傚父: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    


    @ExceptionHandler(StockInsufficientException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleStockInsufficientException(StockInsufficientException ex) {
        log.warn("搴撳瓨涓嶈冻寮傚父: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }
}
