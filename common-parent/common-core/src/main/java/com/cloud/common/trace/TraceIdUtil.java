package com.cloud.common.trace;

import java.util.UUID;
import org.slf4j.MDC;

public final class TraceIdUtil {

  public static final String TRACE_ID_KEY = "traceId";
  public static final String TRACE_HEADER = "X-Trace-Id";
  public static final String B3_TRACE_HEADER = "X-B3-TraceId";
  public static final String LEGACY_TRACE_HEADER = "traceId";

  private TraceIdUtil() {}

  public static String currentTraceId() {
    String traceId = MDC.get(TRACE_ID_KEY);
    if (traceId == null || traceId.isBlank()) {
      traceId = MDC.get(B3_TRACE_HEADER);
    }
    if (traceId == null || traceId.isBlank()) {
      traceId = MDC.get("trace-id");
    }
    return traceId == null ? "" : traceId;
  }

  public static String normalizeTraceId(String candidate) {
    return candidate == null ? "" : candidate.trim();
  }

  public static String currentOrGenerate() {
    String traceId = currentTraceId();
    return traceId.isBlank() ? generateTraceId() : traceId;
  }

  public static String generateTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
