package com.cloud.user.exception;

import com.cloud.common.exception.GlobalExceptionHandler;
import com.cloud.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 用户服务全局异常处理器
 * 继承公共异常处理器，只处理用户服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 */
@Slf4j
@RestControllerAdvice
public class UserGlobalExceptionHandler extends GlobalExceptionHandler {

    // BusinessException 已由父类 GlobalExceptionHandler 处理

    /**
     * 处理权限拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Object> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限拒绝 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.forbidden("您没有权限执行此操作");
    }

    /**
     * 处理唯一约束冲突异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Object> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("数据重复冲突 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.businessError("数据已存在，请检查重复项");
    }

    /**
     * 处理用户服务特定异常
     */
    @ExceptionHandler(UserServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleUserServiceException(UserServiceException e, HttpServletRequest request) {
        log.warn("用户服务异常 [{}]: {}", request.getRequestURI(), e.getMessage());

        // 根据异常类型返回更具体的响应
        if (e instanceof UserServiceException.UserNotFoundException) {
            return Result.notFound(e.getMessage());
        } else if (e instanceof UserServiceException.UserAlreadyExistsException) {
            return Result.businessError("用户已存在");
        } else if (e instanceof UserServiceException.AddressPermissionException) {
            return Result.forbidden(e.getMessage());
        } else if (e instanceof UserServiceException.FileUploadException) {
            return Result.businessError("文件上传失败: " + e.getMessage());
        } else if (e instanceof UserServiceException.FileSizeExceededException) {
            return Result.badRequest("文件大小超过10MB限制");
        }

        return Result.businessError(e.getMessage());
    }

    // 以下异常已由父类 GlobalExceptionHandler 统一处理:
    // - MethodArgumentNotValidException
    // - ConstraintViolationException 
    // - IllegalArgumentException
    // - NullPointerException

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传大小超限 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.badRequest("文件大小超过10MB限制");
    }

    /**
     * 处理数据库完整性违反异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("数据完整性违反 [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.businessError("数据操作失败，请检查数据的完整性");
    }

    // RuntimeException 和 Exception 已由父类 GlobalExceptionHandler 处理
}