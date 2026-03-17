package com.cloud.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@AutoConfiguration
@ConditionalOnClass(EnableMethodSecurity.class)
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@Import({PermissionConfig.class, PermissionChecker.class})
public class SecurityAutoConfiguration {

  @Bean("securityExpressions")
  @ConditionalOnProperty(
      name = "app.security.expressions.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public UnifiedSecurityExpressions securityExpressions() {
    return new UnifiedSecurityExpressions();
  }
}

