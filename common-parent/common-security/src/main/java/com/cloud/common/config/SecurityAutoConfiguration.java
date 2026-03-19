package com.cloud.common.config;

import com.cloud.common.exception.GlobalPermissionExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@AutoConfiguration
@ConditionalOnClass(EnableMethodSecurity.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@EnableConfigurationProperties(PermissionConfig.class)
@Import({
  BaseResourceServerConfig.class,
  PermissionChecker.class,
  PermissionManager.class,
  GlobalPermissionExceptionHandler.class
})
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
