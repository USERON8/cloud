package com.cloud.product.config;

import com.cloud.common.security.JwtBlacklistTokenValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;

@Slf4j
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:${AUTH_JWK_SET_URI:http://127.0.0.1:8081/.well-known/jwks.json}}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuerUri;

    @Bean
    @Order(100)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
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
                        .requestMatchers("/product/internal/**").hasAuthority("SCOPE_internal_api")
                        .requestMatchers("/product/manage/**").hasAnyAuthority("SCOPE_write", "ROLE_ADMIN")
                        .requestMatchers("/product/query/**").authenticated()
                        .requestMatchers("/api/product/**").hasAnyAuthority("SCOPE_read", "SCOPE_write", "ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/product/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("JWT authentication failed: {}", authException.getMessage());
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"JWT token is invalid or expired\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("JWT authorization failed: {}", accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"access_denied\",\"message\":\"Insufficient permissions\"}");
                        })
                );

        return http.build();
    }

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
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        scopeConverter.setAuthorityPrefix("SCOPE_");
        scopeConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new LinkedHashSet<>(scopeConverter.convert(jwt));
            String userType = jwt.getClaimAsString("user_type");
            if (userType != null && !userType.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase(Locale.ROOT)));
            }
            return authorities;
        });
        return converter;
    }
}
