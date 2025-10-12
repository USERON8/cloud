package com.cloud.order.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 订单服务全局异常处理器
 * 继承公共异常处理器，只处理订单服务特有的异常
 * 其他常见异常由父类 GlobalExceptionHandler 统一处理
 *
 * 核心原则：
 * 1. Controller层不允许抛异常，所有异常由全局异常处理器统一处理
 * 2. Service层抛出特定异常，由全局异常处理器转换为标准Result格式
 * 3. 所有异常响应统一使用Result包装，不使用ResponseEntity
 *
 * @author cloud
 */
@Slf4j
@Hidden
@RestControllerAdvice
public class OrderGlobalExceptionHandler extends com.cloud.common.exception.GlobalExceptionHandler {

    /**
     * 处理订单服务特定异常 - OrderServiceException及其子类
     */
    @ExceptionHandler(OrderServiceException.class)
    public Result<String> handleOrderServiceException(OrderServiceException e, HttpServletRequest request) {
        log.warn("订单服务异常 - uri: {}, type: {}, message: {}", request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

        // 根据具体的异常类型返回对应的错误码
        if (e instanceof OrderServiceException.OrderNotFoundException) {
            return Result.error(ResultCode.ORDER_NOT_FOUND, e.getMessage());
        } else if (e instanceof OrderServiceException.OrderCreateFailedException) {
            return Result.error(ResultCode.ORDER_CREATE_FAILED, e.getMessage());
        } else if (e instanceof OrderServiceException.OrderUpdateFailedException) {
            return Result.error(ResultCode.ORDER_UPDATE_FAILED, e.getMessage());
        } else if (e instanceof OrderServiceException.OrderDeleteFailedException) {
            return Result.error(ResultCode.ORDER_DELETE_FAILED, e.getMessage());
        } else if (e instanceof OrderServiceException.OrderStatusErrorException) {
            return Result.error(ResultCode.ORDER_STATUS_ERROR, e.getMessage());
        } else if (e instanceof OrderServiceException.OrderQueryFailedException) {
            return Result.error(ResultCode.ORDER_QUERY_FAILED, e.getMessage());
        } else if (e instanceof OrderServiceException.OrderPaymentFailedException) {
            return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
        } else if (e instanceof OrderServiceException.OrderShippingFailedException) {
            return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
        }

        // 默认处理
        return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
    }

    /**
     * 处理订单业务异常 - OrderBusinessException
     */
    @ExceptionHandler(OrderBusinessException.class)
    public Result<String> handleOrderBusinessException(OrderBusinessException e, HttpServletRequest request) {
        log.warn("订单业务异常 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理库存不足异常 - 库存服务调用失败
     */
    @ExceptionHandler(InsufficientStockException.class)
    public Result<String> handleInsufficientStockException(InsufficientStockException e, HttpServletRequest request) {
        log.warn("库存不足异常 - uri: {}, productId: {}, message: {}", request.getRequestURI(), e.getProductId(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理并发控制异常
     */
    @ExceptionHandler(java.util.ConcurrentModificationException.class)
    public Result<String> handleConcurrentModificationException(java.util.ConcurrentModificationException e, HttpServletRequest request) {
        log.warn("并发修改异常 - uri: {}, message: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.SYSTEM_BUSY, "系统繁忙，请稍后再试");
    }

    /**
     * 处理支付服务调用异常
     */
    @ExceptionHandler(org.springframework.web.client.HttpClientErrorException.class)
    public Result<String> handleHttpClientErrorException(org.springframework.web.client.HttpClientErrorException e, HttpServletRequest request) {
        log.warn("HTTP客户端异常 - uri: {}, statusCode: {}, message: {}", request.getRequestURI(), e.getStatusCode(), e.getMessage());

        if (e.getStatusCode().value() == 404) {
            return Result.error(ResultCode.PAYMENT_NOT_FOUND, "支付服务不可用");
        } else if (e.getStatusCode().value() == 400) {
            return Result.error(ResultCode.BAD_REQUEST, "支付请求参数错误");
        }

        return Result.error(ResultCode.SYSTEM_ERROR, "支付服务调用失败");
    }

    /**
     * 处理库存服务调用异常
     */
    @ExceptionHandler(org.springframework.web.client.HttpServerErrorException.class)
    public Result<String> handleHttpServerErrorException(org.springframework.web.client.HttpServerErrorException e, HttpServletRequest request) {
        log.error("HTTP服务端异常 - uri: {}, statusCode: {}, message: {}", request.getRequestURI(), e.getStatusCode(), e.getMessage(), e);
        return Result.error(ResultCode.SYSTEM_ERROR, "依赖服务调用失败");
    }

  }