package com.cloud.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 增强JWT令牌转发过滤器
 * 将认证后的JWT令牌及用户信息转发给下游服务
 * 支持OAuth2.1标准的用户信息传递
 */
@Slf4j
@Component
public class JwtTokenForwardFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(jwtAuth -> {
                    Jwt jwt = jwtAuth.getToken();
                    String token = jwt.getTokenValue();

                    log.debug("🔑 向下游服务转发JWT Token及用户信息");

                    // 构建HTTP请求头，添加用户信息
                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                            .header("Authorization", "Bearer " + token);


                    // 从 JWT Claims 中提取用户信息并添加到请求头
                    addUserInfoHeaders(requestBuilder, jwt);

                    ServerHttpRequest request = requestBuilder.build();

                    log.debug("✅ 成功转发用户 {} 的Token和信息", jwtAuth.getName());
                    return exchange.mutate().request(request).build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    /**
     * 从 JWT Claims 中提取用户信息并添加到请求头
     */
    private void addUserInfoHeaders(ServerHttpRequest.Builder requestBuilder, Jwt jwt) {
        try {
            // 基本用户信息（不包含敏感信息如手机号码）
            addHeaderIfPresent(requestBuilder, "X-User-Name", jwt.getClaimAsString("username"));
            addHeaderIfPresent(requestBuilder, "X-User-Type", jwt.getClaimAsString("user_type"));
            addHeaderIfPresent(requestBuilder, "X-User-Id", getClaimAsString(jwt, "user_id"));
            addHeaderIfPresent(requestBuilder, "X-User-Nickname", jwt.getClaimAsString("nickname"));
            addHeaderIfPresent(requestBuilder, "X-User-Status", getClaimAsString(jwt, "status"));

            // 客户端信息
            addHeaderIfPresent(requestBuilder, "X-Client-Id", jwt.getClaimAsString("client_id"));

            // Token元数据
            addHeaderIfPresent(requestBuilder, "X-Token-Version", jwt.getClaimAsString("token_version"));

            // 权限信息（从 scope 中提取）
            if (jwt.getClaimAsString("scope") != null) {
                addHeaderIfPresent(requestBuilder, "X-User-Scopes", jwt.getClaimAsString("scope"));
            }

            log.debug("📝 已添加用户 {} 的信息到请求头", jwt.getClaimAsString("username"));

        } catch (Exception e) {
            log.warn("⚠️ 提取JWT用户信息时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 安全地添加请求头（仅在值不为空时）
     */
    private void addHeaderIfPresent(ServerHttpRequest.Builder requestBuilder, String headerName, String value) {
        if (StringUtils.hasText(value) && !"null".equals(value)) {
            requestBuilder.header(headerName, value);
        }
    }

    /**
     * 安全地获取 Claim 值为字符串
     */
    private String getClaimAsString(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        return claim != null ? claim.toString() : null;
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级，在路由之前执行
    }
}
