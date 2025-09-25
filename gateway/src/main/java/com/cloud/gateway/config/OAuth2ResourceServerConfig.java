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
public class OAuth2ResourceServerConfig {

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
                            // OAuth2.1标准端点 - 完全开放
                            .pathMatchers("/oauth2/**", "/.well-known/**", "/userinfo").permitAll()

                            // 认证服务所有端点 - 完全开放，无需token验证
                            .pathMatchers("/auth/**", "/auth-service/**").permitAll()
                            .pathMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()
                            .pathMatchers("/login/**", "/register/**", "/logout/**").permitAll()

                            // 健康检查和监控端点
                            .pathMatchers("/actuator/**").permitAll()

                            // Knife4j和API文档相关路径 - 完整覆盖
                            .pathMatchers(
                                    "/doc.html", "/swagger-ui.html", "/swagger-ui/**",
                                    "/webjars/**", "/v3/api-docs/**", "/swagger-resources/**",
                                    "/favicon.ico", "/csrf",
                                    // Knife4j 聚合相关路径
                                    "/swagger-resources", "/swagger-resources/configuration/ui",
                                    "/swagger-resources/configuration/security",
                                    // 通过网关访问各服务的文档
                                    "/auth-service/doc.html", "/auth-service/v3/api-docs/**",
                                    "/user-service/doc.html", "/user-service/v3/api-docs/**",
                                    "/auth-service/swagger-ui/**", "/user-service/swagger-ui/**",
                                    "/auth-service/webjars/**", "/user-service/webjars/**"
                            ).permitAll();

                    // 根据配置决定是否开放测试API
                    if (enableTestApi) {
                        log.warn("⚠️ 测试API已开放，生产环境请关闭此配置");
                        authExchanges = authExchanges.pathMatchers("/api/test/**").permitAll();
                    }

                    // 业务API需要认证 - 收紧安全配置
                    authExchanges.pathMatchers("/api/**").authenticated()
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