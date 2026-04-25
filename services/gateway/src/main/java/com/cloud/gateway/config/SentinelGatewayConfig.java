package com.cloud.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.result.Result;
import com.cloud.gateway.support.GatewayTraceSupport;
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

  @Value("${app.sentinel.gateway.user-qps:20}")
  private double userQps;

  @Value("${app.sentinel.gateway.user-interval-sec:1}")
  private int userIntervalSec;

  @Value("${app.sentinel.gateway.search-route-ids:search-service-public}")
  private String searchRouteIds;

  @Value(
      "${app.sentinel.gateway.route-ids:auth-service-api,user-service-app,user-service-admin,product-service-app,order-service-app,payment-service-app,stock-service-admin,search-service-public}")
  private String routeIds;

  @PostConstruct
  public void initGatewayRules() {
    if (!sentinelGatewayEnabled) {
      log.info("Sentinel gateway rules disabled.");
      return;
    }
    GatewayApiDefinitionManager.loadApiDefinitions(buildApiDefinitions());
    Set<String> searchRouteSet = parseRouteIds(searchRouteIds);
    Set<GatewayFlowRule> rules =
        parseRouteIds(routeIds).stream()
            .map(routeId -> buildRule(routeId, searchRouteSet))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    rules.addAll(buildUserApiRules());
    GatewayRuleManager.loadRules(rules);
    GatewayCallbackManager.setBlockHandler((exchange, throwable) -> buildBlockedResponse(exchange));
    log.info(
        "Sentinel gateway rules loaded: ruleCount={}, defaultQps={}, intervalSec={}, userQps={}, userIntervalSec={}",
        rules.size(),
        defaultQps,
        intervalSec,
        userQps,
        userIntervalSec);
  }

  private Set<String> parseRouteIds(String value) {
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(routeId -> !routeId.isEmpty())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private GatewayFlowRule buildRule(String routeId, Set<String> searchRouteSet) {
    if (searchRouteSet.contains(routeId)) {
      return new GatewayFlowRule(routeId).setCount(searchQps).setIntervalSec(searchIntervalSec);
    }
    return new GatewayFlowRule(routeId).setCount(defaultQps).setIntervalSec(intervalSec);
  }

  private Set<ApiDefinition> buildApiDefinitions() {
    Set<ApiDefinition> definitions = new LinkedHashSet<>();
    definitions.add(api("/auth"));
    definitions.add(
        api("/api/users", "/api/addresses", "/api/merchants", "/api/merchant-authentications"));
    definitions.add(api("/api/products", "/api/categories", "/api/spus", "/api/skus"));
    definitions.add(api("/api/orders", "/api/after-sales", "/api/users/me/cart"));
    definitions.add(api("/api/payment-orders", "/api/payment-refunds", "/api/payment-checkouts"));
    definitions.add(api("/api/admin", "/api/admins"));
    definitions.add(api("/api/search", "/api/shops"));
    return definitions;
  }

  private Set<GatewayFlowRule> buildUserApiRules() {
    return buildApiDefinitions().stream()
        .map(ApiDefinition::getApiName)
        .map(
            apiName ->
                new GatewayFlowRule(apiName)
                    .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                    .setCount(userQps)
                    .setIntervalSec(userIntervalSec)
                    .setParamItem(
                        new GatewayParamFlowItem()
                            .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
                            .setFieldName("X-User-Id")))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private ApiDefinition api(String... patterns) {
    Set<com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem> items =
        Arrays.stream(patterns)
            .map(
                pattern ->
                    new ApiPathPredicateItem()
                        .setPattern(pattern)
                        .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    return new ApiDefinition(patterns[0]).setPredicateItems(items);
  }

  private Mono<ServerResponse> buildBlockedResponse(ServerWebExchange exchange) {
    String traceId = GatewayTraceSupport.resolveTraceId(exchange);
    String payload;
    try {
      payload =
          objectMapper.writeValueAsString(
              Result.error(ResultCode.RATE_LIMITED, "Request rate limited").withTraceId(traceId));
    } catch (JsonProcessingException ex) {
      log.error("Serialize sentinel block response failed", ex);
      payload = "{\"code\":429,\"message\":\"Request rate limited\",\"data\":null}";
    }
    return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Trace-Id", traceId)
        .bodyValue(payload);
  }
}
