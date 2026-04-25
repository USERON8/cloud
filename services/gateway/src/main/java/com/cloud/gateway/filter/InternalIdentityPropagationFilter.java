package com.cloud.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.security.InternalRequestHeaders;
import com.cloud.common.security.InternalRequestSigner;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class InternalIdentityPropagationFilter implements GlobalFilter, Ordered {

  @Value("${app.security.internal-hmac.enabled:true}")
  private boolean enabled;

  @Value("${app.security.internal-hmac.secret:${app.security.signature.secret:}}")
  private String secret;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!enabled) {
      return chain.filter(exchange);
    }
    if (shouldPreserveBearer(exchange)) {
      return chain.filter(exchange);
    }
    return exchange
        .getPrincipal()
        .cast(Authentication.class)
        .flatMap(authentication -> chain.filter(withInternalIdentity(exchange, authentication)))
        .switchIfEmpty(chain.filter(exchange));
  }

  private boolean shouldPreserveBearer(ServerWebExchange exchange) {
    String path = exchange.getRequest().getURI().getPath();
    return path.startsWith("/auth/")
        || path.startsWith("/oauth2/")
        || path.startsWith("/.well-known/")
        || path.equals("/userinfo")
        || path.equals("/connect/logout");
  }

  private ServerWebExchange withInternalIdentity(
      ServerWebExchange exchange, Authentication authentication) {
    if (!authentication.isAuthenticated()
        || !(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return exchange;
    }
    if (StrUtil.isBlank(secret)) {
      log.warn("Skip internal identity propagation because internal HMAC secret is blank");
      return exchange;
    }

    Jwt jwt = jwtAuth.getToken();
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String subject = jwt.getSubject();
    String userId = firstNonBlank(jwt.getClaimAsString("user_id"), jwt.getClaimAsString("userId"));
    String username =
        firstNonBlank(
            jwt.getClaimAsString("preferred_username"),
            jwt.getClaimAsString("username"),
            jwt.getClaimAsString("sub"));
    String clientId =
        firstNonBlank(
            jwt.getClaimAsString("client_id"),
            jwt.getClaimAsString("clientId"),
            jwt.getClaimAsString("azp"));
    String roles = joinCsv(jwt.getClaimAsStringList("roles"));
    String permissions = joinCsv(jwt.getClaimAsStringList("permissions"));
    String scopes = joinCsv(extractScopes(jwt));
    String path = exchange.getRequest().getURI().getPath();
    String signature =
        InternalRequestSigner.sign(
            exchange.getRequest().getMethod() == null
                ? ""
                : exchange.getRequest().getMethod().name(),
            path,
            timestamp,
            subject,
            userId,
            username,
            clientId,
            roles,
            permissions,
            scopes,
            secret);

    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .headers(
                headers -> {
                  headers.remove(HttpHeaders.AUTHORIZATION);
                  headers.set(InternalRequestHeaders.INTERNAL_REQUEST, "true");
                  putIfNotBlank(headers, InternalRequestHeaders.INTERNAL_SUBJECT, subject);
                  putIfNotBlank(headers, InternalRequestHeaders.INTERNAL_USER_ID, userId);
                  putIfNotBlank(headers, InternalRequestHeaders.INTERNAL_USERNAME, username);
                  putIfNotBlank(headers, InternalRequestHeaders.INTERNAL_CLIENT_ID, clientId);
                  putIfNotBlank(headers, InternalRequestHeaders.INTERNAL_ROLES, roles);
                  putIfNotBlank(headers, InternalRequestHeaders.INTERNAL_PERMISSIONS, permissions);
                  putIfNotBlank(headers, InternalRequestHeaders.INTERNAL_SCOPES, scopes);
                  headers.set(InternalRequestHeaders.INTERNAL_TIMESTAMP, timestamp);
                  headers.set(InternalRequestHeaders.INTERNAL_SIGNATURE, signature);
                })
            .build();
    return exchange.mutate().request(request).build();
  }

  private Set<String> extractScopes(Jwt jwt) {
    Set<String> scopes = new LinkedHashSet<>();
    Object scope = jwt.getClaims().get("scope");
    if (scope instanceof String scopeString && StrUtil.isNotBlank(scopeString)) {
      for (String item : scopeString.trim().split("\\s+")) {
        if (StrUtil.isNotBlank(item)) {
          scopes.add(item.trim());
        }
      }
    }
    Object scp = jwt.getClaims().get("scp");
    if (scp instanceof Collection<?> scopeCollection) {
      scopeCollection.stream()
          .map(Object::toString)
          .map(String::trim)
          .filter(StrUtil::isNotBlank)
          .forEach(scopes::add);
    }
    return scopes;
  }

  private String joinCsv(Collection<String> values) {
    if (values == null || values.isEmpty()) {
      return "";
    }
    return values.stream()
        .map(String::trim)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.joining(","));
  }

  private void putIfNotBlank(HttpHeaders headers, String name, String value) {
    if (StrUtil.isNotBlank(value)) {
      headers.set(name, value);
    }
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StrUtil.isNotBlank(value)) {
        return value;
      }
    }
    return null;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 100;
  }
}
