package com.cloud.gateway.controller;

import cn.hutool.core.util.StrUtil;
import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.dto.product.ProductSearchItemDTO;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final SearchFallbackCache searchFallbackCache;
  private final Map<String, Counter> fallbackCounters = new ConcurrentHashMap<>();
  private final Map<String, Timer> fallbackLatencyTimers = new ConcurrentHashMap<>();

  @DubboReference(check = false, timeout = 2000, retries = 0)
  private ProductDubboApi productDubboApi;

  @Value("${app.search.fallback.timeout-ms:700}")
  private long fallbackTimeoutMs;

  @GetMapping("/gateway/fallback/search")
  @Operation(summary = "Search fallback handler")
  public Mono<ResponseEntity<String>> searchFallback(
      ServerWebExchange exchange,
      @RequestParam(value = "route", required = false) String explicitRoute) {
    String originalPath = resolveOriginalPath(exchange);
    MultiValueMap<String, String> rawQueryParams =
        new LinkedMultiValueMap<>(exchange.getRequest().getQueryParams());
    String routeType = resolveRouteType(originalPath, explicitRoute);
    MultiValueMap<String, String> cacheQueryParams =
        normalizeCacheQueryParams(routeType, rawQueryParams);
    Timer.Sample sample = Timer.start(meterRegistry);

    String cacheKey = buildCacheKey(routeType, cacheQueryParams);
    String cachedBody = searchFallbackCache.get(routeType, cacheKey, cacheQueryParams);
    if (cachedBody != null) {
      counter(routeType, "cache_hit").increment();
      return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(cachedBody))
          .doFinally(signal -> sample.stop(latencyTimer(routeType)));
    }

    return routeFallback(routeType, rawQueryParams)
        .timeout(Duration.ofMillis(fallbackTimeoutMs))
        .doOnSuccess(
            response -> {
              counter(routeType, "success").increment();
              cacheIfEligible(routeType, cacheKey, response, cacheQueryParams);
            })
        .onErrorResume(
            ex -> {
              counter(routeType, "degraded").increment();
              log.warn(
                  "Search fallback degraded: path={}, query={}, reason={}",
                  originalPath,
                  rawQueryParams,
                  ex.getMessage());
              return Mono.just(degradedResponse(routeType, "downstream unavailable"));
            })
        .doFinally(signal -> sample.stop(latencyTimer(routeType)));
  }

  private Mono<ResponseEntity<String>> routeFallback(
      String routeType, MultiValueMap<String, String> queryParams) {
    return switch (routeType) {
      case "suggestions" -> fallbackSuggestions(queryParams);
      case "search" -> fallbackSearch(queryParams);
      default -> Mono.just(errorResponse("Unsupported fallback route"));
    };
  }

  private Mono<ResponseEntity<String>> fallbackSearch(MultiValueMap<String, String> queryParams) {
    String keyword = normalizeKeyword(queryParams.getFirst("keyword"));
    int page = parsePage(queryParams.getFirst("page"));
    int size = parseSize(queryParams.getFirst("size"));
    if (StrUtil.isBlank(keyword)) {
      String json =
          toJson(
              Result.success(
                  "Search fallback applied with empty keyword",
                  buildSearchResult(List.<ProductSearchItemDTO>of(), page, size)));
      return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json));
    }
    return Mono.fromSupplier(
        () ->
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    toJson(
                        Result.success(
                            buildSearchResult(
                                productDubboApi.searchProducts(keyword, size), page, size)))));
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
    return Mono.fromSupplier(
        () ->
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(Result.success(productDubboApi.suggestProducts(keyword, size)))));
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
    Optional<String> originalPath =
        originalUris.stream()
            .map(URI::getPath)
            .filter(StrUtil::isNotBlank)
            .filter(path -> !path.startsWith("/gateway/fallback/"))
            .findFirst();
    return originalPath.orElse(exchange.getRequest().getPath().value());
  }

  private String resolveRouteType(String originalPath, String explicitRoute) {
    if (StrUtil.isNotBlank(explicitRoute)) {
      String normalized = explicitRoute.trim().toLowerCase();
      if ("suggestions".equals(normalized) || "search".equals(normalized)) {
        return normalized;
      }
    }
    if (originalPath.endsWith("/suggestions")) {
      return "suggestions";
    }
    if ("/api/search/products".equals(originalPath)) {
      return "search";
    }
    return "unknown";
  }

  private int parsePage(String rawPage) {
    if (StrUtil.isBlank(rawPage) || !StrUtil.isNumeric(rawPage)) {
      return 0;
    }
    return Math.max(Integer.parseInt(rawPage), 0);
  }

  private int parseSize(String rawSize) {
    if (StrUtil.isBlank(rawSize)) {
      return 10;
    }
    if (!StrUtil.isNumeric(rawSize)) {
      return 10;
    }
    int parsed = Integer.parseInt(rawSize);
    if (parsed <= 0) {
      return 10;
    }
    return Math.min(parsed, 50);
  }

  private String normalizeKeyword(String keyword) {
    if (StrUtil.isBlank(keyword)) {
      return "";
    }
    return keyword.trim();
  }

  private Map<String, Object> buildSearchResult(
      List<ProductSearchItemDTO> products, int page, int size) {
    int safeSize = size <= 0 ? 10 : size;
    List<Map<String, Object>> documents =
        products == null ? List.of() : products.stream().map(this::toProductDocument).toList();
    int total = documents.size();
    int totalPages = safeSize <= 0 ? 0 : (int) Math.ceil((double) total / safeSize);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("list", documents);
    payload.put("total", total);
    payload.put("page", page);
    payload.put("size", safeSize);
    payload.put("totalPages", totalPages);
    payload.put("hasNext", page < totalPages - 1);
    payload.put("hasPrevious", page > 0);
    payload.put("aggregations", Map.of());
    payload.put("highlights", Map.of());
    payload.put("searchAfter", List.of());
    return payload;
  }

  private Map<String, Object> toProductDocument(ProductSearchItemDTO item) {
    Map<String, Object> payload = new LinkedHashMap<>();
    if (item == null) {
      return payload;
    }
    payload.put("productId", item.getId());
    payload.put("shopId", item.getShopId());
    payload.put("productName", item.getName());
    payload.put("price", item.getPrice());
    payload.put("stockQuantity", item.getStockQuantity());
    payload.put("categoryId", item.getCategoryId());
    payload.put("brandId", item.getBrandId());
    payload.put("status", item.getStatus());
    payload.put("description", item.getDescription());
    payload.put("imageUrl", item.getImageUrl());
    return payload;
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
    String normalized = body.replaceAll("\\s+", "");
    return normalized.contains("\"code\":200");
  }

  @SneakyThrows
  private String toJson(Object payload) {
    return objectMapper.writeValueAsString(payload);
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

  private MultiValueMap<String, String> normalizeCacheQueryParams(
      String routeType, MultiValueMap<String, String> queryParams) {
    LinkedMultiValueMap<String, String> normalized = new LinkedMultiValueMap<>();
    String keyword = normalizeKeyword(queryParams != null ? queryParams.getFirst("keyword") : null);
    if (StrUtil.isNotBlank(keyword)) {
      normalized.add("keyword", keyword);
    }

    if ("suggestions".equals(routeType)) {
      normalized.add(
          "size",
          String.valueOf(parseSize(queryParams != null ? queryParams.getFirst("size") : null)));
    }
    return normalized;
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
