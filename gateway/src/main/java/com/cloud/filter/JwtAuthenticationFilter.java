package com.cloud.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * OAuth2 JWT认证过滤器，在网关层统一验证token
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class JwtAuthenticationFilter implements WebFilter {

    private static final String TOKEN_PREFIX = "Bearer ";

    // 白名单路径，不需要token验证
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/register-and-login",
            "/auth/register-merchant",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/**",
            "/favicon.ico",
            "/error"
    );

    private final JwtDecoder jwtDecoder;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 检查是否在白名单中
        if (isWhitelisted(path)) {
            log.debug("路径 {} 在白名单中，跳过token验证", path);
            return chain.filter(exchange);
        }

        // 从请求头中获取token
        String token = extractToken(request);
        if (token == null) {
            log.warn("请求路径 {} 缺少认证token", path);
            return handleUnauthorized(exchange);
        }

        // 验证token是否有效
        return validateToken(token)
                .flatMap(jwt -> {
                    log.debug("Token验证通过，路径: {}", path);
                    // 将用户信息传递给下游服务
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("Authorization", TOKEN_PREFIX + token)
                            .header("X-User-ID", jwt.getSubject())
                            .header("X-User-Type", jwt.getClaimAsString("roles"))
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(throwable -> {
                    log.error("Token验证过程中发生错误", throwable);
                    return handleUnauthorized(exchange);
                });
    }

    /**
     * 检查路径是否在白名单中
     *
     * @param path 请求路径
     * @return 是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith) ||
                WHITE_LIST.stream().anyMatch(pattern -> path.matches(pattern.replace("**", ".*").replace("*", "[^/]*")));
    }

    /**
     * 从请求头中提取token
     *
     * @param request 请求对象
     * @return token字符串，如果不存在则返回null
     */
    private String extractToken(ServerHttpRequest request) {
        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            return authorizationHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 验证token是否有效
     *
     * @param token JWT token
     * @return JWT对象
     */
    private Mono<Jwt> validateToken(String token) {
        try {
            // 使用Spring Security OAuth2的JwtDecoder验证并解析JWT令牌
            Jwt jwt = jwtDecoder.decode(token);
            return Mono.just(jwt);
        } catch (JwtValidationException e) {
            log.error("JWT验证失败", e);
            return Mono.error(e);
        } catch (Exception e) {
            log.error("解析JWT时发生错误", e);
            return Mono.error(e);
        }
    }

    /**
     * 处理未授权的请求
     *
     * @param exchange 服务交换对象
     * @return Mono<Void>
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        String responseBody = "{\"code\": 401, \"message\": \"未授权访问\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.getBytes())));
    }
}