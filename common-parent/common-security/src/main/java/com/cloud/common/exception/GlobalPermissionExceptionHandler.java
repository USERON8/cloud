package com.cloud.common.exception;

import com.cloud.common.result.Result;
import com.cloud.common.trace.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.cloud")
@Order(1)
public class GlobalPermissionExceptionHandler {

    @ExceptionHandler(PermissionException.class)
    public ResponseEntity<Result<Void>> handlePermissionException(PermissionException ex) {
        log.warn("Permission denied: code={}, message={}", ex.getCode(), ex.getMessage());
        int status = translateHttpStatus(ex.getCode());
        return buildResponse(status, Result.error(status, ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildResponse(401, Result.error(401, "Authentication failed: " + ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(403, Result.error(403, "Access denied: " + ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Result<Void>> handleSecurityException(SecurityException ex) {
        log.warn("Security exception: {}", ex.getMessage());
        return buildResponse(403, Result.error(403, "Security error: " + ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtException.class)
    public ResponseEntity<Result<Void>> handleJwtException(org.springframework.security.oauth2.jwt.JwtException ex) {
        log.warn("JWT exception: {}", ex.getMessage());
        return buildResponse(401, Result.error(401, "Invalid JWT token: " + ex.getMessage()));
    }

    private int translateHttpStatus(String errorCode) {
        if (errorCode == null) {
            return 403;
        }
        return switch (errorCode) {
            case "NOT_AUTHENTICATED" -> 401;
            case "PERMISSION_DENIED",
                    "INSUFFICIENT_PERMISSION",
                    "INSUFFICIENT_SCOPE",
                    "ROLE_MISMATCH",
                    "FORBIDDEN_OPERATION" -> 403;
            case "USER_ID_UNKNOWN" -> 400;
            default -> 403;
        };
    }

    private <T> ResponseEntity<Result<T>> buildResponse(int httpStatus, Result<T> body) {
        String traceId = TraceIdUtil.currentTraceId();
        if (body != null) {
            body.withTraceId(traceId);
        }
        HttpHeaders headers = new HttpHeaders();
        if (traceId != null && !traceId.isBlank()) {
            headers.add("X-Trace-Id", traceId);
        }
        return ResponseEntity.status(httpStatus).headers(headers).body(body);
    }
}
