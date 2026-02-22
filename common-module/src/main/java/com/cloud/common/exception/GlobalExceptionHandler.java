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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;








@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    


    @ExceptionHandler(UsernameNotFoundException.class)
    public Result<String> handleUsernameNotFoundException(UsernameNotFoundException e, HttpServletRequest request) {
        log.warn("鐢ㄦ埛璁よ瘉澶辫触 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.USER_NOT_FOUND);
    }

    


    @ExceptionHandler(BadCredentialsException.class)
    public Result<String> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        log.warn("鐢ㄦ埛璁よ瘉澶辫触 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.USERNAME_OR_PASSWORD_ERROR);
    }

    


    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("涓氬姟寮傚父 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(SystemException.class)
    public Result<String> handleSystemException(SystemException e, HttpServletRequest request) {
        log.error("绯荤粺寮傚父 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(ValidationException.class)
    public Result<String> handleValidationException(ValidationException e, HttpServletRequest request) {
        log.warn("鍙傛暟鏍￠獙寮傚父 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(ResourceNotFoundException.class)
    public Result<String> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("璧勬簮鏈壘鍒?- uri: {}, resourceType: {}, resourceId: {}", request.getRequestURI(), e.getResourceType(), e.getResourceId());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(PermissionException.class)
    public Result<String> handlePermissionException(PermissionException e, HttpServletRequest request) {
        log.warn("鏉冮檺寮傚父 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(ConcurrencyException.class)
    public Result<String> handleConcurrencyException(ConcurrencyException e, HttpServletRequest request) {
        log.warn("骞跺彂寮傚父 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
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
        log.warn("鏂规硶鍙傛暟鏍￠獙澶辫触 - uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("鍙傛暟鏍￠獙澶辫触: " + errorMessage);
    }

    


    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        List<String> errors = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);
        log.warn("绾︽潫鏍￠獙澶辫触 - uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("鍙傛暟鏍￠獙澶辫触: " + errorMessage);
    }

    


    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("闈炴硶鍙傛暟寮傚父 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.paramError("鍙傛暟閿欒: " + e.getMessage());
    }

    


    @ExceptionHandler(NullPointerException.class)
    public Result<String> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("绌烘寚閽堝紓甯?- uri: {}", request.getRequestURI(), e);
        return Result.systemError();
    }

    


    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public Result<String> handleDataAccessException(org.springframework.dao.DataAccessException e, HttpServletRequest request) {
        log.error("鏁版嵁搴撳紓甯?- uri: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(ResultCode.DB_ERROR);
    }

    


    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public Result<String> handleHttpRequestMethodNotSupportedException(org.springframework.web.HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("璇锋眰鏂规硶涓嶆敮鎸?- uri: {}, method: {}, supported: {}", request.getRequestURI(), request.getMethod(), e.getSupportedMethods());
        return Result.error(ResultCode.METHOD_NOT_ALLOWED);
    }

    


    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public Result<String> handleHttpMediaTypeNotSupportedException(org.springframework.web.HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("璇锋眰濯掍綋绫诲瀷涓嶆敮鎸?- uri: {}, contentType: {}", request.getRequestURI(), request.getContentType());
        return Result.error(ResultCode.BAD_REQUEST);
    }

    


    @ExceptionHandler(com.fasterxml.jackson.core.JsonProcessingException.class)
    public Result<String> handleJsonProcessingException(com.fasterxml.jackson.core.JsonProcessingException e, HttpServletRequest request) {
        log.warn("JSON瑙ｆ瀽澶辫触 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error("璇锋眰鏍煎紡閿欒锛岃妫€鏌SON鏍煎紡");
    }

    


    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Result<String> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("HTTP娑堟伅涓嶅彲璇?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error("Invalid request body format, please check the payload");
    }

    


    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public Result<String> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缂哄皯璇锋眰鍙傛暟 - uri: {}, parameter: {}", request.getRequestURI(), e.getParameterName());
        return Result.paramError("缂哄皯蹇呰鍙傛暟: " + e.getParameterName());
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
        log.warn("鍙傛暟缁戝畾澶辫触 - uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("鍙傛暟缁戝畾澶辫触: " + errorMessage);
    }

    


    @ExceptionHandler(OAuth2Exception.class)
    public Result<String> handleOAuth2Exception(OAuth2Exception e, HttpServletRequest request) {
        log.warn("OAuth2寮傚父 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(JwtException.class)
    public Result<String> handleJwtException(JwtException e, HttpServletRequest request) {
        log.warn("JWT寮傚父 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    


    @ExceptionHandler(LockException.class)
    public Result<String> handleLockException(LockException e, HttpServletRequest request) {
        log.warn("閿佸紓甯?- uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    



    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e, HttpServletRequest request) {
        log.error("鏈煡寮傚父 - uri: {}, type: {}", request.getRequestURI(), e.getClass().getSimpleName(), e);
        return Result.systemError();
    }
}
