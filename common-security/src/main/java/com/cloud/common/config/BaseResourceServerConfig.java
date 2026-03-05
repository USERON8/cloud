package com.cloud.common.config;

import com.cloud.common.security.JwtAuthorityUtils;
import com.cloud.common.security.JwtBlacklistTokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
public abstract class BaseResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:${AUTH_JWK_SET_URI:http://127.0.0.1:8081/.well-known/jwks.json}}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuerUri;

    @Value("${app.security.testenv-bypass-enabled:false}")
    private boolean securityTestMode;

    @Value("${app.security.test-token-value:TEST_ENV_PERMANENT_TOKEN}")
    private String securityTestTokenValue;

    @Value("${app.security.public-actuator-enabled:false}")
    private boolean publicActuatorEnabled;

    @Value("${app.security.cors.allowed-origin-patterns:http://127.0.0.1:*,https://127.0.0.1:*,http://localhost:*,https://localhost:*}")
    private String corsAllowedOriginPatterns;
    private final Environment environment;

    protected BaseResourceServerConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowCredentials(true);
                    for (String pattern : corsAllowedOriginPatterns.split(",")) {
                        String trimmed = pattern == null ? "" : pattern.trim();
                        if (!trimmed.isEmpty()) {
                            config.addAllowedOriginPattern(trimmed);
                        }
                    }
                    config.addAllowedHeader("*");
                    config.addAllowedMethod("*");
                    return config;
                }));

        if (isStatelessSession()) {
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        }

        if (securityTestMode) {
            if (isProtectedProfile()) {
                throw new IllegalStateException("app.security.testenv-bypass-enabled cannot be true in protected profiles");
            }
            log.warn("Security test mode is enabled. JWT validation is bypassed and all endpoints are permitAll.");
            http.addFilterBefore(testAuthenticationBypassFilter(), UsernamePasswordAuthenticationFilter.class);
            http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(authz -> {
            configurePublicEndpoints(authz);
            configureServiceEndpoints(authz);
            authz.anyRequest().authenticated();
        });

        http.oauth2ResourceServer(oauth2 -> {
            oauth2.jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
            );
            if (!useBearerTokenHandlers()) {
                oauth2.authenticationEntryPoint((request, response, authException) -> {
                    log.warn("JWT authentication failed: {}", authException.getMessage());
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"JWT token is invalid or expired\"}");
                });
                oauth2.accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("JWT authorization failed: {}", accessDeniedException.getMessage());
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"access_denied\",\"message\":\"Insufficient permissions\"}");
                });
            }
        });

        if (useBearerTokenHandlers()) {
            http.exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            );
        }

        return http.build();
    }

    protected void configurePublicEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        if (publicActuatorEnabled) {
            authz.requestMatchers("/actuator/**").permitAll();
        }
        authz.requestMatchers("/webjars/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll();
    }

    protected boolean isStatelessSession() {
        return false;
    }

    protected boolean useBearerTokenHandlers() {
        return false;
    }

    protected abstract void configureServiceEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz);

    @Bean
    public OAuth2TokenValidator<Jwt> blacklistTokenValidator(RedisTemplate<String, Object> redisTemplate) {
        return new JwtBlacklistTokenValidator(redisTemplate);
    }

    @Bean
    public JwtDecoder jwtDecoder(OAuth2TokenValidator<Jwt> blacklistTokenValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, blacklistTokenValidator));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return buildJwtAuthenticationConverter();
    }

    protected JwtAuthenticationConverter buildJwtAuthenticationConverter() {
        return JwtAuthorityUtils.buildJwtAuthenticationConverter(true, true, null);
    }

    private boolean isProtectedProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "staging".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private OncePerRequestFilter testAuthenticationBypassFilter() {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_MERCHANT"),
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("SCOPE_internal_api"),
                new SimpleGrantedAuthority("SCOPE_user:read"),
                new SimpleGrantedAuthority("SCOPE_user:write"),
                new SimpleGrantedAuthority("SCOPE_merchant:read"),
                new SimpleGrantedAuthority("SCOPE_merchant:write"),
                new SimpleGrantedAuthority("SCOPE_admin:read"),
                new SimpleGrantedAuthority("SCOPE_admin:write")
        );

        Jwt testJwt = Jwt.withTokenValue(securityTestTokenValue)
                .header("alg", "none")
                .issuer("test-env")
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.parse("2099-12-31T23:59:59Z"))
                .claim("user_id", "999999")
                .claim("username", "test-user")
                .claim("user_type", "ADMIN")
                .claim("scope", "internal_api user:read user:write merchant:read merchant:write admin:read admin:write")
                .build();

        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(testJwt, authorities, "test-user");
                SecurityContextHolder.getContext().setAuthentication(authentication);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }
        };
    }
}

