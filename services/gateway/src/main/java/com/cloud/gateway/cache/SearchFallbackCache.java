package com.cloud.gateway.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchFallbackCache {

    private final SearchFallbackCacheProperties properties;
    private final Map<Long, Cache<String, String>> cacheByTtl = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("Search fallback cache disabled.");
            return;
        }
        log.info("Search fallback cache enabled: maxSize={}, routeTtlMs={}, paramTtlMs={}, minTtlMs={}",
                properties.getMaxSize(),
                properties.getRouteTtlMs(),
                properties.getParamTtlMs(),
                properties.getMinTtlMs());
    }

    public String get(String routeType, String key, MultiValueMap<String, String> queryParams) {
        if (!properties.isEnabled() || key == null || key.isBlank()) {
            return null;
        }
        long ttlMs = resolveTtlMs(routeType, queryParams);
        if (ttlMs <= 0) {
            return null;
        }
        return cacheFor(ttlMs).getIfPresent(key);
    }

    public void put(String routeType, String key, String value, MultiValueMap<String, String> queryParams) {
        if (!properties.isEnabled() || key == null || key.isBlank() || value == null) {
            return;
        }
        long ttlMs = resolveTtlMs(routeType, queryParams);
        if (ttlMs <= 0) {
            return;
        }
        cacheFor(ttlMs).put(key, value);
    }

    private Cache<String, String> cacheFor(long ttlMs) {
        long safeTtl = clampTtl(ttlMs);
        return cacheByTtl.computeIfAbsent(safeTtl, this::buildCache);
    }

    private long resolveTtlMs(String routeType, MultiValueMap<String, String> queryParams) {
        long ttlMs = resolveRouteTtlMs(routeType);
        Map<String, Long> paramTtl = properties.getParamTtlMs();
        if (paramTtl != null && !paramTtl.isEmpty() && queryParams != null && !queryParams.isEmpty()) {
            for (String key : queryParams.keySet()) {
                Long override = paramTtl.get(key);
                if (override != null && override > 0) {
                    ttlMs = Math.min(ttlMs, override);
                }
            }
        }
        return clampTtl(ttlMs);
    }

    private long resolveRouteTtlMs(String routeType) {
        if (routeType != null && !routeType.isBlank()) {
            Map<String, Long> routeTtl = properties.getRouteTtlMs();
            if (routeTtl != null) {
                Long override = routeTtl.get(routeType);
                if (override != null && override > 0) {
                    return override;
                }
            }
        }
        if ("suggestions".equals(routeType)) {
            return properties.getSuggestionsTtlMs();
        }
        if ("smart-search".equals(routeType)) {
            return properties.getSmartSearchTtlMs();
        }
        return properties.getSearchTtlMs();
    }

    private long clampTtl(long ttlMs) {
        if (ttlMs <= 0) {
            return 0;
        }
        long minTtl = Math.max(100, properties.getMinTtlMs());
        return Math.max(minTtl, ttlMs);
    }

    private Cache<String, String> buildCache(long ttlMs) {
        return Caffeine.newBuilder()
                .maximumSize(Math.max(100, properties.getMaxSize()))
                .expireAfterWrite(Duration.ofMillis(ttlMs))
                .build();
    }
}
