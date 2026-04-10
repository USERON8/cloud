package com.cloud.common.exception;

import com.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.cloud")
@Order(1)
public class GlobalPermissionExceptionHandler {

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException ex) {
    log.warn("Authentication failed: {}", ex.getMessage());
    return buildResponse(401, Result.error(401, "Authentication failed: " + ex.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());
    return buildResponse(403, Result.error(403, "Access denied: " + ex.getMessage()));
  }

  @ExceptionHandler(SecurityException.class)
  ResponseEntity<Result<Void>> handleSecurityException(SecurityException ex) {
    log.warn("Security exception: {}", ex.getMessage());
    return buildResponse(403, Result.error(403, "Security error: " + ex.getMessage()));
  }

  @ExceptionHandler(org.springframework.security.oauth2.jwt.JwtException.class)
  ResponseEntity<Result<Void>> handleJwtException(
      org.springframework.security.oauth2.jwt.JwtException ex) {
    log.warn("JWT exception: {}", ex.getMessage());
    return buildResponse(401, Result.error(401, "Invalid JWT token: " + ex.getMessage()));
  }

  private <T> ResponseEntity<Result<T>> buildResponse(int httpStatus, Result<T> body) {
    return ResponseEntity.status(httpStatus).body(body);
  }
}
