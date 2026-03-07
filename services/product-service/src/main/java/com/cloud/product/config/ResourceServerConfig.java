package com.cloud.product.config;

import com.cloud.common.config.BaseResourceServerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
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
                .requestMatchers(HttpMethod.POST, "/api/product/spu")
                .hasAnyAuthority("SCOPE_admin:write", "SCOPE_merchant:write")
                .requestMatchers(HttpMethod.PUT, "/api/product/spu/*")
                .hasAnyAuthority("SCOPE_admin:write", "SCOPE_merchant:write")
                .requestMatchers(HttpMethod.PATCH, "/api/product/spu/*/status")
                .hasAnyAuthority("SCOPE_admin:write", "SCOPE_merchant:write")
                .requestMatchers("/api/product/**", "/api/category/**").authenticated();
    }
}
