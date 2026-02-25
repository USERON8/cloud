package com.cloud.user.config;

import com.cloud.common.security.JwtAuthorityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Locale;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

    private final TokenBlacklistChecker tokenBlacklistChecker;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:${AUTH_JWK_SET_URI:http://127.0.0.1:8081/.well-known/jwks.json}}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuerUri;

    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowCredentials(true);
                    config.addAllowedOriginPattern("*");
                    config.addAllowedHeader("*");
                    config.addAllowedMethod("*");
                    return config;
                }))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()
                        .requestMatchers("/internal/user/**").hasAuthority("SCOPE_internal_api")
                        .requestMatchers("/admin/**").hasAuthority("SCOPE_internal_api")
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/manage/users/**",
                                "/api/query/users/**",
                                "/api/statistics/**",
                                "/api/thread-pool/**"
                        ).hasRole("ADMIN")
                        .requestMatchers("/api/user/profile/**", "/api/user/address/**", "/api/merchant/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("JWT authentication failed: {}", authException.getMessage());
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"unauthorized\",\"message\":\"JWT token is invalid or expired\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("JWT authorization failed: {}", accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"access_denied\",\"message\":\"Insufficient permissions\"}"
                            );
                        })
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, tokenBlacklistChecker));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return JwtAuthorityUtils.buildJwtAuthenticationConverter(false, true, (authorities, jwt) -> {
            String userType = jwt.getClaimAsString("user_type");
            if (userType == null || userType.isBlank()) {
                return;
            }
            String normalizedUserType = userType.trim().toUpperCase(Locale.ROOT);
            switch (normalizedUserType) {
                case "ADMIN" -> {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_admin:read"));
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_admin:write"));
                }
                case "MERCHANT" -> {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_merchant:read"));
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_merchant:write"));
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_user:read"));
                }
                case "USER" -> {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_user:read"));
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_user:write"));
                }
                default -> {
                }
            }
        });
    }
}
