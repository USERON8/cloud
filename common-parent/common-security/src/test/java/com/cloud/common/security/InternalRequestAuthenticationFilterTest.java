package com.cloud.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class InternalRequestAuthenticationFilterTest {

  private static final String SECRET = "test-internal-secret";

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void internalRequestFailsClosedWhenSecretIsBlank() throws Exception {
    InternalRequestAuthenticationFilter filter = filter("");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
    request.addHeader(InternalRequestHeaders.INTERNAL_REQUEST, "true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainCalled = new AtomicBoolean();

    filter.doFilter(request, response, chain(chainCalled));

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getContentAsString()).contains("internal auth unavailable");
    assertThat(chainCalled).isFalse();
  }

  @Test
  void bearerRequestBypassesInternalHmac() throws Exception {
    InternalRequestAuthenticationFilter filter = filter("");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
    request.addHeader("Authorization", "Bearer token");
    request.addHeader(InternalRequestHeaders.INTERNAL_REQUEST, "true");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainCalled = new AtomicBoolean();

    filter.doFilter(request, response, chain(chainCalled));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chainCalled).isTrue();
  }

  @Test
  void validInternalSignatureAuthenticatesPrincipal() throws Exception {
    InternalRequestAuthenticationFilter filter = filter(SECRET);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1");
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    request.addHeader(InternalRequestHeaders.INTERNAL_REQUEST, "true");
    request.addHeader(InternalRequestHeaders.INTERNAL_TIMESTAMP, timestamp);
    request.addHeader(InternalRequestHeaders.INTERNAL_SUBJECT, "user-1");
    request.addHeader(InternalRequestHeaders.INTERNAL_USER_ID, "1001");
    request.addHeader(InternalRequestHeaders.INTERNAL_USERNAME, "alice");
    request.addHeader(InternalRequestHeaders.INTERNAL_CLIENT_ID, "gateway");
    request.addHeader(InternalRequestHeaders.INTERNAL_ROLES, "admin");
    request.addHeader(InternalRequestHeaders.INTERNAL_PERMISSIONS, "order:read");
    request.addHeader(InternalRequestHeaders.INTERNAL_SCOPES, "internal");
    request.addHeader(
        InternalRequestHeaders.INTERNAL_SIGNATURE,
        InternalRequestSigner.sign(
            "GET",
            "/api/orders/1",
            timestamp,
            "user-1",
            "1001",
            "alice",
            "gateway",
            "admin",
            "order:read",
            "internal",
            SECRET));
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainCalled = new AtomicBoolean();

    filter.doFilter(request, response, chain(chainCalled));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chainCalled).isTrue();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
        .extracting(Object::toString)
        .contains("ROLE_ADMIN", "order:read", "SCOPE_order:read", "SCOPE_internal");
  }

  private InternalRequestAuthenticationFilter filter(String secret) {
    InternalRequestAuthenticationFilter filter = new InternalRequestAuthenticationFilter();
    ReflectionTestUtils.setField(filter, "enabled", true);
    ReflectionTestUtils.setField(filter, "secret", secret);
    ReflectionTestUtils.setField(filter, "timestampSkewSeconds", 60L);
    return filter;
  }

  private FilterChain chain(AtomicBoolean chainCalled) {
    return (request, response) -> chainCalled.set(true);
  }
}
