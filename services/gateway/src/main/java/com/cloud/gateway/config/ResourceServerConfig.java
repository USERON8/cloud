package com.cloud.gateway.config;

import com.cloud.common.security.AudienceTokenValidator;
import com.cloud.common.security.InternalScopeClientValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

    private static final String BLACKLIST_KEY_PREFIX = "oauth2:blacklist:";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final Environment environment;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuerUri;

    @Value("${app.security.jwt.accepted-audiences:gateway,internal-api}")
    private String acceptedAudiences;

    @Value("${app.security.jwt.internal-client-ids:client-service}")
    private String allowedInternalClientIds;

    @Value("${app.security.enable-test-api:false}")
    private boolean enableTestApi;

    @Value("${app.security.testenv-bypass-enabled:false}")
    private boolean securityTestMode;

    @Value("${app.security.public-actuator-enabled:false}")
    private boolean publicActuatorEnabled;

    @Value("${app.security.api-docs-enabled:false}")
    private boolean apiDocsEnabled;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        if (securityTestMode) {
            if (isProtectedProfile()) {
                throw new IllegalStateException("app.security.testenv-bypass-enabled cannot be true in protected profiles");
            }
            log.warn("Gateway security test mode is enabled. JWT validation is bypassed and all exchanges are permitAll.");
            http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
            return http.build();
        }

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> {
                    var authExchanges = exchanges
                            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .pathMatchers("/oauth2/**", "/.well-known/**", "/userinfo").permitAll()
                            .pathMatchers("/connect/**").permitAll()
                            .pathMatchers("/oauth2/authorization/**", "/login/oauth2/**").permitAll()
                            .pathMatchers(HttpMethod.POST,
                                    "/auth/users/register",
                                    "/api/v1/payment/alipay/notify"
                            ).permitAll()
                            .pathMatchers(HttpMethod.DELETE, "/auth/sessions").permitAll()
                            .pathMatchers(HttpMethod.GET, "/api/v1/payment/alipay/return").permitAll()
                            .pathMatchers("/auth/oauth2/github/**").permitAll()
                            .pathMatchers("/login/**").permitAll()
                            .pathMatchers("/health/**", "/metrics/**").permitAll()
                            .pathMatchers(
                                    "/favicon.ico",
                                    "/csrf",
                                    "/error",
                                    "/gateway/fallback/search",
                                    "/static/**",
                                    "/public/**"
                            ).permitAll()
                            .pathMatchers("/webjars/**").permitAll();

                    if (publicActuatorEnabled) {
                        authExchanges = authExchanges.pathMatchers("/actuator/**").permitAll();
                    }

                    if (apiDocsEnabled) {
                        authExchanges = authExchanges
                                .pathMatchers(
                                        "/doc.html",
                                        "/doc.html/**",
                                        "/*/doc.html",
                                        "/*/doc.html/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/*/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/*/v3/api-docs/**",
                                        "/swagger-resources/**",
                                        "/*/swagger-resources/**",
                                        "/auth-service/doc.html", "/auth-service/doc.html/**",
                                        "/user-service/doc.html", "/user-service/doc.html/**",
                                        "/product-service/doc.html", "/product-service/doc.html/**",
                                        "/order-service/doc.html", "/order-service/doc.html/**",
                                        "/payment-service/doc.html", "/payment-service/doc.html/**",
                                        "/stock-service/doc.html", "/stock-service/doc.html/**",
                                        "/search-service/doc.html", "/search-service/doc.html/**"
                                ).permitAll();
                    }

                    if (enableTestApi) {
                        authExchanges = authExchanges.pathMatchers("/test/**").permitAll();
                    }

                    authExchanges
                            .pathMatchers("/api/product/**", "/api/category/**").permitAll()
                            .pathMatchers("/api/search/**").permitAll()
                            .pathMatchers("/api/**").authenticated()
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of(
                "http://127.0.0.1:*",
                "https://127.0.0.1:*",
                "http://localhost:*",
                "https://localhost:*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new AudienceTokenValidator(parseCsv(acceptedAudiences));
        OAuth2TokenValidator<Jwt> withInternalClient = new InternalScopeClientValidator(parseCsv(allowedInternalClientIds));
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                withIssuer,
                withAudience,
                withInternalClient
        ));

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
                            return Mono.error(new BadJwtException("Gateway blacklist validation unavailable"));
                        }));
    }

    private String extractTokenId(Jwt jwt, String tokenValue) {
        String jti = jwt.getClaimAsString("jti");
        if (jti != null && !jti.isBlank()) {
            return jti;
        }
        return sha256Hex(tokenValue);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private boolean isProtectedProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "staging".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}

