package com.cloud.payment.config;

import com.cloud.common.config.ServiceSecurityCustomizer;
import com.cloud.common.security.JwtAuthorityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration(proxyBeanMethods = false)
public class ResourceServerConfig {

  @Bean
  public ServiceSecurityCustomizer serviceSecurityCustomizer() {
    return new ServiceSecurityCustomizer() {
      @Override
      public void configureServiceEndpoints(
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
              authz) {
        authz
            .requestMatchers("/api/v1/payment/alipay/notify")
            .permitAll()
            .requestMatchers("/api/payment-checkouts/**")
            .permitAll()
            .requestMatchers("/api/payment-orders/**", "/api/payment-refunds/**")
            .authenticated();
      }

      @Override
      public JwtAuthenticationConverter buildJwtAuthenticationConverter() {
        return JwtAuthorityUtils.buildJwtAuthenticationConverter(true, false, null);
      }
    };
  }
}
