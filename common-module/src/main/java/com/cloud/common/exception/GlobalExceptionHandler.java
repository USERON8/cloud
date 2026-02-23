package com.cloud.common.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;








@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    


    @ExceptionHandler(UsernameNotFoundException.class)
    public Result<String> handleUsernameNotFoundException(UsernameNotFoundException e, HttpServletRequest request) {
        log.warn("閻劍鍩涚拋銈堢槈婢惰精瑙?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.USER_NOT_FOUND);
    }

    


    @ExceptionHandler(BadCredentialsException.class)
    public Result<String> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        log.warn("閻劍鍩涚拋銈堢槈婢惰精瑙?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.USERNAME_OR_PASSWORD_ERROR);
    }

    


    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("娑撴艾濮熷鍌氱埗 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(SystemException.class)
    public Result<String> handleSystemException(SystemException e, HttpServletRequest request) {
        log.error("缁崵绮哄鍌氱埗 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(ValidationException.class)
    public Result<String> handleValidationException(ValidationException e, HttpServletRequest request) {
        log.warn("閸欏倹鏆熼弽锟犵崣瀵倸鐖?- uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(ResourceNotFoundException.class)
    public Result<String> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("鐠у嫭绨張顏呭閸?- uri: {}, resourceType: {}, resourceId: {}", request.getRequestURI(), e.getResourceType(), e.getResourceId());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(PermissionException.class)
    public Result<String> handlePermissionException(PermissionException e, HttpServletRequest request) {
        log.warn("閺夊啴妾哄鍌氱埗 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(ConcurrencyException.class)
    public Result<String> handleConcurrencyException(ConcurrencyException e, HttpServletRequest request) {
        log.warn("楠炶泛褰傚鍌氱埗 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<String> errors = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);
        log.warn("閺傝纭堕崣鍌涙殶閺嶏繝鐛欐径杈Е - uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("閸欏倹鏆熼弽锟犵崣婢惰精瑙? " + errorMessage);
    }

    


    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<String> handleHandlerMethodValidationException(HandlerMethodValidationException e, HttpServletRequest request) {
        List<String> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> {
                            String parameterName = result.getMethodParameter().getParameterName();
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
            errors = e.getCrossParameterValidationResults().stream()
                    .map(error -> error.getDefaultMessage() == null ? error.toString() : error.getDefaultMessage())
                    .collect(Collectors.toList());
        }

        String errorMessage = errors.isEmpty() ? e.getMessage() : String.join(", ", errors);
        log.warn("Method parameter validation failed - uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("Parameter validation failed: " + errorMessage);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        List<String> errors = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);
        log.warn("缁撅附娼弽锟犵崣婢惰精瑙?- uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("閸欏倹鏆熼弽锟犵崣婢惰精瑙? " + errorMessage);
    }

    


    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("闂堢偞纭堕崣鍌涙殶瀵倸鐖?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.paramError("閸欏倹鏆熼柨娆掝嚖: " + e.getMessage());
    }

    


    @ExceptionHandler(NullPointerException.class)
    public Result<String> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("缁岀儤瀵氶柦鍫濈磽鐢?- uri: {}", request.getRequestURI(), e);
        return Result.systemError();
    }

    


    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public Result<String> handleDataAccessException(org.springframework.dao.DataAccessException e, HttpServletRequest request) {
        log.error("閺佺増宓佹惔鎾崇磽鐢?- uri: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(ResultCode.DB_ERROR);
    }

    


    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public Result<String> handleHttpRequestMethodNotSupportedException(org.springframework.web.HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("鐠囬攱鐪伴弬瑙勭《娑撳秵鏁幐?- uri: {}, method: {}, supported: {}", request.getRequestURI(), request.getMethod(), e.getSupportedMethods());
        return Result.error(ResultCode.METHOD_NOT_ALLOWED);
    }

    


    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public Result<String> handleHttpMediaTypeNotSupportedException(org.springframework.web.HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("鐠囬攱鐪版刊鎺嶇秼缁鐎锋稉宥嗘暜閹?- uri: {}, contentType: {}", request.getRequestURI(), request.getContentType());
        return Result.error(ResultCode.BAD_REQUEST);
    }

    


    @ExceptionHandler(com.fasterxml.jackson.core.JsonProcessingException.class)
    public Result<String> handleJsonProcessingException(com.fasterxml.jackson.core.JsonProcessingException e, HttpServletRequest request) {
        log.warn("JSON鐟欙絾鐎芥径杈Е - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error("Invalid JSON payload format");
    }

    


    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Result<String> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("HTTP濞戝牊浼呮稉宥呭讲鐠?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error("Invalid request body format, please check the payload");
    }

    


    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public Result<String> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缂傚搫鐨拠閿嬬湴閸欏倹鏆?- uri: {}, parameter: {}", request.getRequestURI(), e.getParameterName());
        return Result.paramError("缂傚搫鐨箛鍛邦洣閸欏倹鏆? " + e.getParameterName());
    }

    


    @ExceptionHandler(org.springframework.validation.BindException.class)
    public Result<String> handleBindException(org.springframework.validation.BindException e, HttpServletRequest request) {
        List<String> errors = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);
        log.warn("閸欏倹鏆熺紒鎴濈暰婢惰精瑙?- uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("閸欏倹鏆熺紒鎴濈暰婢惰精瑙? " + errorMessage);
    }

    


    @ExceptionHandler(OAuth2Exception.class)
    public Result<String> handleOAuth2Exception(OAuth2Exception e, HttpServletRequest request) {
        log.warn("OAuth2瀵倸鐖?- uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(JwtException.class)
    public Result<String> handleJwtException(JwtException e, HttpServletRequest request) {
        log.warn("JWT瀵倸鐖?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(LockException.class)
    public Result<String> handleLockException(LockException e, HttpServletRequest request) {
        log.warn("闁夸礁绱撶敮?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    



    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e, HttpServletRequest request) {
        log.error("閺堫亞鐓″鍌氱埗 - uri: {}, type: {}", request.getRequestURI(), e.getClass().getSimpleName(), e);
        return Result.systemError();
    }
}

