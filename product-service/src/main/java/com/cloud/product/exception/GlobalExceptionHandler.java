package com.cloud.product.exception;

import com.cloud.common.exception.BusinessException;
import com.cloud.common.result.Result;
import com.cloud.product.exception.ProductServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 商品服务全局异常处理器
 * 继承公共异常处理器，只处理商品服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 *
 * @author what's up
 * @date 2025-01-15
 */
@Slf4j
@Component("productGlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {


    /**
     * 处理权限拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Object> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限拒绝 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error("FORBIDDEN", "您没有权限执行此操作");
    }

    /**
     * 处理唯一约束冲突异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Object> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("数据重复冲突 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error("BUSINESS_ERROR", "数据已存在，请检查重复项");
    }

    /**
     * 处理商品服务特定异常
     */
    @ExceptionHandler(ProductServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleProductServiceException(ProductServiceException e, HttpServletRequest request) {
        log.warn("商品服务异常 [{}]: {}", request.getRequestURI(), e.getMessage());

        // 根据异常类型返回更具体的响应
        if (e instanceof ProductServiceException.ProductNotFoundException) {
            return Result.error("NOT_FOUND", "商品不存在");
        } else if (e instanceof ProductServiceException.ProductAlreadyExistsException) {
            return Result.error("BUSINESS_ERROR", "商品已存在");
        } else if (e instanceof ProductServiceException.ProductStatusException) {
            return Result.error("BUSINESS_ERROR", "商品状态不允许执行此操作");
        } else if (e instanceof ProductServiceException.CategoryNotFoundException) {
            return Result.error("NOT_FOUND", "商品分类不存在");
        } else if (e instanceof ProductServiceException.StockInsufficientException) {
            return Result.error("BUSINESS_ERROR", "库存不足");
        } else if (e instanceof ProductServiceException.ProductPermissionException) {
            return Result.error("FORBIDDEN", e.getMessage());
        }

        return Result.error("BUSINESS_ERROR", e.getMessage());
    }


    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleBindException(BindException e, HttpServletRequest request) {
        log.warn("参数绑定失败 [{}]: {}", request.getRequestURI(), e.getMessage());

        String message = e.getFieldError() != null ?
                String.format("参数 %s %s", e.getFieldError().getField(), e.getFieldError().getDefaultMessage()) :
                "参数绑定失败";

        return Result.error("BAD_REQUEST", message);
    }


    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型不匹配 [{}]: 参数 {} 类型错误", request.getRequestURI(), e.getName());
        return Result.error("BAD_REQUEST", String.format("参数 %s 类型错误，期望类型: %s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown"));
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数 [{}]: {}", request.getRequestURI(), e.getParameterName());
        return Result.error("BAD_REQUEST", String.format("缺少必需参数: %s", e.getParameterName()));
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传大小超限 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error("BAD_REQUEST", "文件上传大小超过限制");
    }

    /**
     * 处理数据库完整性违反异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("数据完整性违反 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error("BUSINESS_ERROR", "数据操作失败，请检查数据的完整性");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常 [{}]: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error("SYSTEM_ERROR", "系统内部错误，请稍后重试");
    }

}
