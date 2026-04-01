package com.cloud.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.security.GatewayIdentityHeaders;
import com.cloud.common.security.GatewayIdentitySignatureSupport;
import com.cloud.gateway.support.GatewayTraceSupport;
import java.time.Instant;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtTokenForwardFilter implements GlobalFilter, Ordered {

  private static final String[] FORWARDED_IDENTITY_HEADERS = {
    "Authorization",
    "X-Auth-Token",
    GatewayIdentityHeaders.USERNAME,
    GatewayIdentityHeaders.USER_ID,
    GatewayIdentityHeaders.USER_NICKNAME,
    GatewayIdentityHeaders.USER_STATUS,
    GatewayIdentityHeaders.CLIENT_ID,
    GatewayIdentityHeaders.USER_SCOPES,
    GatewayIdentityHeaders.USER_ROLES,
    GatewayIdentityHeaders.USER_PERMISSIONS,
    GatewayIdentityHeaders.USER_AUTHORITIES,
    GatewayIdentityHeaders.SUBJECT,
    GatewayIdentityHeaders.TRACE_ID,
    GatewayIdentityHeaders.SIGNATURE,
    GatewayIdentityHeaders.TIMESTAMP
  };

  @Value("${app.security.internal-identity.secret:${GATEWAY_INTERNAL_IDENTITY_SECRET:}}")
  private String internalIdentitySecret;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter(authentication -> authentication instanceof JwtAuthenticationToken)
        .cast(JwtAuthenticationToken.class)
        .map(
            jwtAuth -> {
              Jwt jwt = jwtAuth.getToken();

              log.debug("Forward trusted identity headers to downstream services");

              ServerHttpRequest.Builder requestBuilder =
                  exchange
                      .getRequest()
                      .mutate()
                      .headers(
                          headers -> {
                            for (String header : FORWARDED_IDENTITY_HEADERS) {
                              headers.remove(header);
                            }
                          });

              addUserInfoHeaders(requestBuilder, jwt, exchange);

              ServerHttpRequest request = requestBuilder.build();

              log.debug(
                  "Successfully forwarded trusted identity headers for user {}", jwtAuth.getName());
              return exchange.mutate().request(request).build();
            })
        .defaultIfEmpty(exchange)
        .flatMap(chain::filter);
  }

  private void addUserInfoHeaders(
      ServerHttpRequest.Builder requestBuilder, Jwt jwt, ServerWebExchange exchange) {
    try {
      String userId = getUserIdClaim(jwt);
      String username = jwt.getClaimAsString("username");
      String nickname = jwt.getClaimAsString("nickname");
      String status = getClaimAsString(jwt, "status");
      String clientId = jwt.getClaimAsString("client_id");
      String scopes = trim(jwt.getClaimAsString("scope"));
      String roles = joinClaim(jwt.getClaim("roles"));
      String permissions = joinClaim(jwt.getClaim("permissions"));
      String authorities = joinClaim(jwt.getClaim("authorities"));
      String traceId = GatewayTraceSupport.resolveTraceId(exchange);
      String subject = trim(jwt.getSubject());
      String timestamp = String.valueOf(Instant.now().getEpochSecond());

      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.USERNAME, username);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.USER_ID, userId);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.USER_NICKNAME, nickname);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.USER_STATUS, status);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.CLIENT_ID, clientId);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.USER_SCOPES, scopes);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.USER_ROLES, roles);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.USER_PERMISSIONS, permissions);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.USER_AUTHORITIES, authorities);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.TRACE_ID, traceId);
      addHeaderIfPresent(requestBuilder, GatewayIdentityHeaders.SUBJECT, subject);
      requestBuilder.header(GatewayIdentityHeaders.TIMESTAMP, timestamp);
      requestBuilder.header(
          GatewayIdentityHeaders.SIGNATURE,
          GatewayIdentitySignatureSupport.sign(
              GatewayIdentitySignatureSupport.canonicalPayload(
                  userId,
                  username,
                  nickname,
                  status,
                  clientId,
                  scopes,
                  roles,
                  permissions,
                  authorities,
                  traceId,
                  subject,
                  timestamp),
              internalIdentitySecret));

      log.debug("Added user claim headers for user {}", jwt.getClaimAsString("username"));

    } catch (Exception e) {
      log.warn("Failed to extract JWT user claims: {}", e.getMessage());
    }
  }

  private void addHeaderIfPresent(
      ServerHttpRequest.Builder requestBuilder, String headerName, String value) {
    if (StrUtil.isNotBlank(value) && !"null".equals(value)) {
      requestBuilder.header(headerName, value);
    }
  }

  private String getClaimAsString(Jwt jwt, String claimName) {
    Object claim = jwt.getClaim(claimName);
    return claim != null ? claim.toString() : null;
  }

  private String getUserIdClaim(Jwt jwt) {
    String userId = getClaimAsString(jwt, "user_id");
    if (StrUtil.isBlank(userId)) {
      userId = getClaimAsString(jwt, "userId");
    }
    return userId;
  }

  private String joinClaim(Object claim) {
    if (claim instanceof Collection<?> collection && !collection.isEmpty()) {
      return collection.stream()
          .map(Object::toString)
          .collect(java.util.stream.Collectors.joining(" "));
    }
    return claim == null ? "" : claim.toString().trim();
  }

  private String trim(String value) {
    return value == null ? "" : value.trim();
  }

  @Override
  public int getOrder() {
    return -100;
  }
}
