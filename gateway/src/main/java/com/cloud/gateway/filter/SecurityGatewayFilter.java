package com.cloud.gateway.filter;

import com.cloud.gateway.monitoring.GatewayPerformanceMonitor;
import com.cloud.gateway.security.GatewayRateLimitManager;
import com.cloud.gateway.security.GatewaySecurityAccessManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * 安全网关过滤器
 * 集成IP白名单、限流、Token撤销、性能监控等安全功能
 *
 * @author what's up
 */
@Slf4j
@Component
public class SecurityGatewayFilter extends AbstractGatewayFilterFactory<SecurityGatewayFilter.Config>
        implements Ordered {

    private final GatewaySecurityAccessManager securityAccessManager;
    private final GatewayRateLimitManager rateLimitManager;

    @Autowired(required = false)
    private GatewayPerformanceMonitor performanceMonitor; // 可选的性能监控依赖

    public SecurityGatewayFilter(GatewaySecurityAccessManager securityAccessManager,
                                 GatewayRateLimitManager rateLimitManager) {
        super(Config.class);
        this.securityAccessManager = securityAccessManager;
        this.rateLimitManager = rateLimitManager;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            String clientIp = getClientIp(request);
            String userAgent = request.getHeaders().getFirst("User-Agent");
            String path = request.getPath().pathWithinApplication().value();

            log.debug("安全网关过滤器处理请求: IP={}, Path={}, UserAgent={}", clientIp, path, userAgent);

                // 1. IP访问检查
            if (config.isEnableIpCheck()) {
                GatewaySecurityAccessManager.AccessCheckResult ipCheckResult =
                        securityAccessManager.checkIpAccess(clientIp, userAgent);

                if (!ipCheckResult.isAllowed()) {
                    log.warn("IP访问被拒绝: IP={}, Reason={}", clientIp, ipCheckResult.getReason());
                    return handleSecurityBlock(exchange, HttpStatus.FORBIDDEN, ipCheckResult.getReason());
                }
            }

            // 2. 限流检查
            if (config.isEnableRateLimit()) {
                String rateLimitKey = determineRateLimitKey(path);
                if (rateLimitKey != null) {
                    return rateLimitManager.checkLimit(rateLimitKey, clientIp)
                            .flatMap(rateLimitResult -> {
                                if (!rateLimitResult.isAllowed()) {
                                    log.warn("请求被限流: IP={}, Path={}, Reason={}", clientIp, path, rateLimitResult.getReason());

                                    // 添加限流响应头
                                    response.getHeaders().add("X-RateLimit-Remaining", "0");
                                    response.getHeaders().add("X-RateLimit-Reset", String.valueOf(rateLimitResult.getResetTime()));

                                    return handleSecurityBlock(exchange, HttpStatus.TOO_MANY_REQUESTS, rateLimitResult.getReason());
                                } else {
                                    // 添加限流信息到响应头
                                    response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(rateLimitResult.getRemaining()));
                                    response.getHeaders().add("X-RateLimit-Reset", String.valueOf(rateLimitResult.getResetTime()));
                                    
                                    // 继续处理Token检查或直接处理请求
                                    return performTokenCheckOrProcess(exchange, chain, startTime, path, config, clientIp);
                                }
                            });
                }
            }

            // 继续处理Token检查或直接处理请求
            return performTokenCheckOrProcess(exchange, chain, startTime, path, config, clientIp);
        };
    }

    /**
     * 处理安全阻止响应
     */
    private Mono<Void> handleSecurityBlock(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        String body = String.format("{\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                status.getReasonPhrase(), message, Instant.now().toString());

        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For可能包含多个IP，取第一个
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        String remoteAddr = request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        return remoteAddr;
    }

    /**
     * 执行Token检查或直接处理请求
     */
    private Mono<Void> performTokenCheckOrProcess(ServerWebExchange exchange, GatewayFilterChain chain,
                                                 long startTime, String path, Config config, String clientIp) {
        // 3. Token检查（在有认证信息的情况下）
        if (config.isEnableTokenCheck()) {
            return ReactiveSecurityContextHolder.getContext()
                    .map(securityContext -> securityContext.getAuthentication())
                    .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                    .cast(JwtAuthenticationToken.class)
                    .flatMap(jwtAuth -> {
                        String tokenValue = jwtAuth.getToken().getTokenValue();
                        String userId = jwtAuth.getToken().getClaimAsString("user_id");

                        // 检查Token是否被撤销
                        return securityAccessManager.isTokenRevoked(tokenValue)
                                .flatMap(revoked -> {
                                    if (revoked) {
                                        log.warn("Token已被撤销: userId={}, IP={}", userId, clientIp);
                                        return handleSecurityBlock(exchange, HttpStatus.UNAUTHORIZED, "Token已被撤销");
                                    }
                                    
                                    // 检查用户的所有Token是否被撤销
                                    if (userId != null) {
                                        return securityAccessManager.isUserTokensRevoked(userId)
                                                .flatMap(userRevoked -> {
                                                    if (userRevoked) {
                                                        log.warn("用户所有Token已被撤销: userId={}, IP={}", userId, clientIp);
                                                        return handleSecurityBlock(exchange, HttpStatus.UNAUTHORIZED, "用户Token已被撤销");
                                                    }
                                                    return processRequestWithMonitoring(exchange, chain, startTime, path, config);
                                                });
                                    }
                                    
                                    return processRequestWithMonitoring(exchange, chain, startTime, path, config);
                                });
                    })
                    .switchIfEmpty(processRequestWithMonitoring(exchange, chain, startTime, path, config));
        }

        // 处理请求（不需要Token检查）
        return processRequestWithMonitoring(exchange, chain, startTime, path, config);
    }
    
    /**
     * 处理请求并监控性能
     */
    private Mono<Void> processRequestWithMonitoring(ServerWebExchange exchange, GatewayFilterChain chain, 
                                               long startTime, String path, Config config) {
        return chain.filter(exchange)
                .doOnSuccess(unused -> {
                    // 记录性能指标（成功请求）
                    if (config.isEnablePerformanceMonitoring() && performanceMonitor != null) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        boolean isError = exchange.getResponse().getStatusCode() != null &&
                                exchange.getResponse().getStatusCode().isError();
                        performanceMonitor.recordRequest(path, responseTime, isError);
                    }
                })
                .doOnError(error -> {
                    // 记录性能指标（错误请求）
                    if (config.isEnablePerformanceMonitoring() && performanceMonitor != null) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        performanceMonitor.recordRequest(path, responseTime, true);
                    }
                    log.error("请求处理异常: Path={}", path, error);
                });
    }
    
    /**
     * 确定限流键
     */
    private String determineRateLimitKey(String path) {
        // 根据路径确定使用哪种限流规则
        if (path.startsWith("/auth/login")) {
            return "auth:login";
        } else if (path.startsWith("/auth/register")) {
            return "auth:register";
        } else if (path.contains("/upload")) {
            return "file:upload";
        } else if (path.startsWith("/api/test")) {
            // 为/api/test路径提供专门的限流键
            return "api:test";
        } else if (path.startsWith("/api/")) {
            return "api:access";
        }

        return null; // 不限流
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 最高优先级，最先执行
    }

    @Override
    public String name() {
        return "Security";
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("enableIpCheck", "enableRateLimit", "enableTokenCheck", "enablePerformanceMonitoring");
    }

    /**
     * 配置类
     */
    public static class Config {
        private boolean enableIpCheck = true;
        private boolean enableRateLimit = true;
        private boolean enableTokenCheck = true;
        private boolean enablePerformanceMonitoring = true;

        // Getters and Setters
        public boolean isEnableIpCheck() {
            return enableIpCheck;
        }

        public void setEnableIpCheck(boolean enableIpCheck) {
            this.enableIpCheck = enableIpCheck;
        }

        public boolean isEnableRateLimit() {
            return enableRateLimit;
        }

        public void setEnableRateLimit(boolean enableRateLimit) {
            this.enableRateLimit = enableRateLimit;
        }

        public boolean isEnableTokenCheck() {
            return enableTokenCheck;
        }

        public void setEnableTokenCheck(boolean enableTokenCheck) {
            this.enableTokenCheck = enableTokenCheck;
        }

        public boolean isEnablePerformanceMonitoring() {
            return enablePerformanceMonitoring;
        }

        public void setEnablePerformanceMonitoring(boolean enablePerformanceMonitoring) {
            this.enablePerformanceMonitoring = enablePerformanceMonitoring;
        }
    }
}
