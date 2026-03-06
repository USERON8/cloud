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







@Slf4j
@RestControllerAdvice(basePackages = "com.cloud")
@Order(1) 
public class GlobalPermissionExceptionHandler {

    





    @ExceptionHandler(PermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handlePermissionException(PermissionException ex) {
        log.warn("W messagearn", ex.getCode(), ex.getMessage());

        return Result.error(translateErrorCode(ex.getCode()), ex.getMessage());
    }

    





    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException ex) {
        log.warn("W messagearn", ex.getMessage());

        return Result.error(401, "Authentication failed: " + ex.getMessage());
    }

    





    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("W messagearn", ex.getMessage());

        return Result.error(403, "璁块棶琚嫆缁濓細" + ex.getMessage());
    }

    





    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleSecurityException(SecurityException ex) {
        log.warn("W messagearn", ex.getMessage());

        return Result.error(403, "瀹夊叏妫€鏌ュけ璐ワ細" + ex.getMessage());
    }

    





    @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleJwtException(org.springframework.security.oauth2.jwt.JwtException ex) {
        log.warn("JWT: {}", ex.getMessage());

        return Result.error(401, "Invalid JWT token: " + ex.getMessage());
    }

    





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
