package com.cloud.gateway.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWebSocketHandler implements WebSocketHandler {

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String userId = resolveUserId(session.getHandshakeInfo().getUri());
        if (userId == null) {
            return session.close(CloseStatus.BAD_DATA);
        }

        String sessionId = session.getId();
        Sinks.Many<String> sink = sessionRegistry.register(userId, sessionId);

        Mono<Void> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(payload -> log.debug("WS inbound ignored: userId={}, size={}", userId, payload.length()))
                .then();

        return session.send(sink.asFlux().map(session::textMessage))
                .and(inbound)
                .doFinally(signal -> sessionRegistry.unregister(userId, sessionId));
    }

    private String resolveUserId(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return null;
        }
        String path = uri.getPath();
        int idx = path.lastIndexOf('/');
        if (idx < 0 || idx == path.length() - 1) {
            return null;
        }
        String userId = path.substring(idx + 1);
        return userId.isBlank() ? null : userId;
    }
}
