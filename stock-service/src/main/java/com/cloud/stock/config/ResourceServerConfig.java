package com.cloud.stock.config;

import com.cloud.common.config.BaseResourceServerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig extends BaseResourceServerConfig {

    @Override
    protected void configureServiceEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        authz.requestMatchers("/internal/stock/**").hasAuthority("SCOPE_internal_api")
                .requestMatchers("/api/stocks/**").authenticated();
    }
}
