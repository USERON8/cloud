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
                "/api/admin/**",
                "/api/admin/manage/users/**",
                "/api/admin/query/users/**",
                "/api/admin/statistics/**",
                "/api/admin/thread-pool/**")
            .hasRole("ADMIN")
            .requestMatchers("/api/app/user/notification/**")
            .hasAuthority("admin:all")
            .requestMatchers(
                "/api/app/user/profile/**", "/api/app/user/address/**", "/api/app/merchant/**")
            .authenticated();
      }

      @Override
      public JwtAuthenticationConverter buildJwtAuthenticationConverter() {
        return JwtAuthorityUtils.buildJwtAuthenticationConverter(false, true, null);
      }
    };
  }
}
