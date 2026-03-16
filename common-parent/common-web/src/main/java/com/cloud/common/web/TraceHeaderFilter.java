package com.cloud.common.web;

import com.cloud.common.trace.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceHeaderFilter extends OncePerRequestFilter {

  private static final String TRACE_HEADER = "X-Trace-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!response.containsHeader(TRACE_HEADER)) {
      String traceId = TraceIdUtil.currentTraceId();
      if (traceId != null && !traceId.isBlank()) {
        response.setHeader(TRACE_HEADER, traceId);
      }
    }
    filterChain.doFilter(request, response);
  }
}
