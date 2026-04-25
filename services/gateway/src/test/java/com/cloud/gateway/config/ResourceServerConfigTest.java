package com.cloud.gateway.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.cloud.gateway.support.GatewayResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

class ResourceServerConfigTest {

  private static final Instant ISSUED_AT = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant EXPIRES_AT = Instant.parse("2027-01-01T00:00:00Z");

  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    ReactiveStringRedisTemplate reactiveStringRedisTemplate =
        Mockito.mock(ReactiveStringRedisTemplate.class);
    given(reactiveStringRedisTemplate.hasKey(anyString())).willReturn(Mono.just(false));

    ResourceServerConfig config =
        new StubJwtResourceServerConfig(
            reactiveStringRedisTemplate, new GatewayResponseWriter(new ObjectMapper()));
    SecurityWebFilterChain securityWebFilterChain =
        config.springSecurityFilterChain(ServerHttpSecurity.http());
    WebFilterChain terminalChain =
        exchange -> {
          exchange.getResponse().setStatusCode(HttpStatus.OK);
          return exchange.getResponse().setComplete();
        };
    WebHandler webHandler =
        exchange -> new WebFilterChainProxy(securityWebFilterChain).filter(exchange, terminalChain);
    webTestClient = WebTestClient.bindToWebHandler(webHandler).build();
  }

  @Test
  void cancelOrderRequiresOrderCancelAuthority() {
    exchange("/api/orders/1/cancellation", HttpMethod.POST, "query").expectStatus().isForbidden();
    exchange("/api/orders/1/cancellation", HttpMethod.POST, "cancel").expectStatus().isOk();
  }

  @Test
  void batchShipmentAllowsMerchantRoleWithoutOrderQuery() {
    exchange("/api/orders/bulk/shipments", HttpMethod.POST, "query").expectStatus().isForbidden();
    exchange("/api/orders/bulk/shipments", HttpMethod.POST, "merchant").expectStatus().isOk();
  }

  @Test
  void completionKeepsOrderQueryAuthority() {
    exchange("/api/orders/1/completion", HttpMethod.POST, "cancel").expectStatus().isForbidden();
    exchange("/api/orders/1/completion", HttpMethod.POST, "query").expectStatus().isOk();
  }

  @Test
  void applyAfterSaleRequiresOrderRefundAuthority() {
    exchange("/api/after-sales", HttpMethod.POST, "query").expectStatus().isForbidden();
    exchange("/api/after-sales", HttpMethod.POST, "refund").expectStatus().isOk();
  }

  @Test
  void afterSaleEventsRequireOrderRefundAuthority() {
    exchange("/api/after-sales/1/events?action=APPROVE", HttpMethod.POST, "query")
        .expectStatus()
        .isForbidden();
    exchange("/api/after-sales/1/events?action=APPROVE", HttpMethod.POST, "refund")
        .expectStatus()
        .isOk();
  }

  @Test
  void orderReadStillRequiresOrderQueryAuthority() {
    exchange("/api/orders/1", HttpMethod.GET, "cancel").expectStatus().isForbidden();
    exchange("/api/orders/1", HttpMethod.GET, "query").expectStatus().isOk();
  }

  @Test
  void cartEndpointsAllowAnyAuthenticatedUserOnExactAndNestedPaths() {
    exchangeWithoutToken("/api/users/me/cart", HttpMethod.GET).expectStatus().isUnauthorized();
    exchange("/api/users/me/cart", HttpMethod.GET, "cancel").expectStatus().isOk();
    exchange("/api/users/me/cart/items", HttpMethod.PUT, "cancel").expectStatus().isOk();
  }

  private WebTestClient.ResponseSpec exchange(String uri, HttpMethod method, String token) {
    WebTestClient.RequestHeadersSpec<?> requestSpec;
    if (HttpMethod.POST.equals(method)) {
      requestSpec = webTestClient.post().uri(uri).headers(headers -> headers.setBearerAuth(token));
    } else if (HttpMethod.GET.equals(method)) {
      requestSpec = webTestClient.get().uri(uri).headers(headers -> headers.setBearerAuth(token));
    } else if (HttpMethod.PUT.equals(method)) {
      requestSpec = webTestClient.put().uri(uri).headers(headers -> headers.setBearerAuth(token));
    } else {
      throw new IllegalArgumentException("Unsupported method: " + method);
    }
    return requestSpec.exchange();
  }

  private WebTestClient.ResponseSpec exchangeWithoutToken(String uri, HttpMethod method) {
    WebTestClient.RequestHeadersSpec<?> requestSpec;
    if (HttpMethod.GET.equals(method)) {
      requestSpec = webTestClient.get().uri(uri);
    } else {
      throw new IllegalArgumentException("Unsupported method: " + method);
    }
    return requestSpec.exchange();
  }

  private static final class StubJwtResourceServerConfig extends ResourceServerConfig {

    private StubJwtResourceServerConfig(
        ReactiveStringRedisTemplate reactiveStringRedisTemplate,
        GatewayResponseWriter gatewayResponseWriter) {
      super(reactiveStringRedisTemplate, gatewayResponseWriter);
    }

    @Override
    public ReactiveJwtDecoder jwtDecoder() {
      return token -> Mono.just(buildJwt(token));
    }

    private Jwt buildJwt(String token) {
      List<String> roles =
          switch (token) {
            case "merchant" -> List.of("MERCHANT");
            default -> List.of("USER");
          };
      List<String> permissions =
          switch (token) {
            case "query" -> List.of("order:query");
            case "cancel" -> List.of("order:cancel");
            case "refund" -> List.of("order:refund");
            case "merchant" -> List.of();
            default -> List.of();
          };
      return Jwt.withTokenValue(token)
          .header("alg", "none")
          .claim("sub", "test-user")
          .claim("roles", roles)
          .claim("permissions", permissions)
          .issuedAt(ISSUED_AT)
          .expiresAt(EXPIRES_AT)
          .build();
    }
  }
}
