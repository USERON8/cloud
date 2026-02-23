package com.cloud.search.exception;

import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;









@Slf4j
@RestControllerAdvice(basePackages = "com.cloud")
public class SearchGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    



    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleSearchRuntimeException(RuntimeException ex) {
        log.error("鎼滅储鏈嶅姟杩愯鏃跺紓甯? {}", ex.getMessage(), ex);
        return Result.error("鎼滅储鏈嶅姟寮傚父锛岃绋嶅悗閲嶈瘯");
    }
}
