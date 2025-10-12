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

/**
 * 统一全局异常处理器 - 各服务可继承此类
 * 提供统一的异常处理标准，所有异常返回统一的Result格式
 * 核心原则：Controller层不允许抛异常，Service层抛出特定异常，由全局异常处理器统一处理
 *
 * @author cloud
 */
@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理用户名未找到异常 - Spring Security认证失败
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public Result<String> handleUsernameNotFoundException(UsernameNotFoundException e, HttpServletRequest request) {
        log.warn("用户认证失败 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.USER_NOT_FOUND);
    }

    /**
     * 处理认证凭证错误异常 - 密码错误
     */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<String> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        log.warn("用户认证失败 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.USERNAME_OR_PASSWORD_ERROR);
    }

    /**
     * 处理业务异常 - Service层抛出的业务逻辑异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理系统异常 - 系统级别的错误
     */
    @ExceptionHandler(SystemException.class)
    public Result<String> handleSystemException(SystemException e, HttpServletRequest request) {
        log.error("系统异常 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常 - 手动参数校验失败
     */
    @ExceptionHandler(ValidationException.class)
    public Result<String> handleValidationException(ValidationException e, HttpServletRequest request) {
        log.warn("参数校验异常 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理资源未找到异常 - 数据不存在
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Result<String> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("资源未找到 - uri: {}, resourceType: {}, resourceId: {}", request.getRequestURI(), e.getResourceType(), e.getResourceId());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理权限异常 - 访问权限不足
     */
    @ExceptionHandler(PermissionException.class)
    public Result<String> handlePermissionException(PermissionException e, HttpServletRequest request) {
        log.warn("权限异常 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理并发异常 - 并发操作冲突
     */
    @ExceptionHandler(ConcurrencyException.class)
    public Result<String> handleConcurrencyException(ConcurrencyException e, HttpServletRequest request) {
        log.warn("并发异常 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理方法参数校验异常 - @Valid注解校验失败
     */
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
        log.warn("方法参数校验失败 - uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("参数校验失败: " + errorMessage);
    }

    /**
     * 处理约束校验异常 - JPA/Bean验证约束失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        List<String> errors = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);
        log.warn("约束校验失败 - uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("参数校验失败: " + errorMessage);
    }

    /**
     * 处理非法参数异常 - 程序逻辑中的参数错误
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数异常 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.paramError("参数错误: " + e.getMessage());
    }

    /**
     * 处理空指针异常 - 程序逻辑错误
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<String> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 - uri: {}", request.getRequestURI(), e);
        return Result.systemError();
    }

    /**
     * 处理数据库访问异常 - 数据库操作失败
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public Result<String> handleDataAccessException(org.springframework.dao.DataAccessException e, HttpServletRequest request) {
        log.error("数据库异常 - uri: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(ResultCode.DB_ERROR);
    }

    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public Result<String> handleHttpRequestMethodNotSupportedException(org.springframework.web.HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("请求方法不支持 - uri: {}, method: {}, supported: {}", request.getRequestURI(), request.getMethod(), e.getSupportedMethods());
        return Result.error(ResultCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理HTTP媒体类型不支持异常
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public Result<String> handleHttpMediaTypeNotSupportedException(org.springframework.web.HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("请求媒体类型不支持 - uri: {}, contentType: {}", request.getRequestURI(), request.getContentType());
        return Result.error(ResultCode.BAD_REQUEST);
    }

    /**
     * 处理JSON解析异常 - 请求格式错误
     */
    @ExceptionHandler(com.fasterxml.jackson.core.JsonProcessingException.class)
    public Result<String> handleJsonProcessingException(com.fasterxml.jackson.core.JsonProcessingException e, HttpServletRequest request) {
        log.warn("JSON解析失败 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error("请求格式错误，请检查JSON格式");
    }

    /**
     * 处理HTTP消息不可读异常 - 请求体格式错误
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Result<String> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("HTTP消息不可读 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error("请求体格式错误，请检查数据格式");
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public Result<String> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数 - uri: {}, parameter: {}", request.getRequestURI(), e.getParameterName());
        return Result.paramError("缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理绑定异常 - 请求参数绑定失败
     */
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
        log.warn("参数绑定失败 - uri: {}, errors: {}", request.getRequestURI(), errorMessage);
        return Result.paramError("参数绑定失败: " + errorMessage);
    }

    /**
     * 处理OAuth2相关异常 - 认证服务特有
     */
    @ExceptionHandler(OAuth2Exception.class)
    public Result<String> handleOAuth2Exception(OAuth2Exception e, HttpServletRequest request) {
        log.warn("OAuth2异常 - uri: {}, code: {}, message: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理JWT相关异常 - Token验证失败
     */
    @ExceptionHandler(JwtException.class)
    public Result<String> handleJwtException(JwtException e, HttpServletRequest request) {
        log.warn("JWT异常 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理锁相关异常 - 分布式锁获取失败
     */
    @ExceptionHandler(LockException.class)
    public Result<String> handleLockException(LockException e, HttpServletRequest request) {
        log.warn("锁异常 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理通用异常 - 兜底异常处理
     * 注意：这个异常处理器应该放在最后，作为兜底处理
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常 - uri: {}, type: {}", request.getRequestURI(), e.getClass().getSimpleName(), e);
        return Result.systemError();
    }
}