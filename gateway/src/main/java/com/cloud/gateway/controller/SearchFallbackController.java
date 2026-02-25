package com.cloud.gateway.controller;

import com.cloud.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SearchFallbackController {

    private static final String FALLBACK_METRIC_COUNT = "gateway.search.fallback.count";
    private static final String FALLBACK_METRIC_LATENCY = "gateway.search.fallback.latency";

    private final WebClient.Builder loadBalancedWebClientBuilder;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${app.search.fallback.timeout-ms:700}")
    private long fallbackTimeoutMs;

    @GetMapping("/gateway/fallback/search")
    public Mono<ResponseEntity<String>> searchFallback(ServerWebExchange exchange) {
        String originalPath = resolveOriginalPath(exchange);
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(exchange.getRequest().getQueryParams());
        String routeType = resolveRouteType(originalPath);
        Timer.Sample sample = Timer.start(meterRegistry);

        return routeFallback(routeType, queryParams)
                .timeout(Duration.ofMillis(fallbackTimeoutMs))
                .doOnSuccess(response -> counter(routeType, "success").increment())
                .onErrorResume(ex -> {
                    counter(routeType, "error").increment();
                    log.error("Search fallback failed: path={}, query={}", originalPath, queryParams, ex);
                    return Mono.just(errorResponse("Search fallback failed"));
                })
                .doFinally(signal -> sample.stop(Timer.builder(FALLBACK_METRIC_LATENCY)
                        .description("Gateway fallback latency for search routes")
                        .tag("route", routeType)
                        .register(meterRegistry)));
    }

    private Mono<ResponseEntity<String>> routeFallback(String routeType, MultiValueMap<String, String> queryParams) {
        return switch (routeType) {
            case "suggestions" -> fallbackSuggestions(queryParams);
            case "search", "smart-search" -> fallbackSearch(queryParams);
            default -> Mono.just(errorResponse("Unsupported fallback route"));
        };
    }

    private Mono<ResponseEntity<String>> fallbackSearch(MultiValueMap<String, String> queryParams) {
        String keyword = normalizeKeyword(queryParams.getFirst("keyword"));
        if (!StringUtils.hasText(keyword)) {
            String json = toJson(Result.success("Search fallback applied with empty keyword", List.of()));
            return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json));
        }

        return productClient().get()
                .uri(uriBuilder -> uriBuilder.path("/api/product/search")
                        .queryParam("name", keyword)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class);
    }

    private Mono<ResponseEntity<String>> fallbackSuggestions(MultiValueMap<String, String> queryParams) {
        String keyword = normalizeKeyword(queryParams.getFirst("keyword"));
        int size = parseSize(queryParams.getFirst("size"));
        if (!StringUtils.hasText(keyword)) {
            String json = toJson(Result.success("Suggestions fallback applied with empty keyword", List.of()));
            return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json));
        }

        return productClient().get()
                .uri(uriBuilder -> uriBuilder.path("/api/product/suggestions")
                        .queryParam("keyword", keyword)
                        .queryParam("size", size)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class);
    }

    private WebClient productClient() {
        return loadBalancedWebClientBuilder.baseUrl("http://product-service").build();
    }

    private Counter counter(String routeType, String result) {
        return Counter.builder(FALLBACK_METRIC_COUNT)
                .description("Gateway fallback count for search routes")
                .tag("route", routeType)
                .tag("result", result)
                .register(meterRegistry);
    }

    private String resolveOriginalPath(ServerWebExchange exchange) {
        Set<URI> originalUris = exchange.getAttributeOrDefault(
                ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR,
                Collections.emptySet()
        );
        Optional<URI> first = originalUris.stream().findFirst();
        return first.map(URI::getPath).orElse(exchange.getRequest().getPath().value());
    }

    private String resolveRouteType(String originalPath) {
        if (originalPath.endsWith("/suggestions")) {
            return "suggestions";
        }
        if (originalPath.endsWith("/smart-search")) {
            return "smart-search";
        }
        if (originalPath.endsWith("/search")) {
            return "search";
        }
        return "unknown";
    }

    private int parseSize(String rawSize) {
        if (!StringUtils.hasText(rawSize)) {
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
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.trim();
    }

    private ResponseEntity<String> errorResponse(String message) {
        String json = toJson(Result.systemError(message));
        return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(json);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Serialize fallback payload failed", e);
            return "{\"code\":500,\"message\":\"Serialize fallback payload failed\",\"data\":null}";
        }
    }
}
