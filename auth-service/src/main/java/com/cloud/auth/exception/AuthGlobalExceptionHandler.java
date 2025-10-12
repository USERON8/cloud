package com.cloud.auth.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证服务全局异常处理器
 * 继承公共异常处理器，只处理认证服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 *
 * 核心原则：
 * 1. Controller层不允许抛异常，所有异常由全局异常处理器统一处理
 * 2. Service层抛出特定异常，由全局异常处理器转换为标准Result格式
 * 3. 所有异常响应统一使用Result包装，不使用ResponseEntity
 *
 * @author cloud
 */
@Slf4j
@Hidden
@RestControllerAdvice
public class AuthGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    /**
     * 处理Spring Security认证异常 - 统一认证失败处理
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<String> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("认证异常 - uri: {}, type: {}, message: {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
        return Result.error(ResultCode.AUTHENTICATION_FAILED);
    }

    /**
     * 处理OAuth2认证异常 - OAuth2.1标准认证失败
     */
    @ExceptionHandler(OAuth2AuthenticationException.class)
    public Result<String> handleOAuth2AuthenticationException(OAuth2AuthenticationException e, HttpServletRequest request) {
        OAuth2Error error = e.getError();
        String errorCode = error.getErrorCode();
        String errorMessage = error.getDescription();

        log.warn("OAuth2认证异常 - uri: {}, errorCode: {}, errorMessage: {}", request.getRequestURI(), errorCode, errorMessage);

        // 根据OAuth2.1标准错误码返回对应的状态码
        return switch (errorCode) {
            case "invalid_request" -> Result.error(ResultCode.OAUTH2_INVALID_REQUEST);
            case "invalid_client" -> Result.error(ResultCode.OAUTH2_INVALID_CLIENT);
            case "invalid_grant" -> Result.error(ResultCode.OAUTH2_INVALID_GRANT);
            case "unauthorized_client" -> Result.error(ResultCode.OAUTH2_UNAUTHORIZED_CLIENT);
            case "unsupported_grant_type" -> Result.error(ResultCode.OAUTH2_UNSUPPORTED_GRANT_TYPE);
            case "invalid_scope" -> Result.error(ResultCode.OAUTH2_INVALID_SCOPE);
            case "access_denied" -> Result.error(ResultCode.OAUTH2_ACCESS_DENIED);
            default -> Result.error(ResultCode.OAUTH2_SERVER_ERROR, "OAuth2认证失败: " + errorMessage);
        };
    }

    /**
     * 处理访问拒绝异常 - 权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("访问被拒绝 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.ACCESS_DENIED);
    }

    /**
     * 处理令牌生成失败异常
     */
    @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtEncodingException.class)
    public Result<String> handleJwtEncodingException(org.springframework.security.oauth2.jwt.JwtEncodingException e, HttpServletRequest request) {
        log.error("JWT编码失败 - uri: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(ResultCode.JWT_GENERATION_FAILED);
    }

    /**
     * 处理令牌验证失败异常
     */
    @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtValidationException.class)
    public Result<String> handleJwtValidationException(org.springframework.security.oauth2.jwt.JwtValidationException e, HttpServletRequest request) {
        log.warn("JWT验证失败 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.JWT_TOKEN_INVALID);
    }

    /**
     * 处理客户端认证异常 - 注释掉不存在的异常类
     */
    // @ExceptionHandler(org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter.BearerTokenAuthenticationEntryPoint.BearerTokenAuthenticationEntryPointException.class)
    // public Result<String> handleBearerTokenAuthenticationEntryPointException(Exception e, HttpServletRequest request) {
    //     log.warn("Bearer Token认证失败 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
    //     return Result.error(ResultCode.UNAUTHORIZED);
    // }

    /**
     * 处理用户状态异常 - 账户锁定、过期等
     */
    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public Result<String> handleDisabledException(org.springframework.security.authentication.DisabledException e, HttpServletRequest request) {
        log.warn("账户已禁用 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.USER_DISABLED);
    }

    /**
     * 处理账户锁定异常
     */
    @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
    public Result<String> handleLockedException(org.springframework.security.authentication.LockedException e, HttpServletRequest request) {
        log.warn("账户已锁定 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.ACCOUNT_LOCKED);
    }

    /**
     * 处理凭证过期异常
     */
    @ExceptionHandler(org.springframework.security.authentication.CredentialsExpiredException.class)
    public Result<String> handleCredentialsExpiredException(org.springframework.security.authentication.CredentialsExpiredException e, HttpServletRequest request) {
        log.warn("凭证已过期 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.CREDENTIALS_EXPIRED);
    }

    /**
     * 处理账户过期异常
     */
    @ExceptionHandler(org.springframework.security.authentication.AccountExpiredException.class)
    public Result<String> handleAccountExpiredException(org.springframework.security.authentication.AccountExpiredException e, HttpServletRequest request) {
        log.warn("账户已过期 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.ACCOUNT_EXPIRED);
    }

    
    
    }