package com.cloud.gateway.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketRedisConfig {

    private static final String USER_CHANNEL_PREFIX = "ws:message:";

    private final WebSocketSessionRegistry sessionRegistry;

    @Bean
    public ReactiveRedisMessageListenerContainer webSocketRedisListenerContainer(
            ReactiveRedisConnectionFactory connectionFactory) {
        ReactiveRedisMessageListenerContainer container = new ReactiveRedisMessageListenerContainer(connectionFactory);
        container.receive(new PatternTopic(USER_CHANNEL_PREFIX + "*"))
                .subscribe(message -> {
                    String channel = decode(message.getChannel());
                    String payload = decode(message.getMessage());
                    if (channel == null || payload == null) {
                        return;
                    }
                    String userId = resolveUserId(channel);
                    if (userId == null) {
                        return;
                    }
                    boolean delivered = sessionRegistry.sendToUser(userId, payload);
                    if (!delivered) {
                        log.debug("No local WebSocket session for userId={}, ignore redis message", userId);
                    }
                }, error -> log.error("WebSocket redis subscriber error", error));
        return container;
    }

    private String resolveUserId(String channel) {
        if (channel == null || !channel.startsWith(USER_CHANNEL_PREFIX)) {
            return null;
        }
        String userId = channel.substring(USER_CHANNEL_PREFIX.length());
        return userId.isBlank() ? null : userId;
    }

    private String decode(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        ByteBuffer readonly = buffer.asReadOnlyBuffer();
        return StandardCharsets.UTF_8.decode(readonly).toString();
    }
}
