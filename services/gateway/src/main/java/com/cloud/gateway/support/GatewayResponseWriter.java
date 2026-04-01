package com.cloud.gateway.support;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayResponseWriter {

  private final ObjectMapper objectMapper;

  public Mono<Void> writeError(
      ServerWebExchange exchange, HttpStatus status, ResultCode code, String message) {
    if (exchange == null || exchange.getResponse() == null) {
      return Mono.empty();
    }
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    response.getHeaders().set("X-Trace-Id", GatewayTraceSupport.resolveTraceId(exchange));

    Result<?> body =
        Result.error(code, message).withTraceId(GatewayTraceSupport.resolveTraceId(exchange));
    byte[] payload = serialize(body, status, message);
    return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
  }

  private byte[] serialize(Result<?> body, HttpStatus status, String message) {
    try {
      return objectMapper.writeValueAsBytes(body);
    } catch (JsonProcessingException ex) {
      log.error("Serialize gateway response failed: status={}, message={}", status, message, ex);
      String fallback =
          "{\"code\":" + status.value() + ",\"message\":\"" + escape(message) + "\",\"data\":null}";
      return fallback.getBytes(StandardCharsets.UTF_8);
    }
  }

  private String escape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
