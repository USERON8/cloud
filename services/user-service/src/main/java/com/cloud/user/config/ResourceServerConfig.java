package com.cloud.user.config;

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
            .requestMatchers(
                "/api/admin/thread-pool/internal/**", "/api/admin/statistics/internal/**")
            .hasAuthority("SCOPE_internal")
            .requestMatchers(
                "/api/admins/**",
                "/api/admin/users/**",
                "/api/admin/statistics/**",
                "/api/admin/thread-pools/**")
            .hasRole("ADMIN")
            .requestMatchers("/api/admin/notifications/**")
            .hasAuthority("admin:all")
            .requestMatchers(
                "/api/users/**",
                "/api/addresses/**",
                "/api/merchants/**",
                "/api/merchant-authentications/**")
            .authenticated();
      }

      @Override
      public JwtAuthenticationConverter buildJwtAuthenticationConverter() {
        return JwtAuthorityUtils.buildJwtAuthenticationConverter(false, true, null);
      }
    };
  }
}
