package com.cloud.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 网关访问日志过滤器
 */
@Slf4j
@Component
public class GatewayFilter implements WebFilter, Ordered {

    // 专门用于访问日志的Logger
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        String requestPath = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        String clientIp = getClientIp(exchange);

        log.info("网关接收请求: {} {} from {}", method, requestPath, clientIp);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // 记录请求结束时间和耗时
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    // 记录访问日志
                    String accessLog = String.format("[%s] %s %s %s %dms",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            clientIp,
                            method,
                            requestPath,
                            duration
                    );

                    ACCESS_LOG.info(accessLog);
                    log.info("网关处理完成: {} {} 耗时: {}ms", method, requestPath, duration);
                });
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}