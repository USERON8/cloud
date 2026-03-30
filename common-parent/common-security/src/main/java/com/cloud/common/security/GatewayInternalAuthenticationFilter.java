package com.cloud.common.security;

import com.cloud.common.trace.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class GatewayInternalAuthenticationFilter extends OncePerRequestFilter {

  private final JwtAuthenticationConverter jwtAuthenticationConverter;
  private final boolean enabled;
  private final String secret;
  private final long timestampSkewSeconds;

  public GatewayInternalAuthenticationFilter(
      JwtAuthenticationConverter jwtAuthenticationConverter,
      boolean enabled,
      String secret,
      long timestampSkewSeconds) {
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    this.enabled = enabled;
    this.secret = secret;
    this.timestampSkewSeconds = timestampSkewSeconds;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!enabled || SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    String signature = request.getHeader(GatewayIdentityHeaders.SIGNATURE);
    String timestamp = request.getHeader(GatewayIdentityHeaders.TIMESTAMP);
    if (isBlank(signature) || isBlank(timestamp)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (!isTimestampValid(timestamp)) {
      log.warn(
          "Reject internal identity due to expired timestamp: path={}", request.getRequestURI());
      response.sendError(
          HttpServletResponse.SC_UNAUTHORIZED, "Internal identity timestamp expired");
      return;
    }

    String payload =
        GatewayIdentitySignatureSupport.canonicalPayload(
            request.getHeader(GatewayIdentityHeaders.USER_ID),
            request.getHeader(GatewayIdentityHeaders.USERNAME),
            request.getHeader(GatewayIdentityHeaders.USER_NICKNAME),
            request.getHeader(GatewayIdentityHeaders.USER_STATUS),
            request.getHeader(GatewayIdentityHeaders.CLIENT_ID),
            request.getHeader(GatewayIdentityHeaders.USER_SCOPES),
            request.getHeader(GatewayIdentityHeaders.USER_ROLES),
            request.getHeader(GatewayIdentityHeaders.USER_PERMISSIONS),
            request.getHeader(GatewayIdentityHeaders.USER_AUTHORITIES),
            request.getHeader(GatewayIdentityHeaders.TRACE_ID),
            request.getHeader(GatewayIdentityHeaders.SUBJECT),
            timestamp);
    String expected = GatewayIdentitySignatureSupport.sign(payload, secret);
    if (!GatewayIdentitySignatureSupport.constantTimeEquals(signature, expected)) {
      log.warn(
          "Reject internal identity due to invalid signature: path={}", request.getRequestURI());
      response.sendError(
          HttpServletResponse.SC_UNAUTHORIZED, "Internal identity signature invalid");
      return;
    }

    Jwt jwt = buildJwt(request, timestamp);
    JwtAuthenticationToken authentication =
        (JwtAuthenticationToken) jwtAuthenticationConverter.convert(jwt);
    if (authentication == null) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Internal identity invalid");
      return;
    }
    SecurityContextHolder.getContext().setAuthentication(authentication);
    try {
      String traceId = request.getHeader(GatewayIdentityHeaders.TRACE_ID);
      if (!isBlank(traceId)) {
        MDC.put(TraceIdUtil.TRACE_ID_KEY, traceId);
      }
      filterChain.doFilter(request, response);
    } finally {
      if (!isBlank(request.getHeader(GatewayIdentityHeaders.TRACE_ID))) {
        MDC.remove(TraceIdUtil.TRACE_ID_KEY);
      }
      SecurityContextHolder.clearContext();
    }
  }

  private Jwt buildJwt(HttpServletRequest request, String timestamp) {
    Instant issuedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
    Instant expiresAt = issuedAt.plusSeconds(Math.max(30L, timestampSkewSeconds));
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("user_id", trim(request.getHeader(GatewayIdentityHeaders.USER_ID)));
    claims.put("username", trim(request.getHeader(GatewayIdentityHeaders.USERNAME)));
    claims.put("nickname", trim(request.getHeader(GatewayIdentityHeaders.USER_NICKNAME)));
    claims.put("status", trim(request.getHeader(GatewayIdentityHeaders.USER_STATUS)));
    claims.put("client_id", trim(request.getHeader(GatewayIdentityHeaders.CLIENT_ID)));
    claims.put("scope", trim(request.getHeader(GatewayIdentityHeaders.USER_SCOPES)));
    claims.put("roles", splitClaim(request.getHeader(GatewayIdentityHeaders.USER_ROLES)));
    claims.put(
        "permissions", splitClaim(request.getHeader(GatewayIdentityHeaders.USER_PERMISSIONS)));
    claims.put(
        "authorities", splitClaim(request.getHeader(GatewayIdentityHeaders.USER_AUTHORITIES)));
    claims.put("aud", List.of("internal-api"));
    claims.put("trace_id", trim(request.getHeader(GatewayIdentityHeaders.TRACE_ID)));
    String subject = trim(request.getHeader(GatewayIdentityHeaders.SUBJECT));
    if (subject.isBlank()) {
      subject = trim(request.getHeader(GatewayIdentityHeaders.USERNAME));
    }
    return Jwt.withTokenValue("gateway-internal")
        .header("alg", "internal-hmac")
        .issuer("gateway")
        .subject(subject.isBlank() ? "gateway-user" : subject)
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .claims(map -> map.putAll(claims))
        .build();
  }

  private boolean isTimestampValid(String timestamp) {
    try {
      long ts = Long.parseLong(timestamp);
      long now = Instant.now().getEpochSecond();
      return Math.abs(now - ts) <= Math.max(5L, timestampSkewSeconds);
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private List<String> splitClaim(String value) {
    if (isBlank(value)) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    for (String part : value.trim().split("[,\\s]+")) {
      if (!part.isBlank()) {
        result.add(part.trim());
      }
    }
    return result;
  }

  private String trim(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
