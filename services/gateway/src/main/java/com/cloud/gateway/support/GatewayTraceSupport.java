package com.cloud.gateway.support;

import com.cloud.common.trace.TraceIdUtil;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

public final class GatewayTraceSupport {

  public static final String TRACE_ID_ATTRIBUTE = "gateway.traceId";

  private GatewayTraceSupport() {}

  public static String resolveTraceId(ServerWebExchange exchange) {
    if (exchange == null) {
      return TraceIdUtil.generateTraceId();
    }
    Object cached = exchange.getAttribute(TRACE_ID_ATTRIBUTE);
    if (cached instanceof String value && !value.isBlank()) {
      return value;
    }
    String traceId = resolveTraceId(exchange.getRequest());
    exchange.getAttributes().put(TRACE_ID_ATTRIBUTE, traceId);
    return traceId;
  }

  public static String resolveTraceId(ServerHttpRequest request) {
    if (request == null || request.getHeaders() == null) {
      return TraceIdUtil.generateTraceId();
    }
    String traceId = trim(request.getHeaders().getFirst(TraceIdUtil.TRACE_HEADER));
    if (traceId.isBlank()) {
      traceId = trim(request.getHeaders().getFirst(TraceIdUtil.B3_TRACE_HEADER));
    }
    if (traceId.isBlank()) {
      traceId = trim(request.getHeaders().getFirst(TraceIdUtil.LEGACY_TRACE_HEADER));
    }
    return traceId.isBlank() ? TraceIdUtil.generateTraceId() : traceId;
  }

  private static String trim(String value) {
    return TraceIdUtil.normalizeTraceId(value);
  }
}
