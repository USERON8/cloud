package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import com.cloud.common.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@Slf4j
@Hidden
@RestControllerAdvice(basePackages = "com.cloud")
public class GlobalExceptionHandler {

    @Autowired(required = false)
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Result<String>> handleUsernameNotFoundException(
            UsernameNotFoundException e, HttpServletRequest request) {
        log.warn(
                "Username not found - uri: {}, message: {}",
                request.getRequestURI(),
                e.getMessage());
        return buildResponse(404, Result.error(ResultCode.USER_NOT_FOUND));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<String>> handleBadCredentialsException(
            BadCredentialsException e, HttpServletRequest request) {
        log.warn("Bad credentials - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return buildResponse(401, Result.error(ResultCode.USERNAME_OR_PASSWORD_ERROR));
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<String>> handleBizException(BizException e, HttpServletRequest request) {
        log.warn(
                "Biz exception - uri: {}, code: {}, message: {}",
                request.getRequestURI(),
                e.getCode(),
                e.getMessage());
        return buildResponse(e.getHttpStatus(), Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<Result<String>> handleSystemException(
            SystemException e, HttpServletRequest request) {
        log.error(
                "System exception - uri: {}, code: {}, message: {}",
                request.getRequestURI(),
                e.getCode(),
                e.getMessage(),
                e);
        recordSystemException();
        return buildResponse(500, Result.error(e.getCode(), "系统繁忙，请稍后重试"));
    }

    @ExceptionHandler(RemoteException.class)
    public ResponseEntity<Result<String>> handleRemoteException(
            RemoteException e, HttpServletRequest request) {
        log.error(
                "Remote exception - uri: {}, code: {}, message: {}",
                request.getRequestURI(),
                e.getCode(),
                e.getMessage(),
                e);
        recordSystemException();
        return buildResponse(503, Result.error(e.getCode(), "远程服务不可用，请稍后重试"));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Result<String>> handleValidationException(
            ValidationException e, HttpServletRequest request) {
        log.warn(
                "Validation exception - uri: {}, code: {}, message: {}",
                request.getRequestURI(),
                e.getCode(),
                e.getMessage());
        return buildResponse(400, Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<String>> handleResourceNotFoundException(
            ResourceNotFoundException e, HttpServletRequest request) {
        log.warn(
                "Resource not found - uri: {}, resourceType: {}, resourceId: {}",
                request.getRequestURI(),
                e.getResourceType(),
                e.getResourceId());
        return buildResponse(404, Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(PermissionException.class)
    public ResponseEntity<Result<String>> handlePermissionException(
            PermissionException e, HttpServletRequest request) {
        log.warn(
                "Permission exception - uri: {}, message: {}",
                request.getRequestURI(),
                e.getMessage());
        return buildResponse(403, Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ConcurrencyException.class)
    public ResponseEntity<Result<String>> handleConcurrencyException(
            ConcurrencyException e, HttpServletRequest request) {
        log.warn(
                "Concurrency exception - uri: {}, message: {}",
                request.getRequestURI(),
                e.getMessage());
        return buildResponse(409, Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        List<String> errors =
                e.getBindingResult().getAllErrors().stream()
                        .map(
                                error -> {
                                    if (error instanceof FieldError fieldError) {
                                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                                    }
                                    return error.getDefaultMessage();
                                })
                        .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);
        log.warn(
                "Request body validation failed - uri: {}, errors: {}",
                request.getRequestURI(),
                errorMessage);
        return buildResponse(400, Result.paramError("参数校验失败: " + errorMessage));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<String>> handleHandlerMethodValidationException(
            HandlerMethodValidationException e, HttpServletRequest request) {
        List<String> errors =
                e.getParameterValidationResults().stream()
                        .flatMap(
                                result ->
                                        result.getResolvableErrors().stream()
                                                .map(
                                                        error -> {
                                                            String parameterName =
                                                                    result.getMethodParameter().getParameterName();
                                                            String message = error.getDefaultMessage();
                                                            if (message == null || message.isBlank()) {
                                                                message = error.toString();
                                                            }
                                                            if (parameterName == null || parameterName.isBlank()) {
                                                                return message;
                                                            }
                                                            return parameterName + ": " + message;
                                                        }))
                        .collect(Collectors.toList());

        if (errors.isEmpty()) {
            errors =
                    e.getCrossParameterValidationResults().stream()
                            .map(
                                    error ->
                                            error.getDefaultMessage() == null
                                                    ? error.toString()
                                                    : error.getDefaultMessage())
                            .collect(Collectors.toList());
        }

        String errorMessage = errors.isEmpty() ? e.getMessage() : String.join(", ", errors);
        log.warn(
                "Method parameter validation failed - uri: {}, errors: {}",
                request.getRequestURI(),
                errorMessage);
        return buildResponse(400, Result.paramError("参数校验失败: " + errorMessage));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<String>> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        List<String> errors =
                violations.stream()
                        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                        .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);
        log.warn(
                "Constraint validation failed - uri: {}, errors: {}",
                request.getRequestURI(),
                errorMessage);
        return buildResponse(400, Result.paramError("参数校验失败: " + errorMessage));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<String>> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Illegal argument - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return buildResponse(400, Result.paramError("非法参数: " + e.getMessage()));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Result<String>> handleNullPointerException(
            NullPointerException e, HttpServletRequest request) {
        log.error("Null pointer exception - uri: {}", request.getRequestURI(), e);
        recordSystemException();
        return buildResponse(500, Result.systemError("系统繁忙，请稍后重试"));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Result<String>> handleDataAccessException(
            org.springframework.dao.DataAccessException e, HttpServletRequest request) {
        log.error(
                "Data access exception - uri: {}, message: {}",
                request.getRequestURI(),
                e.getMessage(),
                e);
        recordSystemException();
        return buildResponse(500, Result.error(ResultCode.DB_ERROR));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<String>> handleHttpRequestMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {
        log.warn(
                "HTTP method not allowed - uri: {}, method: {}, supported: {}",
                request.getRequestURI(),
                request.getMethod(),
                e.getSupportedMethods());
        return buildResponse(405, Result.error(ResultCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<String>> handleHttpMediaTypeNotSupportedException(
            org.springframework.web.HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn(
                "Unsupported media type - uri: {}, contentType: {}",
                request.getRequestURI(),
                request.getContentType());
        return buildResponse(400, Result.error(ResultCode.BAD_REQUEST));
    }

    @ExceptionHandler(com.fasterxml.jackson.core.JsonProcessingException.class)
    public ResponseEntity<Result<String>> handleJsonProcessingException(
            com.fasterxml.jackson.core.JsonProcessingException e, HttpServletRequest request) {
        log.warn(
                "Invalid JSON payload - uri: {}, message: {}",
                request.getRequestURI(),
                e.getMessage());
        return buildResponse(400, Result.error("JSON 格式错误"));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Result<String>> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException e,
            HttpServletRequest request) {
        log.warn(
                "Unreadable HTTP message - uri: {}, message: {}",
                request.getRequestURI(),
                e.getMessage());
        return buildResponse(400, Result.error("请求体格式错误，请检查 JSON"));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Result<String>> handleMissingServletRequestParameterException(
            org.springframework.web.bind.MissingServletRequestParameterException e,
            HttpServletRequest request) {
        log.warn(
                "Missing request parameter - uri: {}, parameter: {}",
                request.getRequestURI(),
                e.getParameterName());
        return buildResponse(400, Result.paramError("缺少必填参数: " + e.getParameterName()));
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<Result<String>> handleBindException(
            org.springframework.validation.BindException e, HttpServletRequest request) {
        List<String> errors =
                e.getBindingResult().getAllErrors().stream()
                        .map(
                                error -> {
                                    if (error instanceof FieldError fieldError) {
                                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                                    }
                                    return error.getDefaultMessage();
                                })
                        .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);
        log.warn("Bind validation failed - uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return buildResponse(400, Result.paramError("参数绑定失败: " + errorMessage));
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<Result<String>> handleOAuth2Exception(
            OAuth2Exception e, HttpServletRequest request) {
        log.warn(
                "OAuth2 exception - uri: {}, code: {}, message: {}",
                request.getRequestURI(),
                e.getCode(),
                e.getMessage());
        return buildResponse(400, Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Result<String>> handleJwtException(JwtException e, HttpServletRequest request) {
        log.warn("JWT exception - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return buildResponse(401, Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(LockException.class)
    public ResponseEntity<Result<String>> handleLockException(LockException e, HttpServletRequest request) {
        log.warn(
                "Distributed lock exception - uri: {}, message: {}",
                request.getRequestURI(),
                e.getMessage());
        return buildResponse(409, Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception e, HttpServletRequest request) {
        log.error(
                "Unhandled exception - uri: {}, type: {}",
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                e);
        recordSystemException();
        return buildResponse(500, Result.systemError("系统繁忙，请稍后重试"));
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

    private void recordSystemException() {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("exception.system", "service", serviceName).increment();
    }
}
