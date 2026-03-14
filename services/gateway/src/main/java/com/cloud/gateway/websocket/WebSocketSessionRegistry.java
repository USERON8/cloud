package com.cloud.gateway.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionRegistry {

    private static final String ONLINE_USERS_KEY = "ws:online:users";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    private final Map<String, Map<String, Sinks.Many<String>>> sessions = new ConcurrentHashMap<>();

    public Sinks.Many<String> register(String userId, String sessionId) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        Map<String, Sinks.Many<String>> userSessions = sessions.computeIfAbsent(userId, key -> new ConcurrentHashMap<>());
        boolean first = userSessions.isEmpty();
        userSessions.put(sessionId, sink);
        if (first) {
            reactiveStringRedisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId).subscribe();
        }
        log.debug("WebSocket session registered: userId={}, sessionId={}", userId, sessionId);
        return sink;
    }

    public void unregister(String userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        Map<String, Sinks.Many<String>> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(sessionId);
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
            reactiveStringRedisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId).subscribe();
        }
        log.debug("WebSocket session unregistered: userId={}, sessionId={}", userId, sessionId);
    }

    public boolean sendToUser(String userId, String payload) {
        if (userId == null || payload == null) {
            return false;
        }
        Map<String, Sinks.Many<String>> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return false;
        }
        boolean delivered = false;
        for (Map.Entry<String, Sinks.Many<String>> entry : userSessions.entrySet()) {
            Sinks.EmitResult result = entry.getValue().tryEmitNext(payload);
            if (result.isSuccess()) {
                delivered = true;
            } else if (result == Sinks.EmitResult.FAIL_TERMINATED || result == Sinks.EmitResult.FAIL_CANCELLED) {
                userSessions.remove(entry.getKey());
            }
        }
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
            reactiveStringRedisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId).subscribe();
        }
        return delivered;
    }

    public void broadcast(String payload) {
        if (payload == null) {
            return;
        }
        for (String userId : sessions.keySet()) {
            sendToUser(userId, payload);
        }
    }
}
