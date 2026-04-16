package com.cloud.common.config;

import com.cloud.common.exception.GlobalPermissionExceptionHandler;
import com.cloud.common.security.InternalRequestAuthenticationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
  PermissionManager.class,
  GlobalPermissionExceptionHandler.class
})
public class SecurityAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  InternalRequestAuthenticationFilter internalRequestAuthenticationFilter() {
    return new InternalRequestAuthenticationFilter();
  }
}
