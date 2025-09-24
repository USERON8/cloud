package com.cloud.auth.exception;

import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证服务全局异常处理器
 * 继承公共异常处理器，只处理认证服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 *
 * @author what's up
 * @date 2025-01-15
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    /**
     * 处理用户名不存在异常
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Result<String>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.warn("用户名不存在异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "用户名或密码错误"));
    }

    /**
     * 处理认证失败异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<String>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("认证失败异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, "用户名或密码错误"));
    }

    /**
     * 处理OAuth2相关异常
     */
    @ExceptionHandler(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<String> handleOAuth2AuthenticationException(
            org.springframework.security.oauth2.core.OAuth2AuthenticationException ex) {
        log.warn("OAuth2认证异常: {}", ex.getMessage());
        return Result.error(401, "OAuth2认证失败: " + ex.getError().getDescription());
    }
}
