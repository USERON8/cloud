package com.cloud.gateway.controller;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.result.Result;
import com.cloud.gateway.cache.SearchFallbackCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(
    name = "Gateway Search Fallback",
    description = "Search fallback endpoints for gateway degradation")
public class SearchFallbackController {

  private static final String FALLBACK_METRIC_COUNT = "gateway.search.fallback.count";
  private static final String FALLBACK_METRIC_LATENCY = "gateway.search.fallback.latency";

  private final WebClient.Builder loadBalancedWebClientBuilder;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final SearchFallbackCache searchFallbackCache;
  private final Map<String, Counter> fallbackCounters = new ConcurrentHashMap<>();
  private final Map<String, Timer> fallbackLatencyTimers = new ConcurrentHashMap<>();
  private volatile WebClient productClient;

  @Value("${app.search.fallback.timeout-ms:700}")
  private long fallbackTimeoutMs;

  @Value("${app.search.fallback.product-service-base-url:http://product-service}")
  private String productServiceBaseUrl;

  @Value("${app.search.fallback.routes.search-path:/api/product/search}")
  private String fallbackSearchPath;

  @Value("${app.search.fallback.routes.suggestions-path:/api/product/suggestions}")
  private String fallbackSuggestionsPath;

  @GetMapping("/gateway/fallback/search")
  @Operation(summary = "Search fallback handler")
  public Mono<ResponseEntity<String>> searchFallback(
      ServerWebExchange exchange,
      @RequestParam(value = "route", required = false) String explicitRoute) {
    String originalPath = resolveOriginalPath(exchange);
    MultiValueMap<String, String> queryParams =
        new LinkedMultiValueMap<>(exchange.getRequest().getQueryParams());
    String routeType = resolveRouteType(originalPath, explicitRoute);
    Timer.Sample sample = Timer.start(meterRegistry);

    String cacheKey = buildCacheKey(routeType, queryParams);
    String cachedBody = searchFallbackCache.get(routeType, cacheKey, queryParams);
    if (cachedBody != null) {
      counter(routeType, "cache_hit").increment();
      return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(cachedBody))
          .doFinally(signal -> sample.stop(latencyTimer(routeType)));
    }

    return routeFallback(routeType, queryParams)
        .timeout(Duration.ofMillis(fallbackTimeoutMs))
        .doOnSuccess(
            response -> {
              counter(routeType, "success").increment();
              cacheIfEligible(routeType, cacheKey, response, queryParams);
            })
        .onErrorResume(
            ex -> {
              counter(routeType, "degraded").increment();
              log.warn(
                  "Search fallback degraded: path={}, query={}, reason={}",
                  originalPath,
                  queryParams,
                  ex.getMessage());
              return Mono.just(degradedResponse(routeType, "downstream unavailable"));
            })
        .doFinally(signal -> sample.stop(latencyTimer(routeType)));
  }

  private Mono<ResponseEntity<String>> routeFallback(
      String routeType, MultiValueMap<String, String> queryParams) {
    return switch (routeType) {
      case "suggestions" -> fallbackSuggestions(queryParams);
      case "search", "smart-search" -> fallbackSearch(queryParams);
      default -> Mono.just(errorResponse("Unsupported fallback route"));
    };
  }

  private Mono<ResponseEntity<String>> fallbackSearch(MultiValueMap<String, String> queryParams) {
    String keyword = normalizeKeyword(queryParams.getFirst("keyword"));
    if (StrUtil.isBlank(keyword)) {
      String json = toJson(Result.success("Search fallback applied with empty keyword", List.of()));
      return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json));
    }

    return productClient()
        .get()
        .uri(uriBuilder -> uriBuilder.path(fallbackSearchPath).queryParam("name", keyword).build())
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToMono(
            response ->
                response
                    .toEntity(String.class)
                    .map(
                        entity -> {
                          if (entity.getStatusCode().is2xxSuccessful()
                              && isSuccessResult(entity.getBody())) {
                            return entity;
                          }
                          return degradedResponse("search", "invalid downstream payload");
                        }));
  }

  private Mono<ResponseEntity<String>> fallbackSuggestions(
      MultiValueMap<String, String> queryParams) {
    String keyword = normalizeKeyword(queryParams.getFirst("keyword"));
    int size = parseSize(queryParams.getFirst("size"));
    if (StrUtil.isBlank(keyword)) {
      String json =
          toJson(Result.success("Suggestions fallback applied with empty keyword", List.of()));
      return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json));
    }

    return productClient()
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(fallbackSuggestionsPath)
                    .queryParam("keyword", keyword)
                    .queryParam("size", size)
                    .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToMono(
            response ->
                response
                    .toEntity(String.class)
                    .map(
                        entity -> {
                          if (entity.getStatusCode().is2xxSuccessful()
                              && isSuccessResult(entity.getBody())) {
                            return entity;
                          }
                          return degradedResponse("suggestions", "invalid downstream payload");
                        }));
  }

  private WebClient productClient() {
    WebClient local = productClient;
    if (local != null) {
      return local;
    }
    synchronized (this) {
      if (productClient == null) {
        productClient = loadBalancedWebClientBuilder.baseUrl(productServiceBaseUrl).build();
      }
      return productClient;
    }
  }

  private Counter counter(String routeType, String result) {
    String counterKey = routeType + ':' + result;
    return fallbackCounters.computeIfAbsent(
        counterKey,
        key ->
            Counter.builder(FALLBACK_METRIC_COUNT)
                .description("Gateway fallback count for search routes")
                .tag("route", routeType)
                .tag("result", result)
                .register(meterRegistry));
  }

  private Timer latencyTimer(String routeType) {
    return fallbackLatencyTimers.computeIfAbsent(
        routeType,
        key ->
            Timer.builder(FALLBACK_METRIC_LATENCY)
                .description("Gateway fallback latency for search routes")
                .tag("route", routeType)
                .register(meterRegistry));
  }

  private String resolveOriginalPath(ServerWebExchange exchange) {
    Set<URI> originalUris =
        exchange.getAttributeOrDefault(
            ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR, Collections.emptySet());
    Optional<URI> first = originalUris.stream().findFirst();
    return first.map(URI::getPath).orElse(exchange.getRequest().getPath().value());
  }

  private String resolveRouteType(String originalPath, String explicitRoute) {
    if (StrUtil.isNotBlank(explicitRoute)) {
      String normalized = explicitRoute.trim().toLowerCase();
      if ("suggestions".equals(normalized)
          || "search".equals(normalized)
          || "smart-search".equals(normalized)) {
        return normalized;
      }
    }
    if (originalPath.endsWith("/suggestions")) {
      return "suggestions";
    }
    if (originalPath.endsWith("/smart-search")) {
      return "smart-search";
    }
    if (originalPath.endsWith("/basic")) {
      return "search";
    }
    if (originalPath.endsWith("/search")) {
      return "search";
    }
    return "unknown";
  }

  private int parseSize(String rawSize) {
    if (StrUtil.isBlank(rawSize)) {
      return 10;
    }
    try {
      int parsed = Integer.parseInt(rawSize);
      if (parsed <= 0) {
        return 10;
      }
      return Math.min(parsed, 50);
    } catch (NumberFormatException ignored) {
      return 10;
    }
  }

  private String normalizeKeyword(String keyword) {
    if (StrUtil.isBlank(keyword)) {
      return "";
    }
    return keyword.trim();
  }

  private ResponseEntity<String> errorResponse(String message) {
    String json = toJson(Result.systemError(message));
    return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(json);
  }

  private ResponseEntity<String> degradedResponse(String routeType, String reason) {
    String message = "Fallback degraded for " + routeType + ": " + reason;
    String json = toJson(Result.success(message, List.of()));
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
  }

  private boolean isSuccessResult(String body) {
    if (StrUtil.isBlank(body)) {
      return false;
    }
    try {
      return objectMapper.readTree(body).path("code").asInt(-1) == 200;
    } catch (Exception ex) {
      return false;
    }
  }

  private String toJson(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.error("Serialize fallback payload failed", e);
      return "{\"code\":500,\"message\":\"Serialize fallback payload failed\",\"data\":null}";
    }
  }

  private String buildCacheKey(String routeType, MultiValueMap<String, String> queryParams) {
    if (routeType == null || routeType.isBlank()) {
      return "";
    }
    if (queryParams == null || queryParams.isEmpty()) {
      return routeType;
    }
    StringBuilder builder = new StringBuilder(routeType);
    queryParams.keySet().stream()
        .sorted()
        .forEach(
            key -> {
              List<String> values = queryParams.get(key);
              if (values == null || values.isEmpty()) {
                return;
              }
              values.stream()
                  .sorted()
                  .forEach(
                      value -> {
                        builder.append('|').append(key).append('=').append(value);
                      });
            });
    return builder.toString();
  }

  private void cacheIfEligible(
      String routeType,
      String cacheKey,
      ResponseEntity<String> response,
      MultiValueMap<String, String> queryParams) {
    if (cacheKey == null || cacheKey.isBlank()) {
      return;
    }
    if (response == null || !response.getStatusCode().is2xxSuccessful()) {
      return;
    }
    String body = response.getBody();
    if (!isSuccessResult(body)) {
      return;
    }
    searchFallbackCache.put(routeType, cacheKey, body, queryParams);
  }
}
