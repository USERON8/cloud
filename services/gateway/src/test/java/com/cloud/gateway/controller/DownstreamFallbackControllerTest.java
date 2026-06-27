package com.cloud.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class DownstreamFallbackControllerTest {

  @Test
  void paymentFallbackReturnsUnavailableError() {
    DownstreamFallbackController controller = new DownstreamFallbackController(new ObjectMapper());
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.post("/gateway/fallback/payment").build());

    ResponseEntity<String> response = controller.paymentFallback(exchange).block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody())
        .contains("\"code\":18001")
        .contains("Payment service is unavailable")
        .doesNotContain("\"code\":200")
        .doesNotContain("processing");
  }
}
