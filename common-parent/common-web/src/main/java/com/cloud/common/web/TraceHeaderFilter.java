package com.cloud.common.web;

import com.cloud.common.trace.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceHeaderFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = resolveTraceId(request);
    request.setAttribute(TraceIdUtil.TRACE_ID_KEY, traceId);
    MDC.put(TraceIdUtil.TRACE_ID_KEY, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      if (!response.containsHeader(TraceIdUtil.TRACE_HEADER)) {
        response.setHeader(TraceIdUtil.TRACE_HEADER, traceId);
      }
      MDC.remove(TraceIdUtil.TRACE_ID_KEY);
    }
  }

  private String resolveTraceId(HttpServletRequest request) {
    String traceId = TraceIdUtil.normalizeTraceId(request.getHeader(TraceIdUtil.TRACE_HEADER));
    if (traceId.isBlank()) {
      traceId = TraceIdUtil.normalizeTraceId(request.getHeader(TraceIdUtil.B3_TRACE_HEADER));
    }
    if (traceId.isBlank()) {
      Object attribute = request.getAttribute(TraceIdUtil.TRACE_ID_KEY);
      traceId = TraceIdUtil.normalizeTraceId(attribute == null ? null : attribute.toString());
    }
    return traceId.isBlank() ? TraceIdUtil.generateTraceId() : traceId;
  }
}
