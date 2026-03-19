package com.cloud.gateway.config;

import com.cloud.common.security.AudienceTokenValidator;
import com.cloud.common.security.InternalScopeClientValidator;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class ResourceServerConfig {

  private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

  private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
  private final Environment environment;

  @Value(
      "${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:${AUTH_JWK_SET_URI:http://${AUTH_HOST:127.0.0.1}:${AUTH_PORT:8081}/.well-known/jwks.json}}")
  private String jwkSetUri;

  @Value(
      "${spring.security.oauth2.resourceserver.jwt.issuer-uri:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
  private String issuerUri;

  @Value("${app.security.jwt.accepted-audiences:gateway,internal-api}")
  private String acceptedAudiences;

  @Value("${app.security.jwt.internal-client-ids:client-service}")
  private String allowedInternalClientIds;

  @Value("${app.security.enable-test-api:false}")
  private boolean enableTestApi;

  @Value("${app.security.testenv-bypass-enabled:false}")
  private boolean securityTestMode;

  @Value("${app.security.public-actuator-enabled:false}")
  private boolean publicActuatorEnabled;

  @Value("${app.security.api-docs-enabled:false}")
  private boolean apiDocsEnabled;

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    if (securityTestMode) {
      if (isProtectedProfile()) {
        throw new IllegalStateException(
            "app.security.testenv-bypass-enabled cannot be true in protected profiles");
      }
      log.warn(
          "Gateway security test mode is enabled. JWT validation is bypassed and all exchanges are permitAll.");
      http.csrf(ServerHttpSecurity.CsrfSpec::disable)
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
      return http.build();
    }

    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeExchange(
            exchanges -> {
              var authExchanges =
                  exchanges
                      .pathMatchers(HttpMethod.OPTIONS, "/**")
                      .permitAll()
                      .pathMatchers("/auth/**")
                      .permitAll()
                      .pathMatchers("/oauth2/**", "/.well-known/**", "/userinfo")
                      .permitAll()
                      .pathMatchers("/connect/**")
                      .permitAll()
                      .pathMatchers("/oauth2/authorization/**", "/login/oauth2/**")
                      .permitAll()
                      .pathMatchers("/ws/**")
                      .permitAll()
                      .pathMatchers(HttpMethod.POST, "/api/v1/payment/alipay/notify")
                      .permitAll()
                      .pathMatchers(HttpMethod.GET, "/api/v1/payment/alipay/return")
                      .permitAll()
                      .pathMatchers("/auth/oauth2/github/**")
                      .permitAll()
                      .pathMatchers("/login/**")
                      .permitAll()
                      .pathMatchers("/health/**", "/metrics/**")
                      .permitAll()
                      .pathMatchers(
                          "/favicon.ico",
                          "/csrf",
                          "/error",
                          "/gateway/fallback/search",
                          "/static/**",
                          "/public/**")
                      .permitAll()
                      .pathMatchers("/webjars/**")
                      .permitAll();

              if (publicActuatorEnabled) {
                authExchanges = authExchanges.pathMatchers("/actuator/**").permitAll();
              }

              if (apiDocsEnabled) {
                authExchanges =
                    authExchanges
                        .pathMatchers(
                            "/doc.html",
                            "/doc.html/**",
                            "/*/doc.html",
                            "/*/doc.html/**",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/*/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/*/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/*/swagger-resources/**",
                            "/auth-service/doc.html",
                            "/auth-service/doc.html/**",
                            "/user-service/doc.html",
                            "/user-service/doc.html/**",
                            "/product-service/doc.html",
                            "/product-service/doc.html/**",
                            "/order-service/doc.html",
                            "/order-service/doc.html/**",
                            "/payment-service/doc.html",
                            "/payment-service/doc.html/**",
                            "/stock-service/doc.html",
                            "/stock-service/doc.html/**",
                            "/search-service/doc.html",
                            "/search-service/doc.html/**")
                        .permitAll();
              }

              if (enableTestApi) {
                authExchanges = authExchanges.pathMatchers("/test/**").permitAll();
              }

              authExchanges
                  .pathMatchers("/api/product/*/view")
                  .permitAll()
                  .pathMatchers("/api/search/**")
                  .permitAll()
                  .pathMatchers("/api/user/**")
                  .hasRole("USER")
                  .pathMatchers(HttpMethod.POST, "/api/orders")
                  .hasAuthority("order:create")
                  .pathMatchers("/api/orders/**")
                  .hasAuthority("order:query")
                  .pathMatchers(HttpMethod.GET, "/api/product/**")
                  .hasAuthority("product:view")
                  .pathMatchers(HttpMethod.POST, "/api/product/**")
                  .hasAuthority("product:create")
                  .pathMatchers(HttpMethod.PUT, "/api/product/**")
                  .hasAuthority("product:edit")
                  .pathMatchers(HttpMethod.DELETE, "/api/product/**")
                  .hasAuthority("product:delete")
                  .pathMatchers(HttpMethod.GET, "/api/category/**")
                  .hasAuthority("product:view")
                  .pathMatchers(HttpMethod.POST, "/api/category/**")
                  .hasAuthority("product:create")
                  .pathMatchers(HttpMethod.PUT, "/api/category/**")
                  .hasAuthority("product:edit")
                  .pathMatchers(HttpMethod.PATCH, "/api/category/**")
                  .hasAuthority("product:edit")
                  .pathMatchers(HttpMethod.DELETE, "/api/category/**")
                  .hasAuthority("product:delete")
                  .pathMatchers("/api/merchant/manage/**")
                  .hasRole("MERCHANT")
                  .pathMatchers("/api/admin/**")
                  .hasRole("ADMIN")
                  .pathMatchers("/api/merchant/auth/review/**")
                  .hasAuthority("merchant:audit")
                  .pathMatchers("/api/stocks/**", "/api/payments/**")
                  .hasAnyRole("USER", "ADMIN")
                  .anyExchange()
                  .authenticated();
            })
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(
                        jwt ->
                            jwt.jwtDecoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint(
                        (exchange, ex) -> {
                          log.warn("OAuth2 authentication failed: {}", ex.getMessage());
                          exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                          exchange
                              .getResponse()
                              .getHeaders()
                              .add("Content-Type", "application/json;charset=UTF-8");
                          String body =
                              "{\"code\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication failed\",\"timestamp\":"
                                  + System.currentTimeMillis()
                                  + "}";
                          return exchange
                              .getResponse()
                              .writeWith(
                                  Mono.just(
                                      exchange
                                          .getResponse()
                                          .bufferFactory()
                                          .wrap(body.getBytes())));
                        })
                    .accessDeniedHandler(
                        (exchange, ex) -> {
                          log.warn("OAuth2 access denied: {}", ex.getMessage());
                          exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                          exchange
                              .getResponse()
                              .getHeaders()
                              .add("Content-Type", "application/json;charset=UTF-8");
                          String body =
                              "{\"code\":403,\"error\":\"Forbidden\",\"message\":\"Insufficient permissions\",\"timestamp\":"
                                  + System.currentTimeMillis()
                                  + "}";
                          return exchange
                              .getResponse()
                              .writeWith(
                                  Mono.just(
                                      exchange
                                          .getResponse()
                                          .bufferFactory()
                                          .wrap(body.getBytes())));
                        }));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.setAllowedOriginPatterns(
        List.of(
            "http://127.0.0.1:*",
            "https://127.0.0.1:*",
            "http://localhost:*",
            "https://localhost:*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
    OAuth2TokenValidator<Jwt> withAudience =
        new AudienceTokenValidator(parseCsv(acceptedAudiences));
    OAuth2TokenValidator<Jwt> withInternalClient =
        new InternalScopeClientValidator(parseCsv(allowedInternalClientIds));
    decoder.setJwtValidator(
        new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
            withIssuer, withAudience, withInternalClient));

    return token ->
        decoder
            .decode(token)
            .flatMap(
                jwt ->
                    reactiveStringRedisTemplate
                        .hasKey(BLACKLIST_KEY_PREFIX + token)
                        .flatMap(
                            blacklisted -> {
                              if (Boolean.TRUE.equals(blacklisted)) {
                                return Mono.error(new BadJwtException("Token is blacklisted"));
                              }
                              return Mono.just(jwt);
                            })
                        .onErrorResume(
                            ex -> {
                              log.error("Gateway blacklist validation failed", ex);
                              return Mono.error(
                                  new BadJwtException("Gateway blacklist validation unavailable"));
                            }));
  }

  @Bean
  public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
    ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Set<GrantedAuthority> authorities = new LinkedHashSet<>();

          List<String> roles = jwt.getClaimAsStringList("roles");
          if (roles != null) {
            roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(String::trim)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
          }

          List<String> permissions = jwt.getClaimAsStringList("permissions");
          if (permissions != null) {
            permissions.stream()
                .filter(permission -> permission != null && !permission.isBlank())
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
          }

          return Flux.fromIterable(authorities);
        });
    return converter;
  }

  // Token value is used directly as blacklist key suffix to match auth:blacklist:{token} design.

  private boolean isProtectedProfile() {
    for (String profile : environment.getActiveProfiles()) {
      if ("prod".equalsIgnoreCase(profile) || "staging".equalsIgnoreCase(profile)) {
        return true;
      }
    }
    return false;
  }

  private Set<String> parseCsv(String raw) {
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
  }
}
