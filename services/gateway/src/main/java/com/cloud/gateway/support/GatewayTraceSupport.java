package com.cloud.gateway.support;

import java.util.UUID;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

public final class GatewayTraceSupport {

  private GatewayTraceSupport() {}

  public static String resolveTraceId(ServerWebExchange exchange) {
    return exchange == null ? generateTraceId() : resolveTraceId(exchange.getRequest());
  }

  public static String resolveTraceId(ServerHttpRequest request) {
    if (request == null || request.getHeaders() == null) {
      return generateTraceId();
    }
    String traceId = trim(request.getHeaders().getFirst("X-Trace-Id"));
    if (traceId.isBlank()) {
      traceId = trim(request.getHeaders().getFirst("X-B3-TraceId"));
    }
    if (traceId.isBlank()) {
      traceId = trim(request.getHeaders().getFirst("traceId"));
    }
    return traceId.isBlank() ? generateTraceId() : traceId;
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }

  private static String generateTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
