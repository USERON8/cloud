package com.cloud.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
                        .requestMatchers("/internal/user/**").permitAll()
                        .requestMatchers("/admin/**").hasAnyAuthority(
                                "SCOPE_internal_api",
                                "SCOPE_admin",
                                "SCOPE_admin.read",
                                "SCOPE_admin.write",
                                "SCOPE_admin:read",
                                "SCOPE_admin:write"
                        )
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
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        scopeConverter.setAuthorityPrefix("SCOPE_");
        scopeConverter.setAuthoritiesClaimName("scope");

        JwtGrantedAuthoritiesConverter roleClaimConverter = new JwtGrantedAuthoritiesConverter();
        roleClaimConverter.setAuthorityPrefix("ROLE_");
        roleClaimConverter.setAuthoritiesClaimName("authorities");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>();

            Collection<GrantedAuthority> scopeAuthorities = scopeConverter.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }

            Collection<GrantedAuthority> roleAuthorities = roleClaimConverter.convert(jwt);
            if (roleAuthorities != null) {
                authorities.addAll(roleAuthorities);
            }

            String userType = jwt.getClaimAsString("user_type");
            if (userType != null && !userType.isBlank()) {
                String normalizedUserType = userType.trim().toUpperCase(Locale.ROOT);
                authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedUserType));

                switch (normalizedUserType) {
                    case "ADMIN" -> {
                        addScopeAuthority(authorities, "admin");
                        addScopeAuthority(authorities, "admin.read");
                        addScopeAuthority(authorities, "admin.write");
                        addScopeAuthority(authorities, "admin:read");
                        addScopeAuthority(authorities, "admin:write");
                    }
                    case "MERCHANT" -> {
                        addScopeAuthority(authorities, "merchant");
                        addScopeAuthority(authorities, "merchant.read");
                        addScopeAuthority(authorities, "merchant.write");
                        addScopeAuthority(authorities, "merchant:read");
                        addScopeAuthority(authorities, "merchant:write");
                        addScopeAuthority(authorities, "user.read");
                    }
                    case "USER" -> {
                        addScopeAuthority(authorities, "user");
                        addScopeAuthority(authorities, "user.read");
                        addScopeAuthority(authorities, "user.write");
                        addScopeAuthority(authorities, "user:read");
                        addScopeAuthority(authorities, "user:write");
                    }
                    default -> {
                    }
                }
            }

            return authorities;
        });

        return converter;
    }

    private static void addScopeAuthority(Set<GrantedAuthority> authorities, String scope) {
        authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
    }
}
