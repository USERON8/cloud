package com.cloud.stock.exception;

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

import java.util.Objects;

/**
 * 库存服务全局异常处理器
 * 统一处理库存服务中的各种异常
 *
 * @author cloud
 * @since 1.0.0
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
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
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
     * 处理库存服务特定异常
     */
    @ExceptionHandler(StockServiceException.class)
    public Result<Void> handleStockServiceException(StockServiceException e) {
        log.error("库存服务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理库存记录不存在异常
     */
    @ExceptionHandler(StockNotFoundException.class)
    public Result<Void> handleStockNotFoundException(StockNotFoundException e) {
        log.warn("库存记录不存在: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理库存不足异常
     */
    @ExceptionHandler(InsufficientStockException.class)
    public Result<Void> handleInsufficientStockException(InsufficientStockException e) {
        log.warn("库存不足: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理库存冻结异常
     */
    @ExceptionHandler(FreezeStockException.class)
    public Result<Void> handleFreezeStockException(FreezeStockException e) {
        log.warn("库存冻结失败: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理库存解冻异常
     */
    @ExceptionHandler(UnfreezeStockException.class)
    public Result<Void> handleUnfreezeStockException(UnfreezeStockException e) {
        log.warn("库存解冻失败: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理库存扣减异常
     */
    @ExceptionHandler(ReduceStockException.class)
    public Result<Void> handleReduceStockException(ReduceStockException e) {
        log.warn("库存扣减失败: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理库存更新异常
     */
    @ExceptionHandler(UpdateStockException.class)
    public Result<Void> handleUpdateStockException(UpdateStockException e) {
        log.warn("库存更新失败: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理库存查询异常
     */
    @ExceptionHandler(QueryStockException.class)
    public Result<Void> handleQueryStockException(QueryStockException e) {
        log.error("库存查询失败: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理其他未预期异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未知系统异常: {}", e.getMessage(), e);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "系统异常，请联系管理员");
    }
}