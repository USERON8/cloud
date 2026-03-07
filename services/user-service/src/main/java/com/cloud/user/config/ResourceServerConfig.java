package com.cloud.user.config;

import com.cloud.common.config.BaseResourceServerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import com.cloud.common.security.JwtAuthorityUtils;

@Configuration
@EnableWebSecurity
public class ResourceServerConfig extends BaseResourceServerConfig {
    public ResourceServerConfig(Environment environment) {
        super(environment);
    }

    @Override
    protected void configureServiceEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        authz.requestMatchers("/admin/**").hasAuthority("SCOPE_internal_api")
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
        return JwtAuthorityUtils.buildJwtAuthenticationConverter(false, true, null);
    }
}
