package com.cloud.merchant.exception;

import com.cloud.common.domain.Result;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Result<Void> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("资源未找到: {}", e.getMessage());
        return Result.error(ResultCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public Result<Void> handleValidationException(ValidationException e) {
        log.warn("参数验证失败: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数验证失败: {}", e.getMessage());
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        log.warn("参数绑定失败: {}", e.getMessage());
        String message = e.getFieldError().getDefaultMessage();
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理权限拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(ResultCode.FORBIDDEN);
    }

    /**
     * 处理用户名未找到异常
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public Result<Void> handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.warn("用户不存在: {}", e.getMessage());
        return Result.error(ResultCode.USER_NOT_FOUND);
    }

    /**
     * 处理凭证错误异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<Void> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("用户名或密码错误: {}", e.getMessage());
        return Result.error(ResultCode.USERNAME_OR_PASSWORD_ERROR);
    }

    /**
     * 处理唯一约束冲突异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据重复: {}", e.getMessage());
        return Result.error(ResultCode.DB_DUPLICATE_KEY);
    }

    /**
     * 处理商家服务特定异常
     */
    @ExceptionHandler(MerchantServiceException.class)
    public Result<Void> handleMerchantServiceException(MerchantServiceException e) {
        log.error("商家服务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理商家不存在异常
     */
    @ExceptionHandler(MerchantNotFoundException.class)
    public Result<Void> handleMerchantNotFoundException(MerchantNotFoundException e) {
        log.warn("商家不存在: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理商家状态异常
     */
    @ExceptionHandler(MerchantStatusException.class)
    public Result<Void> handleMerchantStatusException(MerchantStatusException e) {
        log.warn("商家状态错误: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理商家权限异常
     */
    @ExceptionHandler(MerchantPermissionException.class)
    public Result<Void> handleMerchantPermissionException(MerchantPermissionException e) {
        log.warn("商家权限不足: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理其他未预期异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }
}