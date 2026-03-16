package com.cloud.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SentinelGatewayConfig {

  private final ObjectMapper objectMapper;

  @Value("${app.sentinel.gateway.enabled:true}")
  private boolean sentinelGatewayEnabled;

  @Value("${app.sentinel.gateway.default-qps:80}")
  private double defaultQps;

  @Value("${app.sentinel.gateway.interval-sec:1}")
  private int intervalSec;

  @Value("${app.sentinel.gateway.search-qps:30}")
  private double searchQps;

  @Value("${app.sentinel.gateway.search-interval-sec:1}")
  private int searchIntervalSec;

  @Value("${app.sentinel.gateway.search-route-ids:search-service-api-v2}")
  private String searchRouteIds;

  @Value(
      "${app.sentinel.gateway.route-ids:user-service-api-v2,product-service-api-v2,order-service-api-v2,payment-service-api-v2,stock-service-api-v2,search-service-api-v2}")
  private String routeIds;

  @PostConstruct
  public void initGatewayRules() {
    if (!sentinelGatewayEnabled) {
      log.info("Sentinel gateway rules disabled.");
      return;
    }
    Set<String> searchRouteSet = parseRouteIds(searchRouteIds);
    Set<GatewayFlowRule> rules =
        parseRouteIds(routeIds).stream()
            .map(routeId -> buildRule(routeId, searchRouteSet))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    GatewayRuleManager.loadRules(rules);
    GatewayCallbackManager.setBlockHandler((exchange, t) -> buildBlockedResponse(exchange, t));
    log.info(
        "Sentinel gateway rules loaded: routeCount={}, defaultQps={}, intervalSec={}",
        rules.size(),
        defaultQps,
        intervalSec);
  }

  private Set<String> parseRouteIds(String value) {
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(v -> !v.isEmpty())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private GatewayFlowRule buildRule(String routeId, Set<String> searchRouteSet) {
    if (searchRouteSet.contains(routeId)) {
      return new GatewayFlowRule(routeId).setCount(searchQps).setIntervalSec(searchIntervalSec);
    }
    return new GatewayFlowRule(routeId).setCount(defaultQps).setIntervalSec(intervalSec);
  }

  private Mono<ServerResponse> buildBlockedResponse(
      ServerWebExchange exchange, Throwable throwable) {
    String traceId = resolveTraceId(exchange);
    String payload;
    try {
      payload =
          objectMapper.writeValueAsString(
              Result.error(ResultCode.RATE_LIMITED, "请求过于频繁").withTraceId(traceId));
    } catch (JsonProcessingException e) {
      log.error("Serialize sentinel block response failed", e);
      payload = "{\"code\":429,\"message\":\"sentinel blocked\",\"data\":null}";
    }
    return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Trace-Id", traceId == null ? "" : traceId)
        .bodyValue(payload);
  }

  private String resolveTraceId(ServerWebExchange exchange) {
    if (exchange == null) {
      return "";
    }
    ServerHttpRequest request = exchange.getRequest();
    if (request == null || request.getHeaders() == null) {
      return "";
    }
    String traceId = request.getHeaders().getFirst("X-Trace-Id");
    if (traceId == null || traceId.isBlank()) {
      traceId = request.getHeaders().getFirst("X-B3-TraceId");
    }
    if (traceId == null || traceId.isBlank()) {
      traceId = request.getHeaders().getFirst("traceId");
    }
    return traceId == null ? "" : traceId;
  }
}
