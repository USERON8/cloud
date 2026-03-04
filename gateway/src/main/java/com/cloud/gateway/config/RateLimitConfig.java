package com.cloud.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean("ipPathKeyResolver")
    public KeyResolver ipPathKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String ip = request.getRemoteAddress() == null
                    ? "unknown"
                    : request.getRemoteAddress().getAddress().getHostAddress();
            String method = request.getMethod() == null ? "UNKNOWN" : request.getMethod().name();
            String path = request.getPath().pathWithinApplication().value();
            String routeBucket = extractRouteBucket(path);
            return Mono.just(ip + ":" + method + ":" + routeBucket);
        };
    }

    private String extractRouteBucket(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int idx = normalized.indexOf('/');
        return idx < 0 ? normalized : normalized.substring(0, idx);
    }
}
