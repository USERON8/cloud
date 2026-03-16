package com.cloud.common.trace;

import org.slf4j.MDC;

public final class TraceIdUtil {

  private TraceIdUtil() {}

  public static String currentTraceId() {
    String traceId = MDC.get("traceId");
    if (traceId == null || traceId.isBlank()) {
      traceId = MDC.get("X-B3-TraceId");
    }
    if (traceId == null || traceId.isBlank()) {
      traceId = MDC.get("trace-id");
    }
    return traceId == null ? "" : traceId;
  }
}
