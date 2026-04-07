package com.cloud.gateway.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ApiSignatureReplayFilter implements GlobalFilter, Ordered {

  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final String HEADER_SIGNATURE = "X-Signature";
  private static final String HEADER_TIMESTAMP = "X-Timestamp";
  private static final String HEADER_NONCE = "X-Nonce";
  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String NONCE_KEY_PREFIX = "gateway:nonce:";

  private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

  @Value("${app.security.signature.enabled:true}")
  private boolean enabled;

  @Value("${app.security.signature.secret}")
  private String secret;

  @Value("${app.security.signature.timestamp-skew-seconds:300}")
  private long timestampSkewSeconds;

  @Value("${app.security.signature.nonce-expire-seconds:600}")
  private long nonceExpireSeconds;

  public ApiSignatureReplayFilter(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
    this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
  }

  @PostConstruct
  public void validateSecret() {
    if (enabled && StrUtil.isBlank(secret)) {
      throw new IllegalStateException(
          "GATEWAY_SIGNATURE_SECRET must be configured when app.security.signature.enabled=true");
    }
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!enabled) {
      return chain.filter(exchange);
    }

    ServerHttpRequest request = exchange.getRequest();
    HttpMethod method = request.getMethod();
    if (method == null
        || HttpMethod.GET.equals(method)
        || HttpMethod.HEAD.equals(method)
        || HttpMethod.OPTIONS.equals(method)) {
      return chain.filter(exchange);
    }

    String path = request.getPath().pathWithinApplication().value();
    if (!path.startsWith("/api/")) {
      return chain.filter(exchange);
    }

    String authorization = request.getHeaders().getFirst(HEADER_AUTHORIZATION);
    if (StrUtil.isNotBlank(authorization) && authorization.startsWith(BEARER_PREFIX)) {
      return chain.filter(exchange);
    }

    String signature = request.getHeaders().getFirst(HEADER_SIGNATURE);
    String timestamp = request.getHeaders().getFirst(HEADER_TIMESTAMP);
    String nonce = request.getHeaders().getFirst(HEADER_NONCE);

    if (StrUtil.isBlank(signature) || StrUtil.isBlank(timestamp) || StrUtil.isBlank(nonce)) {
      return reject(exchange, HttpStatus.UNAUTHORIZED, "signature headers missing");
    }

    long ts;
    try {
      ts = Long.parseLong(timestamp);
    } catch (NumberFormatException e) {
      return reject(exchange, HttpStatus.UNAUTHORIZED, "invalid timestamp");
    }

    long now = Instant.now().getEpochSecond();
    if (Math.abs(now - ts) > Math.max(5, timestampSkewSeconds)) {
      return reject(exchange, HttpStatus.UNAUTHORIZED, "timestamp expired");
    }

    return DataBufferUtils.join(request.getBody())
        .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
        .flatMap(
            dataBuffer -> {
              byte[] bodyBytes = extractBytes(dataBuffer);
              DataBufferUtils.release(dataBuffer);

              String payload =
                  buildPayload(
                      method.name(),
                      path,
                      request.getURI().getRawQuery(),
                      bodyBytes,
                      timestamp,
                      nonce);
              String expected = sign(payload, secret);
              if (!constantTimeEquals(signature, expected)) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "invalid signature");
              }

              String nonceKey = NONCE_KEY_PREFIX + nonce;
              Duration ttl = Duration.ofSeconds(Math.max(30, nonceExpireSeconds));
              ServerHttpRequest decoratedRequest =
                  decorateRequest(request, exchange.getResponse().bufferFactory(), bodyBytes);
              return reactiveStringRedisTemplate
                  .opsForValue()
                  .setIfAbsent(nonceKey, "1", ttl)
                  .flatMap(
                      acquired -> {
                        if (Boolean.TRUE.equals(acquired)) {
                          return chain.filter(exchange.mutate().request(decoratedRequest).build());
                        }
                        log.warn("Replay request rejected: path={}, nonce={}", path, nonce);
                        return reject(exchange, HttpStatus.CONFLICT, "replay request");
                      });
            })
        .onErrorResume(
            ex -> {
              log.error("Signature replay check failed, path={}", path, ex);
              return reject(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "signature check failed");
            });
  }

  @Override
  public int getOrder() {
    return -150;
  }

  private String sign(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
      mac.init(secretKeySpec);
      byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return toHex(bytes);
    } catch (Exception e) {
      throw new IllegalStateException("sign payload failed", e);
    }
  }

  private String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private boolean constantTimeEquals(String actual, String expected) {
    if (actual == null || expected == null) {
      return false;
    }
    byte[] a = actual.getBytes(StandardCharsets.UTF_8);
    byte[] b = expected.getBytes(StandardCharsets.UTF_8);
    if (a.length != b.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }
    return result == 0;
  }

  private byte[] extractBytes(DataBuffer dataBuffer) {
    if (dataBuffer == null) {
      return new byte[0];
    }
    ByteBuffer byteBuffer = dataBuffer.asByteBuffer().asReadOnlyBuffer();
    byte[] bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);
    return bytes;
  }

  private String buildPayload(
      String method,
      String path,
      String rawQuery,
      byte[] bodyBytes,
      String timestamp,
      String nonce) {
    String query = rawQuery == null ? "" : rawQuery;
    String body = bodyBytes == null ? "" : Base64.getEncoder().encodeToString(bodyBytes);
    return method + "\n" + path + "\n" + query + "\n" + body + "\n" + timestamp + "\n" + nonce;
  }

  private ServerHttpRequest decorateRequest(
      ServerHttpRequest request, DataBufferFactory bufferFactory, byte[] bodyBytes) {
    if (bodyBytes == null || bodyBytes.length == 0) {
      return request;
    }
    return new ServerHttpRequestDecorator(request) {
      @Override
      public reactor.core.publisher.Flux<DataBuffer> getBody() {
        return reactor.core.publisher.Flux.defer(
            () -> reactor.core.publisher.Flux.just(bufferFactory.wrap(bodyBytes)));
      }
    };
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String body = "{\"code\":" + status.value() + ",\"message\":\"" + message + "\"}";
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    return exchange
        .getResponse()
        .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
  }
}
