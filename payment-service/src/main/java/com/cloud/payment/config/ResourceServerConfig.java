package com.cloud.payment.config;

import com.cloud.common.config.BaseResourceServerConfig;
import com.cloud.common.security.JwtAuthorityUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@EnableWebSecurity
public class ResourceServerConfig extends BaseResourceServerConfig {

    @Override
    protected void configureServiceEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        authz.requestMatchers("/api/v1/payment/alipay/notify").permitAll()
                .requestMatchers("/internal/payment/**").hasAuthority("SCOPE_internal_api")
                .requestMatchers("/api/payments/**", "/api/v1/payment/alipay/**")
                .hasAnyAuthority("SCOPE_read", "SCOPE_write", "ROLE_USER", "ROLE_ADMIN", "ROLE_MERCHANT");
    }

    @Override
    protected JwtAuthenticationConverter buildJwtAuthenticationConverter() {
        return JwtAuthorityUtils.buildJwtAuthenticationConverter(true, false, null);
    }
}
