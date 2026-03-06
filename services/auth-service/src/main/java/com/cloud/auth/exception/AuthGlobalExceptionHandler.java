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













@Slf4j
@Hidden
@RestControllerAdvice(basePackages = "com.cloud")
public class AuthGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    


    @ExceptionHandler(AuthenticationException.class)
    public Result<String> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("- uri: {}, type: {}, message: {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
        return Result.error(ResultCode.AUTHENTICATION_FAILED);
    }

    


    @ExceptionHandler(OAuth2AuthenticationException.class)
    public Result<String> handleOAuth2AuthenticationException(OAuth2AuthenticationException e, HttpServletRequest request) {
        OAuth2Error error = e.getError();
        String errorCode = error.getErrorCode();
        String errorMessage = error.getDescription();

        log.warn("OAuth2 - uri: {}, errorCode: {}, errorMessage: {}", request.getRequestURI(), errorCode, errorMessage);

        
        return switch (errorCode) {
            case "invalid_request" -> Result.error(ResultCode.OAUTH2_INVALID_REQUEST);
            case "invalid_client" -> Result.error(ResultCode.OAUTH2_INVALID_CLIENT);
            case "invalid_grant" -> Result.error(ResultCode.OAUTH2_INVALID_GRANT);
            case "unauthorized_client" -> Result.error(ResultCode.OAUTH2_UNAUTHORIZED_CLIENT);
            case "unsupported_grant_type" -> Result.error(ResultCode.OAUTH2_UNSUPPORTED_GRANT_TYPE);
            case "invalid_scope" -> Result.error(ResultCode.OAUTH2_INVALID_SCOPE);
            case "access_denied" -> Result.error(ResultCode.OAUTH2_ACCESS_DENIED);
            default -> Result.error(ResultCode.OAUTH2_SERVER_ERROR, "OAuth2璁よ瘉澶辫触: " + errorMessage);
        };
    }

    


    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.ACCESS_DENIED);
    }

    


    @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtEncodingException.class)
    public Result<String> handleJwtEncodingException(org.springframework.security.oauth2.jwt.JwtEncodingException e, HttpServletRequest request) {
        log.error("JWT - uri: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(ResultCode.JWT_GENERATION_FAILED);
    }

    


    @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtValidationException.class)
    public Result<String> handleJwtValidationException(org.springframework.security.oauth2.jwt.JwtValidationException e, HttpServletRequest request) {
        log.warn("JWT - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.JWT_TOKEN_INVALID);
    }

    


    
    
    
    
    

    


    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public Result<String> handleDisabledException(org.springframework.security.authentication.DisabledException e, HttpServletRequest request) {
        log.warn("?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.USER_DISABLED);
    }

    


    @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
    public Result<String> handleLockedException(org.springframework.security.authentication.LockedException e, HttpServletRequest request) {
        log.warn("?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.ACCOUNT_LOCKED);
    }

    


    @ExceptionHandler(org.springframework.security.authentication.CredentialsExpiredException.class)
    public Result<String> handleCredentialsExpiredException(org.springframework.security.authentication.CredentialsExpiredException e, HttpServletRequest request) {
        log.warn("?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.CREDENTIALS_EXPIRED);
    }

    


    @ExceptionHandler(org.springframework.security.authentication.AccountExpiredException.class)
    public Result<String> handleAccountExpiredException(org.springframework.security.authentication.AccountExpiredException e, HttpServletRequest request) {
        log.warn("?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.ACCOUNT_EXPIRED);
    }


}
