package com.cloud.gateway.config;

import com.cloud.common.trace.TraceIdUtil;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class LoadBalancedWebClientConfig {

  @Value("${app.remote.http.max-connections:200}")
  private int maxConnections;

  @Value("${app.remote.http.pending-acquire-timeout-ms:3000}")
  private long pendingAcquireTimeoutMs;

  @Value("${app.remote.http.connect-timeout-ms:2000}")
  private int connectTimeoutMs;

  @Value("${app.remote.http.response-timeout-ms:3000}")
  private long responseTimeoutMs;

  @Value("${app.remote.http.read-timeout-ms:3000}")
  private long readTimeoutMs;

  @Value("${app.remote.http.write-timeout-ms:3000}")
  private long writeTimeoutMs;

  @Bean
  @LoadBalanced
  public WebClient.Builder loadBalancedWebClientBuilder() {
    ConnectionProvider provider =
        ConnectionProvider.builder("gateway-webclient")
            .maxConnections(maxConnections)
            .pendingAcquireTimeout(Duration.ofMillis(pendingAcquireTimeoutMs))
            .build();
    HttpClient httpClient =
        HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(responseTimeoutMs))
            .doOnConnected(
                connection ->
                    connection
                        .addHandlerLast(
                            new io.netty.handler.timeout.ReadTimeoutHandler(
                                readTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(
                            new io.netty.handler.timeout.WriteTimeoutHandler(
                                writeTimeoutMs, TimeUnit.MILLISECONDS)));
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .filter(
            (request, next) -> {
              String traceId =
                  TraceIdUtil.normalizeTraceId(
                      request.headers().getFirst(TraceIdUtil.TRACE_HEADER));
              if (traceId.isBlank()) {
                traceId = TraceIdUtil.currentOrGenerate();
              }
              return next.exchange(
                  org.springframework.web.reactive.function.client.ClientRequest.from(request)
                      .header(TraceIdUtil.TRACE_HEADER, traceId)
                      .build());
            });
  }
}
