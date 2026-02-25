package com.cloud.order.config;

import com.cloud.common.config.BaseResourceServerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
public class ResourceServerConfig extends BaseResourceServerConfig {

    @Override
    protected void configureServiceEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        authz.requestMatchers("/internal/order/**").hasAuthority("SCOPE_internal_api")
                .requestMatchers("/api/orders/**", "/api/v1/refund/**").authenticated();
    }
}
