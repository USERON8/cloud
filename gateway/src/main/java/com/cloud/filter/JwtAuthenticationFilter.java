package com.cloud.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JWT认证过滤器，在网关层统一验证token
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class JwtAuthenticationFilter implements WebFilter {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String TOKEN_REDIS_PREFIX = "token:";

    // 白名单路径，不需要token验证
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/register-and-login",
            "/v3/api-docs",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/**"
    );
    private final StringRedisTemplate stringRedisTemplate;
    @Value("${jwt.secret:mySecretKey1234567890123456}")
    private String jwtSecret;
    // JWT签名算法
    private Algorithm algorithm;

    public JwtAuthenticationFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 初始化JWT签名算法
    private Algorithm getAlgorithm() {
        if (algorithm == null) {
            algorithm = Algorithm.HMAC256(jwtSecret);
        }
        return algorithm;
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
                .flatMap(isValid -> {
                    if (isValid) {
                        log.debug("Token验证通过，路径: {}", path);
                        // 从JWT中提取用户信息
                        DecodedJWT decodedJWT = decodeJwt(token);
                        if (decodedJWT != null) {
                            // 将用户信息传递给下游服务
                            ServerHttpRequest mutatedRequest = request.mutate()
                                    .header("Authorization", TOKEN_PREFIX + token)
                                    .header("X-User-ID", decodedJWT.getClaim("userId").asString())
                                    .header("X-User-Type", decodedJWT.getClaim("userType").asString())
                                    .build();
                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        } else {
                            log.warn("无法解析JWT中的用户信息，路径: {}", path);
                            return handleUnauthorized(exchange);
                        }
                    } else {
                        log.warn("Token验证失败，路径: {}", path);
                        return handleUnauthorized(exchange);
                    }
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
     * @return 是否有效
     */
    private Mono<Boolean> validateToken(String token) {
        try {
            // 首先验证JWT格式和签名
            DecodedJWT decodedJWT = decodeJwt(token);
            if (decodedJWT == null) {
                return Mono.just(false);
            }

            // 检查token是否过期
            Date expiration = decodedJWT.getExpiresAt();
            if (expiration != null && expiration.before(new Date())) {
                log.debug("Token已过期");
                return Mono.just(false);
            }

            // 检查Redis中是否存在该token
            String key = TOKEN_REDIS_PREFIX + token;
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            if (hasKey != null && hasKey) {
                // 延长token有效期
                stringRedisTemplate.expire(key, 3600, TimeUnit.SECONDS);
                return Mono.just(true);
            }
            return Mono.just(false);
        } catch (Exception e) {
            log.error("验证token时发生错误", e);
            return Mono.just(false);
        }
    }

    /**
     * 解析JWT
     *
     * @param token JWT token
     * @return DecodedJWT对象，如果解析失败返回null
     */
    private DecodedJWT decodeJwt(String token) {
        try {
            // 使用JWTVerifier验证并解析JWT令牌
            JWTVerifier verifier = JWT.require(getAlgorithm()).build();
            return verifier.verify(token);
        } catch (Exception e) {
            log.error("解析JWT时发生错误", e);
            return null;
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