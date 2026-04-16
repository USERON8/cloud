package com.cloud.common.security;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class InternalRequestAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  @Value("${app.security.internal-hmac.enabled:true}")
  private boolean enabled;

  @Value("${app.security.internal-hmac.secret:${app.security.signature.secret:}}")
  private String secret;

  @Value("${app.security.internal-hmac.timestamp-skew-seconds:60}")
  private long timestampSkewSeconds;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!enabled || hasBearerAuthorization(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    String marker = request.getHeader(InternalRequestHeaders.INTERNAL_REQUEST);
    if (!"true".equalsIgnoreCase(marker)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (StrUtil.isBlank(secret)) {
      log.error("Internal request authentication is enabled but secret is blank");
      reject(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal auth unavailable");
      return;
    }

    String timestamp = request.getHeader(InternalRequestHeaders.INTERNAL_TIMESTAMP);
    String signature = request.getHeader(InternalRequestHeaders.INTERNAL_SIGNATURE);
    if (StrUtil.hasBlank(timestamp, signature)) {
      reject(response, HttpServletResponse.SC_UNAUTHORIZED, "internal auth headers missing");
      return;
    }

    long requestTimestamp;
    try {
      requestTimestamp = Long.parseLong(timestamp);
    } catch (NumberFormatException ex) {
      reject(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid internal auth timestamp");
      return;
    }

    long now = Instant.now().getEpochSecond();
    if (Math.abs(now - requestTimestamp) > Math.max(5L, timestampSkewSeconds)) {
      reject(response, HttpServletResponse.SC_UNAUTHORIZED, "internal auth timestamp expired");
      return;
    }

    String subject = request.getHeader(InternalRequestHeaders.INTERNAL_SUBJECT);
    String userId = request.getHeader(InternalRequestHeaders.INTERNAL_USER_ID);
    String username = request.getHeader(InternalRequestHeaders.INTERNAL_USERNAME);
    String clientId = request.getHeader(InternalRequestHeaders.INTERNAL_CLIENT_ID);
    String rolesHeader = request.getHeader(InternalRequestHeaders.INTERNAL_ROLES);
    String permissionsHeader = request.getHeader(InternalRequestHeaders.INTERNAL_PERMISSIONS);
    String scopesHeader = request.getHeader(InternalRequestHeaders.INTERNAL_SCOPES);

    String expected =
        InternalRequestSigner.sign(
            request.getMethod(),
            request.getRequestURI(),
            timestamp,
            subject,
            userId,
            username,
            clientId,
            rolesHeader,
            permissionsHeader,
            scopesHeader,
            secret);
    if (!InternalRequestSigner.constantTimeEquals(signature, expected)) {
      log.warn(
          "Reject internal request due to invalid signature: method={}, path={}",
          request.getMethod(),
          request.getRequestURI());
      reject(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid internal auth signature");
      return;
    }

    InternalAuthenticatedPrincipal principal =
        new InternalAuthenticatedPrincipal(
            subject,
            userId,
            username,
            clientId,
            parseCsv(rolesHeader),
            parseCsv(permissionsHeader),
            parseCsv(scopesHeader));
    Collection<GrantedAuthority> authorities = buildAuthorities(principal);
    AbstractAuthenticationToken authentication =
        new AbstractAuthenticationToken(authorities) {
          @Override
          public Object getCredentials() {
            return "";
          }

          @Override
          public Object getPrincipal() {
            return principal;
          }

          @Override
          public String getName() {
            if (StrUtil.isNotBlank(principal.username())) {
              return principal.username();
            }
            if (StrUtil.isNotBlank(principal.userId())) {
              return principal.userId();
            }
            return StrUtil.blankToDefault(principal.subject(), "internal");
          }
        };
    authentication.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    filterChain.doFilter(request, response);
  }

  private boolean hasBearerAuthorization(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    return StrUtil.isNotBlank(authorization) && authorization.startsWith(BEARER_PREFIX);
  }

  private Collection<GrantedAuthority> buildAuthorities(InternalAuthenticatedPrincipal principal) {
    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    principal.roles().stream()
        .map(String::trim)
        .filter(StrUtil::isNotBlank)
        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
        .map(SimpleGrantedAuthority::new)
        .forEach(authorities::add);
    principal.permissions().stream()
        .map(String::trim)
        .filter(StrUtil::isNotBlank)
        .forEach(
            permission -> {
              authorities.add(new SimpleGrantedAuthority(permission));
              authorities.add(new SimpleGrantedAuthority("SCOPE_" + permission));
            });
    principal.scopes().stream()
        .map(String::trim)
        .filter(StrUtil::isNotBlank)
        .map(scope -> scope.startsWith("SCOPE_") ? scope : "SCOPE_" + scope)
        .map(SimpleGrantedAuthority::new)
        .forEach(authorities::add);
    return authorities;
  }

  private Set<String> parseCsv(String raw) {
    if (StrUtil.isBlank(raw)) {
      return Set.of();
    }
    return java.util.Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private void reject(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}");
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return "OPTIONS".equalsIgnoreCase(request.getMethod());
  }
}
