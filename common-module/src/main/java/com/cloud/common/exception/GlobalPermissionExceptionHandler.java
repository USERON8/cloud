package com.cloud.common.exception;

import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局权限异常处理器
 * 处理权限相关异常，提供统一的错误响应格式
 * 
 * @author what's up
 */
@Slf4j
@RestControllerAdvice
@Order(1) // 确保权限异常处理器优先执行
public class GlobalPermissionExceptionHandler {
    
    /**
     * 处理自定义权限异常
     * 
     * @param ex 权限异常
     * @return 统一错误响应
     */
    @ExceptionHandler(PermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handlePermissionException(PermissionException ex) {
        log.warn("权限异常: {} - {}", ex.getCode(), ex.getMessage());
        
        return Result.error(translateErrorCode(ex.getCode()), ex.getMessage());
    }
    
    /**
     * 处理Spring Security认证异常
     * 
     * @param ex 认证异常
     * @return 统一错误响应
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException ex) {
        log.warn("认证异常: {}", ex.getMessage());
        
        return Result.error(401, "认证失败：" + ex.getMessage());
    }
    
    /**
     * 处理Spring Security访问拒绝异常
     * 
     * @param ex 访问拒绝异常
     * @return 统一错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("访问拒绝异常: {}", ex.getMessage());
        
        return Result.error(403, "访问被拒绝：" + ex.getMessage());
    }
    
    /**
     * 处理安全相关的运行时异常
     * 
     * @param ex 安全异常
     * @return 统一错误响应
     */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleSecurityException(SecurityException ex) {
        log.warn("安全异常: {}", ex.getMessage());
        
        return Result.error(403, "安全检查失败：" + ex.getMessage());
    }
    
    /**
     * 处理JWT相关异常
     * 
     * @param ex JWT异常
     * @return 统一错误响应
     */
    @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleJwtException(org.springframework.security.oauth2.jwt.JwtException ex) {
        log.warn("JWT异常: {}", ex.getMessage());
        
        return Result.error(401, "JWT令牌无效：" + ex.getMessage());
    }
    
    /**
     * 将内部错误码转换为HTTP响应码
     * 
     * @param errorCode 内部错误码
     * @return HTTP响应码
     */
    private int translateErrorCode(String errorCode) {
        if (errorCode == null) {
            return 403;
        }
        
        switch (errorCode) {
            case "NOT_AUTHENTICATED":
                return 401;
            case "PERMISSION_DENIED":
            case "INSUFFICIENT_PERMISSION":
            case "INSUFFICIENT_SCOPE":
            case "USER_TYPE_MISMATCH":
            case "FORBIDDEN_OPERATION":
                return 403;
            case "USER_TYPE_UNKNOWN":
            case "USER_ID_UNKNOWN":
                return 400;
            default:
                return 403;
        }
    }
}
