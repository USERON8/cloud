package com.cloud.product.config;

import com.cloud.common.config.BaseResourceServerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class ResourceServerConfig extends BaseResourceServerConfig {
    public ResourceServerConfig(Environment environment) {
        super(environment);
    }

    @Override
    protected void configureServiceEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        authz.requestMatchers("/api/product/search", "/api/product/suggestions").permitAll()
                .requestMatchers("/internal/product/**").hasAuthority("SCOPE_internal_api")
                .requestMatchers("/api/product/**", "/api/category/**").authenticated();
    }
}
