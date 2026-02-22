package com.cloud.order.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Hidden
@RestControllerAdvice
public class OrderGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    @ExceptionHandler(OrderServiceException.class)
    public Result<String> handleOrderServiceException(OrderServiceException e, HttpServletRequest request) {
        log.warn("Order service exception: uri={}, type={}, message={}",
                request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

        if (e instanceof OrderServiceException.OrderNotFoundException) {
            return Result.error(ResultCode.ORDER_NOT_FOUND, e.getMessage());
        }
        if (e instanceof OrderServiceException.OrderCreateFailedException) {
            return Result.error(ResultCode.ORDER_CREATE_FAILED, e.getMessage());
        }
        if (e instanceof OrderServiceException.OrderUpdateFailedException) {
            return Result.error(ResultCode.ORDER_UPDATE_FAILED, e.getMessage());
        }
        if (e instanceof OrderServiceException.OrderDeleteFailedException) {
            return Result.error(ResultCode.ORDER_DELETE_FAILED, e.getMessage());
        }
        if (e instanceof OrderServiceException.OrderStatusErrorException) {
            return Result.error(ResultCode.ORDER_STATUS_ERROR, e.getMessage());
        }
        if (e instanceof OrderServiceException.OrderQueryFailedException) {
            return Result.error(ResultCode.ORDER_QUERY_FAILED, e.getMessage());
        }

        return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
    }

    @ExceptionHandler(OrderBusinessException.class)
    public Result<String> handleOrderBusinessException(OrderBusinessException e, HttpServletRequest request) {
        log.warn("Order business exception: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public Result<String> handleInsufficientStockException(InsufficientStockException e, HttpServletRequest request) {
        log.warn("Insufficient stock: uri={}, productId={}, message={}",
                request.getRequestURI(), e.getProductId(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(java.util.ConcurrentModificationException.class)
    public Result<String> handleConcurrentModificationException(java.util.ConcurrentModificationException e,
                                                                HttpServletRequest request) {
        log.warn("Concurrent modification: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.SYSTEM_BUSY, "Concurrent request conflict, please retry");
    }

    @ExceptionHandler(org.springframework.web.client.HttpClientErrorException.class)
    public Result<String> handleHttpClientErrorException(org.springframework.web.client.HttpClientErrorException e,
                                                         HttpServletRequest request) {
        log.warn("Remote client error: uri={}, status={}, message={}",
                request.getRequestURI(), e.getStatusCode(), e.getMessage());

        int status = e.getStatusCode().value();
        if (status == 404) {
            return Result.error(ResultCode.PAYMENT_NOT_FOUND, "Remote resource not found");
        }
        if (status == 400) {
            return Result.error(ResultCode.BAD_REQUEST, "Remote request is invalid");
        }
        return Result.error(ResultCode.SYSTEM_ERROR, "Remote service call failed");
    }

    @ExceptionHandler(org.springframework.web.client.HttpServerErrorException.class)
    public Result<String> handleHttpServerErrorException(org.springframework.web.client.HttpServerErrorException e,
                                                         HttpServletRequest request) {
        log.error("Remote server error: uri={}, status={}, message={}",
                request.getRequestURI(), e.getStatusCode(), e.getMessage(), e);
        return Result.error(ResultCode.SYSTEM_ERROR, "Remote service internal error");
    }
}
