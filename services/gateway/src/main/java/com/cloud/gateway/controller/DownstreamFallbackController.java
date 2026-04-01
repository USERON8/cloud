package com.cloud.gateway.controller;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import com.cloud.gateway.support.GatewayTraceSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Hidden
@RestController
@RequiredArgsConstructor
public class DownstreamFallbackController {

  private final ObjectMapper objectMapper;

  @RequestMapping("/gateway/fallback/payment")
  public Mono<ResponseEntity<String>> paymentFallback(ServerWebExchange exchange) {
    String traceId = GatewayTraceSupport.resolveTraceId(exchange);
    log.warn("Payment route fallback triggered: path={}", exchange.getRequest().getPath());
    return Mono.just(
        jsonResponse(
            HttpStatus.OK,
            Result.success("Payment is processing, please check the order status later", Map.of())
                .withTraceId(traceId)));
  }

  @RequestMapping("/gateway/fallback/user")
  public Mono<ResponseEntity<String>> userFallback(ServerWebExchange exchange) {
    String traceId = GatewayTraceSupport.resolveTraceId(exchange);
    HttpMethod method = exchange.getRequest().getMethod();
    log.warn(
        "User route fallback triggered: path={}, method={}",
        exchange.getRequest().getPath(),
        method);
    if (method == null || HttpMethod.GET.equals(method)) {
      Map<String, Object> placeholder =
          Map.of(
              "id", 0,
              "nickname", "User unavailable",
              "username", "unknown",
              "avatarUrl", "",
              "degraded", true);
      return Mono.just(
          jsonResponse(
              HttpStatus.OK,
              Result.success("User service degraded, placeholder returned", placeholder)
                  .withTraceId(traceId)));
    }
    return Mono.just(
        jsonResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            Result.error(
                    ResultCode.REMOTE_SERVICE_UNAVAILABLE,
                    "User service is unavailable, please retry later")
                .withTraceId(traceId)));
  }

  private ResponseEntity<String> jsonResponse(HttpStatus status, Result<?> body) {
    try {
      return ResponseEntity.status(status)
          .contentType(MediaType.APPLICATION_JSON)
          .body(objectMapper.writeValueAsString(body));
    } catch (JsonProcessingException ex) {
      log.error("Serialize gateway fallback response failed", ex);
      return ResponseEntity.status(status)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              "{\"code\":500,\"message\":\"Gateway fallback serialization failed\",\"data\":null}");
    }
  }
}
