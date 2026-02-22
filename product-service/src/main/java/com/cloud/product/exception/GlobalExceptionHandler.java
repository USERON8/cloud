package com.cloud.product.exception;

import com.cloud.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;









@Slf4j
@Component("productGlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {


    


    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Object> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("鏉冮檺鎷掔粷 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error("FORBIDDEN", "鎮ㄦ病鏈夋潈闄愭墽琛屾鎿嶄綔");
    }

    


    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Object> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("鏁版嵁閲嶅鍐茬獊 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error("BUSINESS_ERROR", "鏁版嵁宸插瓨鍦紝璇锋鏌ラ噸澶嶉」");
    }

    


    @ExceptionHandler(ProductServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleProductServiceException(ProductServiceException e, HttpServletRequest request) {
        log.warn("鍟嗗搧鏈嶅姟寮傚父 [{}]: {}", request.getRequestURI(), e.getMessage());

        
        if (e instanceof ProductServiceException.ProductNotFoundException) {
            return Result.error("NOT_FOUND", "鍟嗗搧涓嶅瓨鍦?);
        } else if (e instanceof ProductServiceException.ProductAlreadyExistsException) {
            return Result.error("BUSINESS_ERROR", "鍟嗗搧宸插瓨鍦?);
        } else if (e instanceof ProductServiceException.ProductStatusException) {
            return Result.error("BUSINESS_ERROR", "鍟嗗搧鐘舵€佷笉鍏佽鎵ц姝ゆ搷浣?);
        } else if (e instanceof ProductServiceException.CategoryNotFoundException) {
            return Result.error("NOT_FOUND", "鍟嗗搧鍒嗙被涓嶅瓨鍦?);
        } else if (e instanceof ProductServiceException.StockInsufficientException) {
            return Result.error("BUSINESS_ERROR", "搴撳瓨涓嶈冻");
        } else if (e instanceof ProductServiceException.ProductPermissionException) {
            return Result.error("FORBIDDEN", e.getMessage());
        }

        return Result.error("BUSINESS_ERROR", e.getMessage());
    }


    


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("鍙傛暟绫诲瀷涓嶅尮閰?[{}]: 鍙傛暟 {} 绫诲瀷閿欒", request.getRequestURI(), e.getName());
        return Result.error("BAD_REQUEST", String.format("鍙傛暟 %s 绫诲瀷閿欒锛屾湡鏈涚被鍨? %s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown"));
    }


    


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("鏂囦欢涓婁紶澶у皬瓒呴檺 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error("BAD_REQUEST", "鏂囦欢涓婁紶澶у皬瓒呰繃闄愬埗");
    }

    


    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("鏁版嵁瀹屾暣鎬ц繚鍙?[{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error("BUSINESS_ERROR", "鏁版嵁鎿嶄綔澶辫触锛岃妫€鏌ユ暟鎹殑瀹屾暣鎬?);
    }


}
