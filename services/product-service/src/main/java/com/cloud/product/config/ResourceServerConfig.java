package com.cloud.product.config;

import com.cloud.common.config.ServiceSecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
        authz
            .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**")
            .hasAuthority("product:view")
            .requestMatchers(HttpMethod.GET, "/api/spus", "/api/spus/**")
            .hasAuthority("product:view")
            .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**", "/api/skus")
            .hasAuthority("product:view")
            .requestMatchers(HttpMethod.POST, "/api/spus", "/api/spus/**")
            .hasAuthority("product:create")
            .requestMatchers(HttpMethod.PUT, "/api/spus", "/api/spus/**")
            .hasAuthority("product:edit")
            .requestMatchers(HttpMethod.PATCH, "/api/spus", "/api/spus/**")
            .hasAuthority("product:edit")
            .requestMatchers(HttpMethod.DELETE, "/api/spus", "/api/spus/**")
            .hasAuthority("product:delete")
            .requestMatchers(HttpMethod.POST, "/api/categories", "/api/categories/**")
            .hasAuthority("product:create")
            .requestMatchers(HttpMethod.PUT, "/api/categories", "/api/categories/**")
            .hasAuthority("product:edit")
            .requestMatchers(HttpMethod.PATCH, "/api/categories", "/api/categories/**")
            .hasAuthority("product:edit")
            .requestMatchers(HttpMethod.DELETE, "/api/categories", "/api/categories/**")
            .hasAuthority("product:delete")
            .requestMatchers("/api/products/**", "/api/spus/**", "/api/categories/**", "/api/skus")
            .authenticated();
      }
    };
  }
}
