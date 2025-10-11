package com.cloud.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * OAuth2资源服务器配置
 * 提供JWT token验证和权限配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.security.enable-test-api:false}")
    private boolean enableTestApi;

    /**
     * 配置安全过滤器链
     *
     * @param http ServerHttpSecurity对象
     * @return SecurityWebFilterChain 安全过滤器链
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        log.info("🔧 配置网关安全过滤器链，测试API开放状态: {}", enableTestApi);

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> {
                    var authExchanges = exchanges
                            // ========== OAuth2.1标准端点 - 完全开放 ==========
                            .pathMatchers("/oauth2/**", "/.well-known/**", "/userinfo").permitAll()
                            .pathMatchers("/connect/**").permitAll()  // OpenID Connect端点

                            // ========== 认证服务公开API - 无需token ==========
                            // 认证服务端点
                            .pathMatchers(
                                    "/auth/register",
                                    "/auth/login",
                                    "/auth/logout",
                                    "/auth/register-and-login",
                                    "/auth/refresh-token",
                                    "/auth/github/**",
                                    "/auth/**"  // 所有auth服务端点公开（可根据需要调整）
                            ).permitAll()

                            // 服务前缀路径（兼容性）
                            .pathMatchers("/auth-service/**").permitAll()

                            // 通用认证路径
                            .pathMatchers("/login/**", "/register/**", "/logout/**").permitAll()

                            // ========== 健康检查和监控 ==========
                            .pathMatchers("/actuator/**", "/health/**", "/metrics/**").permitAll()

                            // ========== Knife4j和API文档 - 完全开放 ==========
                            // Knife4j核心路径
                            .pathMatchers(
                                    "/doc.html",
                                    "/doc.html/**",
                                    "/**/doc.html",  // 匹配所有服务的doc.html
                                    "/**/doc.html/**"
                            ).permitAll()

                            // Swagger UI
                            .pathMatchers(
                                    "/swagger-ui.html",
                                    "/swagger-ui/**",
                                    "/**/swagger-ui/**"
                            ).permitAll()

                            // API文档资源
                            .pathMatchers(
                                    "/v3/api-docs/**",
                                    "/**/v3/api-docs/**",
                                    "/swagger-resources/**",
                                    "/**/swagger-resources/**",
                                    "/webjars/**",
                                    "/**/webjars/**"
                            ).permitAll()

                            // 静态资源
                            .pathMatchers(
                                    "/favicon.ico",
                                    "/csrf",
                                    "/error",
                                    "/static/**",
                                    "/public/**"
                            ).permitAll()

                            // 各微服务的文档端点（明确列出）
                            .pathMatchers(
                                    "/auth-service/doc.html", "/auth-service/doc.html/**",
                                    "/user-service/doc.html", "/user-service/doc.html/**",
                                    "/product-service/doc.html", "/product-service/doc.html/**",
                                    "/order-service/doc.html", "/order-service/doc.html/**",
                                    "/payment-service/doc.html", "/payment-service/doc.html/**",
                                    "/stock-service/doc.html", "/stock-service/doc.html/**",
                                    "/search-service/doc.html", "/search-service/doc.html/**",
                                    "/log-service/doc.html", "/log-service/doc.html/**"
                            ).permitAll();

                    // 根据配置决定是否开放测试API
                    if (enableTestApi) {
                        log.warn("⚠️ 测试API已开放，生产环境请关闭此配置");
                        authExchanges = authExchanges.pathMatchers("/test/**").permitAll();
                    }

                    // 需要认证的业务端点
                    authExchanges
                            // 用户服务 - 需要认证
                            .pathMatchers("/users/**", "/merchant/**", "/admin/**").authenticated()
                            // 商品服务 - 部分公开（浏览），部分需要认证（管理）
                            .pathMatchers("/product/admin/**", "/category/admin/**").authenticated()
                            .pathMatchers("/product/**", "/category/**").permitAll()  // 商品浏览公开
                            // 订单服务 - 需要认证
                            .pathMatchers("/order/**", "/cart/**").authenticated()
                            // 支付服务 - 需要认证
                            .pathMatchers("/payment/**").authenticated()
                            // 库存服务 - 需要认证
                            .pathMatchers("/stock/**").authenticated()
                            // 搜索服务 - 公开
                            .pathMatchers("/search/**").permitAll()
                            // 日志服务 - 需要认证
                            .pathMatchers("/log/**").authenticated()
                            // 其他所有请求都需要认证
                            .anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder())
                        )
                        .authenticationEntryPoint((exchange, ex) -> {
                            log.warn("OAuth2认证失败: {}", ex.getMessage());
                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                            exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                            String jsonResponse = "{\"code\":401,\"error\":\"Unauthorized\",\"message\":\"认证失败，请提供有效的JWT Token\",\"timestamp\":" + System.currentTimeMillis() + "}";
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                                    .bufferFactory().wrap(jsonResponse.getBytes())));
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            log.warn("OAuth2权限不足: {}", ex.getMessage());
                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                            exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                            String jsonResponse = "{\"code\":403,\"error\":\"Forbidden\",\"message\":\"权限不足，无法访问该资源\",\"timestamp\":" + System.currentTimeMillis() + "}";
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                                    .bufferFactory().wrap(jsonResponse.getBytes())));
                        })
                );

        return http.build();
    }

    /**
     * JWT解码器
     * 使用认证服务的JWK端点进行JWT验证
     *
     * @return ReactiveJwtDecoder JWT解码器实例
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        log.info("配置JWT解码器，JWK端点: {}", jwkSetUri);
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}