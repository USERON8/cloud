package com.cloud.search.config;

import com.cloud.common.security.JwtBlacklistTokenValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String jwtIssuer;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/actuator/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-ui.html",
                            "/doc.html",
                            "/webjars/**",
                            "/favicon.ico"
                    ).permitAll();

                    auth.requestMatchers("/internal/**")
                            .hasAuthority("SCOPE_internal_api");

                    auth.requestMatchers("/api/search/rebuild-index")
                            .hasAnyAuthority("ROLE_ADMIN", "SCOPE_admin:write");

                    auth.requestMatchers("/api/search/**", "/api/search/shops/**")
                            .permitAll();

                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )
                .build();
    }

    @Bean
    public OAuth2TokenValidator<Jwt> blacklistTokenValidator(RedisTemplate<String, Object> redisTemplate) {
        return new JwtBlacklistTokenValidator(redisTemplate);
    }

    @Bean
    public JwtDecoder jwtDecoder(OAuth2TokenValidator<Jwt> blacklistTokenValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(jwtIssuer);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, blacklistTokenValidator));

        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roleClaimConverter = new JwtGrantedAuthoritiesConverter();
        roleClaimConverter.setAuthorityPrefix("ROLE_");
        roleClaimConverter.setAuthoritiesClaimName("authorities");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>(extractScopeAuthorities(jwt));

            Collection<GrantedAuthority> roleAuthorities = roleClaimConverter.convert(jwt);
            if (roleAuthorities != null) {
                authorities.addAll(roleAuthorities);
            }

            String userType = jwt.getClaimAsString("user_type");
            if (userType != null && !userType.isBlank()) {
                String normalizedUserType = userType.trim().toUpperCase(Locale.ROOT);
                authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedUserType));
            }
            return authorities;
        });
        return converter;
    }

    private static Set<GrantedAuthority> extractScopeAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        normalizeAndAddScopeAuthorities(authorities, jwt.getClaim("scope"));
        normalizeAndAddScopeAuthorities(authorities, jwt.getClaim("scp"));
        return authorities;
    }

    private static void normalizeAndAddScopeAuthorities(Set<GrantedAuthority> authorities, Object scopeClaim) {
        if (scopeClaim == null) {
            return;
        }
        if (scopeClaim instanceof String scopeString) {
            Arrays.stream(scopeString.split("\\s+"))
                    .map(String::trim)
                    .filter(scope -> !scope.isEmpty())
                    .map(ResourceServerConfig::normalizeScope)
                    .forEach(scope -> addScopeAuthority(authorities, scope));
            return;
        }
        if (scopeClaim instanceof Collection<?> scopes) {
            scopes.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(scope -> !scope.isEmpty())
                    .map(ResourceServerConfig::normalizeScope)
                    .forEach(scope -> addScopeAuthority(authorities, scope));
        }
    }

    private static String normalizeScope(String scope) {
        return scope.replace('.', ':').toLowerCase(Locale.ROOT);
    }

    private static void addScopeAuthority(Set<GrantedAuthority> authorities, String scope) {
        authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
    }
}
