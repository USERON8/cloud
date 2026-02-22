package com.cloud.product.exception;

import com.cloud.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Object> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("Access denied: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error("FORBIDDEN", "Access denied");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Object> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("Duplicate key: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error("BUSINESS_ERROR", "Duplicate data exists");
    }

    @ExceptionHandler(ProductServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleProductServiceException(ProductServiceException e, HttpServletRequest request) {
        log.warn("Product service exception: uri={}, message={}", request.getRequestURI(), e.getMessage());
        if (e instanceof ProductServiceException.ProductNotFoundException) {
            return Result.error("NOT_FOUND", e.getMessage());
        }
        if (e instanceof ProductServiceException.ProductPermissionException) {
            return Result.error("FORBIDDEN", e.getMessage());
        }
        return Result.error("BUSINESS_ERROR", e.getMessage());
    }

    @ExceptionHandler(CategoryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleCategoryException(CategoryException e, HttpServletRequest request) {
        log.warn("Category exception: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error("BUSINESS_ERROR", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error("BUSINESS_ERROR", "Data integrity violation");
    }
}
