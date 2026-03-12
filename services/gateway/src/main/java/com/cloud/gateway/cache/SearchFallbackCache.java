package com.cloud.gateway.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class SearchFallbackCache {

    @Value("${app.search.fallback.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.search.fallback.cache.max-size:1000}")
    private long maxSize;

    @Value("${app.search.fallback.cache.search-ttl-ms:3000}")
    private long searchTtlMs;

    @Value("${app.search.fallback.cache.suggestions-ttl-ms:10000}")
    private long suggestionsTtlMs;

    private Cache<String, String> searchCache;
    private Cache<String, String> suggestionsCache;

    @PostConstruct
    public void init() {
        if (!cacheEnabled) {
            log.info("Search fallback cache disabled.");
            return;
        }
        searchCache = buildCache(searchTtlMs);
        suggestionsCache = buildCache(suggestionsTtlMs);
        log.info("Search fallback cache enabled: maxSize={}, searchTtlMs={}, suggestionsTtlMs={}",
                maxSize, searchTtlMs, suggestionsTtlMs);
    }

    public String getSearch(String key) {
        if (!cacheEnabled || searchCache == null) {
            return null;
        }
        return searchCache.getIfPresent(key);
    }

    public void putSearch(String key, String value) {
        if (!cacheEnabled || searchCache == null) {
            return;
        }
        searchCache.put(key, value);
    }

    public String getSuggestions(String key) {
        if (!cacheEnabled || suggestionsCache == null) {
            return null;
        }
        return suggestionsCache.getIfPresent(key);
    }

    public void putSuggestions(String key, String value) {
        if (!cacheEnabled || suggestionsCache == null) {
            return;
        }
        suggestionsCache.put(key, value);
    }

    private Cache<String, String> buildCache(long ttlMs) {
        long safeTtl = Math.max(500, ttlMs);
        return Caffeine.newBuilder()
                .maximumSize(Math.max(100, maxSize))
                .expireAfterWrite(Duration.ofMillis(safeTtl))
                .build();
    }
}
