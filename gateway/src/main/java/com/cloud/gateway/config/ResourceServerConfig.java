package com.cloud.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

    private static final String BLACKLIST_KEY_PREFIX = "oauth2:blacklist:";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuerUri;

    @Value("${app.security.enable-test-api:false}")
    private boolean enableTestApi;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> {
                    var authExchanges = exchanges
                            .pathMatchers("/oauth2/**", "/.well-known/**", "/userinfo").permitAll()
                            .pathMatchers("/connect/**").permitAll()
                            .pathMatchers(HttpMethod.POST,
                                    "/auth/register",
                                    "/auth/login",
                                    "/auth/users/register",
                                    "/auth/sessions",
                                    "/auth/users/register-and-login",
                                    "/auth/tokens/refresh",
                                    "/auth/register-and-login",
                                    "/auth/refresh-token",
                                    "/api/v1/payment/alipay/notify"
                            ).permitAll()
                            .pathMatchers(HttpMethod.GET, "/api/v1/payment/alipay/return").permitAll()
                            .pathMatchers("/auth/oauth2/github/**", "/auth/github/**").permitAll()
                            .pathMatchers("/login/**", "/register/**", "/logout/**").permitAll()
                            .pathMatchers("/actuator/**", "/health/**", "/metrics/**").permitAll()
                            .pathMatchers(
                                    "/doc.html",
                                    "/doc.html/**",
                                    "/*/doc.html",
                                    "/*/doc.html/**"
                            ).permitAll()
                            .pathMatchers(
                                    "/swagger-ui.html",
                                    "/swagger-ui/**",
                                    "/*/swagger-ui/**"
                            ).permitAll()
                            .pathMatchers(
                                    "/v3/api-docs/**",
                                    "/*/v3/api-docs/**",
                                    "/swagger-resources/**",
                                    "/*/swagger-resources/**",
                                    "/webjars/**",
                                    "/*/webjars/**"
                            ).permitAll()
                            .pathMatchers(
                                    "/favicon.ico",
                                    "/csrf",
                                    "/error",
                                    "/static/**",
                                    "/public/**"
                            ).permitAll()
                            .pathMatchers(
                                    "/auth-service/doc.html", "/auth-service/doc.html/**",
                                    "/user-service/doc.html", "/user-service/doc.html/**",
                                    "/product-service/doc.html", "/product-service/doc.html/**",
                                    "/order-service/doc.html", "/order-service/doc.html/**",
                                    "/payment-service/doc.html", "/payment-service/doc.html/**",
                                    "/stock-service/doc.html", "/stock-service/doc.html/**",
                                    "/search-service/doc.html", "/search-service/doc.html/**"
                            ).permitAll();

                    if (enableTestApi) {
                        authExchanges = authExchanges.pathMatchers("/test/**").permitAll();
                    }

                    authExchanges
                            .pathMatchers(
                                    "/users/**", "/merchant/**", "/admin/**",
                                    "/api/manage/users/**", "/api/query/users/**",
                                    "/api/user/address/**", "/api/merchant/**",
                                    "/api/admin/**", "/api/statistics/**", "/api/thread-pool/**"
                            ).authenticated()
                            .pathMatchers("/api/product", "/api/search").authenticated()
                            .pathMatchers("/product/admin/**", "/category/admin/**").authenticated()
                            .pathMatchers("/product/**", "/category/**", "/api/product/**", "/api/category/**").permitAll()
                            .pathMatchers("/order/**", "/cart/**", "/api/orders/**", "/api/v1/refund/**").authenticated()
                            .pathMatchers("/payment/**", "/api/payments/**", "/api/v1/payment/alipay/**").authenticated()
                            .pathMatchers("/stock/**", "/api/stocks/**").authenticated()
                            .pathMatchers("/search/**", "/api/search/**").permitAll()
                            .anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
                        .authenticationEntryPoint((exchange, ex) -> {
                            log.warn("OAuth2 authentication failed: {}", ex.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                            String body = "{\"code\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication failed\",\"timestamp\":" + System.currentTimeMillis() + "}";
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            log.warn("OAuth2 access denied: {}", ex.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                            String body = "{\"code\":403,\"error\":\"Forbidden\",\"message\":\"Insufficient permissions\",\"timestamp\":" + System.currentTimeMillis() + "}";
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
                        })
                );

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(withIssuer);

        return token -> decoder.decode(token)
                .flatMap(jwt -> reactiveStringRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + extractTokenId(jwt, token))
                        .flatMap(blacklisted -> {
                            if (Boolean.TRUE.equals(blacklisted)) {
                                return Mono.error(new BadJwtException("Token is blacklisted"));
                            }
                            return Mono.just(jwt);
                        })
                        .onErrorResume(ex -> {
                            log.error("Gateway blacklist validation failed", ex);
                            return Mono.just(jwt);
                        }));
    }

    private String extractTokenId(Jwt jwt, String tokenValue) {
        String jti = jwt.getClaimAsString("jti");
        if (jti != null && !jti.isBlank()) {
            return jti;
        }
        return String.valueOf(tokenValue.hashCode());
    }
}
