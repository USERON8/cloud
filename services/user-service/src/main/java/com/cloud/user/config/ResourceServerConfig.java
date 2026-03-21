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
                "/api/manage/users/**",
                "/api/query/users/**",
                "/api/statistics/**",
                "/api/thread-pool/**")
            .hasRole("ADMIN")
            .requestMatchers("/api/user/notification/**")
            .hasAuthority("admin:all")
            .requestMatchers("/api/user/profile/**", "/api/user/address/**", "/api/merchant/**")
            .authenticated();
      }

      @Override
      public JwtAuthenticationConverter buildJwtAuthenticationConverter() {
        return JwtAuthorityUtils.buildJwtAuthenticationConverter(false, true, null);
      }
    };
  }
}
