package com.cloud.common.config;

import com.cloud.common.security.JwtAuthorityUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public interface ServiceSecurityCustomizer {

  default boolean isStatelessSession() {
    return false;
  }

  default boolean useBearerTokenHandlers() {
    return false;
  }

  default JwtAuthenticationConverter buildJwtAuthenticationConverter() {
    return JwtAuthorityUtils.buildJwtAuthenticationConverter(true, true, null);
  }

  void configureServiceEndpoints(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          authz);
}
