package com.cloud.gateway.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketRedisConfig {

  private static final String USER_CHANNEL_PREFIX = "ws:message:";

  private final WebSocketSessionRegistry sessionRegistry;

  @Bean
  public ReactiveRedisMessageListenerContainer webSocketRedisListenerContainer(
      ReactiveRedisConnectionFactory connectionFactory) {

    ReactiveRedisMessageListenerContainer container =
        new ReactiveRedisMessageListenerContainer(connectionFactory);

    // Use string serialization so both channel and payload arrive as decoded strings.
    ReactiveRedisOperations<String, String> ops =
        new ReactiveRedisTemplate<>(connectionFactory, RedisSerializationContext.string());

    ops.listenToPattern(USER_CHANNEL_PREFIX + "*")
        .subscribe(
            message -> {
              String channel = message.getChannel();
              String payload = message.getMessage();

              String userId = resolveUserId(channel);
              if (userId == null) return;

              boolean delivered = sessionRegistry.sendToUser(userId, payload);
              if (!delivered) {
                log.debug("No local WebSocket session " + "for userId={}, ignore", userId);
              }
            },
            error -> log.error("WebSocket redis subscriber error", error));

    return container;
  }

  private String resolveUserId(String channel) {
    if (channel == null || !channel.startsWith(USER_CHANNEL_PREFIX)) {
      return null;
    }
    String userId = channel.substring(USER_CHANNEL_PREFIX.length());
    return userId.isBlank() ? null : userId;
  }
}
