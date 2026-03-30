package com.cloud.gateway.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.gateway.support.GatewayResponseWriter;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackages = "com.cloud.gateway")
@RequiredArgsConstructor
public class GatewayGlobalExceptionHandler {

  private final GatewayResponseWriter gatewayResponseWriter;

  @ExceptionHandler({
    WebExchangeBindException.class,
    BindException.class,
    ConstraintViolationException.class,
    ServerWebInputException.class,
    IllegalArgumentException.class
  })
  public Mono<Void> handleBadRequest(Exception ex, ServerWebExchange exchange) {
    return gatewayResponseWriter.writeError(
        exchange, HttpStatus.BAD_REQUEST, ResultCode.PARAM_ERROR, "Request validation failed");
  }

  @ExceptionHandler(Exception.class)
  public Mono<Void> handleUnknown(Exception ex, ServerWebExchange exchange) {
    return gatewayResponseWriter.writeError(
        exchange,
        HttpStatus.INTERNAL_SERVER_ERROR,
        ResultCode.SYSTEM_ERROR,
        "Gateway request failed");
  }
}
