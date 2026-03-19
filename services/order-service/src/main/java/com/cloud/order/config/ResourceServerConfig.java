package com.cloud.order.config;

import com.cloud.common.config.ServiceSecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration(proxyBeanMethods = false)
public class ResourceServerConfig {

  @Bean
  public ServiceSecurityCustomizer serviceSecurityCustomizer() {
    return new ServiceSecurityCustomizer() {
      @Override
      public void configureServiceEndpoints(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
        authz.requestMatchers("/api/**").authenticated();
      }
    };
  }
}
