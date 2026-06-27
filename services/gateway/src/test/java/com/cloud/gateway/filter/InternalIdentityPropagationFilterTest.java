package com.cloud.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.common.security.InternalRequestHeaders;
import com.cloud.common.security.InternalRequestSigner;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

class InternalIdentityPropagationFilterTest {

  private static final String SECRET = "test-internal-secret";

  @Test
  void jwtAuthenticationIsConvertedToSignedInternalHeaders() {
    InternalIdentityPropagationFilter filter = new InternalIdentityPropagationFilter();
    ReflectionTestUtils.setField(filter, "enabled", true);
    ReflectionTestUtils.setField(filter, "secret", SECRET);
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .build());
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-1")
            .claim("user_id", 1001L)
            .claim("preferred_username", "alice")
            .claim("client_id", "web-client")
            .claim("roles", List.of("admin"))
            .claim("permissions", List.of("order:read"))
            .claim("scope", "internal profile")
            .build();
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("SCOPE_internal")));

    ServerWebExchange signedExchange =
        ReflectionTestUtils.invokeMethod(filter, "withInternalIdentity", exchange, authentication);

    HttpHeaders headers = signedExchange.getRequest().getHeaders();
    assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_REQUEST)).isEqualTo("true");
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_SUBJECT)).isEqualTo("user-1");
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_USER_ID)).isEqualTo("1001");
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_USERNAME)).isEqualTo("alice");
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_CLIENT_ID)).isEqualTo("web-client");
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_ROLES)).isEqualTo("admin");
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_PERMISSIONS))
        .isEqualTo("order:read");
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_SCOPES))
        .isEqualTo("internal,profile");
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_SIGNATURE))
        .isEqualTo(
            InternalRequestSigner.sign(
                "GET",
                "/api/orders/1",
                headers.getFirst(InternalRequestHeaders.INTERNAL_TIMESTAMP),
                "user-1",
                "1001",
                "alice",
                "web-client",
                "admin",
                "order:read",
                "internal,profile",
                SECRET));
  }

  @Test
  void blankSecretLeavesBearerRequestUnchanged() {
    InternalIdentityPropagationFilter filter = new InternalIdentityPropagationFilter();
    ReflectionTestUtils.setField(filter, "enabled", true);
    ReflectionTestUtils.setField(filter, "secret", "");
    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .build());
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-1")
            .claims(claims -> claims.putAll(Map.of("client_id", "web-client")))
            .build();

    ServerWebExchange unchanged =
        ReflectionTestUtils.invokeMethod(
            filter,
            "withInternalIdentity",
            exchange,
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("SCOPE_internal"))));

    HttpHeaders headers = unchanged.getRequest().getHeaders();
    assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
    assertThat(headers.getFirst(InternalRequestHeaders.INTERNAL_REQUEST)).isNull();
  }
}
