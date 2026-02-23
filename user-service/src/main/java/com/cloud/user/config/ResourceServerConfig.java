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
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;







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

                        // Internal Feign endpoints are exposed under /internal/user/**.
                        // Keep them accessible for intra-service calls where no end-user JWT exists.
                        .requestMatchers("/internal/user/**").permitAll()

                        
                        .requestMatchers("/admin/manage/**")
                        .hasAuthority("SCOPE_admin")

                        
                        .requestMatchers("/user/manage/**")
                        .hasAuthority("SCOPE_admin")

                        
                        .requestMatchers("/api/user/profile/**")
                        .hasAnyAuthority("SCOPE_user", "SCOPE_admin")

                        
                        .requestMatchers("/api/user/address/**")
                        .hasAnyAuthority("SCOPE_user", "SCOPE_admin")

                        
                        .requestMatchers("/api/user/merchant/**")
                        .hasAnyAuthority("SCOPE_merchant", "SCOPE_admin")

                        
                        .requestMatchers("/example/permissions/**").authenticated()

                        
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("馃敀 JWT璁よ瘉澶辫触: {}", authException.getMessage());
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"unauthorized\",\"message\":\"JWT浠ょ墝鏃犳晥鎴栧凡杩囨湡\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("馃毇 JWT鎺堟潈澶辫触: {}", accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"access_denied\",\"message\":\"鏉冮檺涓嶈冻\"}"
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
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }
}


