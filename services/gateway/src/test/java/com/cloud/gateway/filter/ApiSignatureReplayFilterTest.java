package com.cloud.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

class ApiSignatureReplayFilterTest {

  private static final String SECRET = "test-secret";

  private ReactiveStringRedisTemplate reactiveStringRedisTemplate;
  private ReactiveValueOperations<String, String> valueOperations;
  private ApiSignatureReplayFilter filter;

  @BeforeEach
  void setUp() {
    reactiveStringRedisTemplate = mock(ReactiveStringRedisTemplate.class);
    valueOperations = mock(ReactiveValueOperations.class);
    when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(Mono.just(true));

    filter = new ApiSignatureReplayFilter(reactiveStringRedisTemplate);
    ReflectionTestUtils.setField(filter, "enabled", true);
    ReflectionTestUtils.setField(filter, "secret", SECRET);
    ReflectionTestUtils.setField(filter, "timestampSkewSeconds", 300L);
    ReflectionTestUtils.setField(filter, "nonceExpireSeconds", 600L);
  }

  @Test
  void shouldRejectSignatureThatOnlyCoversPath() {
    String body = "{\"skuId\":1001,\"quantity\":2}";
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String nonce = "nonce-1";
    MockServerHttpRequest request =
        MockServerHttpRequest.post("/api/orders?source=web")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .header("X-Signature", signPathOnly("POST", "/api/orders", timestamp, nonce))
            .body(body);
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    filter
        .filter(exchange, ex -> Mono.error(new AssertionError("chain should not be called")))
        .block(Duration.ofSeconds(1));

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(valueOperations, never()).setIfAbsent(anyString(), eq("1"), any());
  }

  @Test
  void shouldAcceptSignatureCoveringQueryAndBodyAndPreserveBody() {
    String body = "{\"skuId\":1001,\"quantity\":2}";
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String nonce = "nonce-2";
    MockServerHttpRequest request =
        MockServerHttpRequest.post("/api/orders?source=web")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .header(
                "X-Signature",
                signFullPayload("POST", "/api/orders", "source=web", body, timestamp, nonce))
            .body(body);
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    final String[] forwardedBody = new String[1];
    filter
        .filter(
            exchange,
            ex ->
                DataBufferUtils.join(ex.getRequest().getBody())
                    .doOnNext(
                        buffer -> {
                          byte[] bytes = new byte[buffer.readableByteCount()];
                          buffer.read(bytes);
                          DataBufferUtils.release(buffer);
                          forwardedBody[0] = new String(bytes, StandardCharsets.UTF_8);
                        })
                    .then())
        .block(Duration.ofSeconds(1));

    assertThat(exchange.getResponse().getStatusCode()).isNull();
    assertThat(forwardedBody[0]).isEqualTo(body);
    verify(valueOperations).setIfAbsent(anyString(), eq("1"), any());
  }

  private String signPathOnly(String method, String path, String timestamp, String nonce) {
    return hmac(method + "\n" + path + "\n\n\n" + timestamp + "\n" + nonce);
  }

  private String signFullPayload(
      String method, String path, String rawQuery, String body, String timestamp, String nonce) {
    String encodedBody = Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
    return hmac(
        method
            + "\n"
            + path
            + "\n"
            + rawQuery
            + "\n"
            + encodedBody
            + "\n"
            + timestamp
            + "\n"
            + nonce);
  }

  private String hmac(String payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
