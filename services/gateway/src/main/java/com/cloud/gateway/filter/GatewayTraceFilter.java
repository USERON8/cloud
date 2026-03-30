package com.cloud.gateway.filter;

import com.cloud.common.trace.TraceIdUtil;
import com.cloud.gateway.support.GatewayTraceSupport;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayTraceFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String traceId = GatewayTraceSupport.resolveTraceId(exchange);
    ServerHttpRequest mutatedRequest =
        exchange.getRequest().mutate().header(TraceIdUtil.TRACE_HEADER, traceId).build();
    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
    mutatedExchange.getAttributes().put(GatewayTraceSupport.TRACE_ID_ATTRIBUTE, traceId);
    mutatedExchange
        .getResponse()
        .beforeCommit(
            () -> {
              mutatedExchange.getResponse().getHeaders().set(TraceIdUtil.TRACE_HEADER, traceId);
              return Mono.empty();
            });
    return chain.filter(mutatedExchange);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
