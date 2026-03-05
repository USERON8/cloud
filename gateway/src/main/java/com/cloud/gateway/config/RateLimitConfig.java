package com.cloud.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean("ipPathKeyResolver")
    @Primary
    public KeyResolver ipPathKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String ip = resolveIp(request);
            String method = request.getMethod() == null ? "UNKNOWN" : request.getMethod().name();
            String path = request.getPath().pathWithinApplication().value();
            String routeBucket = extractRouteBucket(path);
            return Mono.just(ip + ":" + method + ":" + routeBucket);
        };
    }

    @Bean("userIpPathKeyResolver")
    public KeyResolver userIpPathKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String principal = request.getHeaders().getFirst("X-User-Id");
            if (!StringUtils.hasText(principal)) {
                principal = resolveIp(request);
            }
            String method = request.getMethod() == null ? "UNKNOWN" : request.getMethod().name();
            String path = request.getPath().pathWithinApplication().value();
            String routeBucket = extractRouteBucket(path);
            return Mono.just(principal + ":" + method + ":" + routeBucket);
        };
    }

    private String resolveIp(ServerHttpRequest request) {
        if (request.getRemoteAddress() == null || request.getRemoteAddress().getAddress() == null) {
            return "unknown";
        }
        return request.getRemoteAddress().getAddress().getHostAddress();
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
