package com.cloud.payment.config;

import com.cloud.common.config.PermissionChecker;
import com.cloud.common.config.PermissionConfig;
import com.cloud.common.config.UnifiedSecurityExpressions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@Import({PermissionConfig.class, PermissionChecker.class})
public class SecurityConfig {

    @Bean("securityExpressions")
    @ConditionalOnProperty(name = "app.security.expressions.enabled", havingValue = "true", matchIfMissing = true)
    public UnifiedSecurityExpressions securityExpressions() {
        return new UnifiedSecurityExpressions();
    }
}
