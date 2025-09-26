package com.cloud.log.exception;

import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 日志服务全局异常处理器
 * 继承公共异常处理器，只处理日志服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 *
 * @author what's up
 * @date 2025-01-15
 */
@Slf4j
@RestControllerAdvice("logGlobalExceptionHandler")
public class LogGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    /**
     * 处理日志服务特定异常
     * 可以根据需要添加日志服务特有的异常处理
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleLogRuntimeException(RuntimeException ex) {
        log.error("日志服务运行时异常: {}", ex.getMessage(), ex);
        return Result.error("日志服务异常，请稍后重试");
    }
}
