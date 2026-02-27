package com.cloud.user.config;

import com.cloud.common.config.BaseResourceServerConfig;
import com.cloud.common.security.JwtAuthorityUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Locale;

@Configuration
@EnableWebSecurity
public class ResourceServerConfig extends BaseResourceServerConfig {
    public ResourceServerConfig(Environment environment) {
        super(environment);
    }

    @Override
    protected void configureServiceEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        authz.requestMatchers("/internal/user/**").hasAuthority("SCOPE_internal_api")
                .requestMatchers("/admin/**").hasAuthority("SCOPE_internal_api")
                .requestMatchers(
                        "/api/admin/**",
                        "/api/manage/users/**",
                        "/api/query/users/**",
                        "/api/statistics/**",
                        "/api/thread-pool/**"
                )
                .hasRole("ADMIN")
                .requestMatchers("/api/user/profile/**", "/api/user/address/**", "/api/merchant/**")
                .authenticated();
    }

    @Override
    protected JwtAuthenticationConverter buildJwtAuthenticationConverter() {
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
