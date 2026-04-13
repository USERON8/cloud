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
            .requestMatchers(HttpMethod.POST, "/api/app/product/spu")
            .hasAuthority("product:create")
            .requestMatchers(HttpMethod.PUT, "/api/app/product/spu/*")
            .hasAuthority("product:edit")
            .requestMatchers(HttpMethod.PATCH, "/api/app/product/spu/*/status")
            .hasAuthority("product:edit")
            .requestMatchers(HttpMethod.GET, "/api/app/product/**", "/api/app/category/**")
            .hasAuthority("product:view")
            .requestMatchers(HttpMethod.POST, "/api/app/product/**", "/api/app/category/**")
            .hasAuthority("product:create")
            .requestMatchers(HttpMethod.PUT, "/api/app/product/**", "/api/app/category/**")
            .hasAuthority("product:edit")
            .requestMatchers(HttpMethod.PATCH, "/api/app/product/**", "/api/app/category/**")
            .hasAuthority("product:edit")
            .requestMatchers(HttpMethod.DELETE, "/api/app/product/**", "/api/app/category/**")
            .hasAuthority("product:delete")
            .requestMatchers("/api/app/product/**", "/api/app/category/**")
            .authenticated();
      }
    };
  }
}
