package com.cloud.gateway.websocket;

import cn.hutool.core.util.StrUtil;
import java.net.URI;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWebSocketHandler implements WebSocketHandler {

  private final WebSocketSessionRegistry sessionRegistry;

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    String requestedUserId = resolveUserId(session.getHandshakeInfo().getUri());
    if (requestedUserId == null) {
      return session.close(CloseStatus.BAD_DATA);
    }

    return session
        .getHandshakeInfo()
        .getPrincipal()
        .map(this::resolveAuthenticatedUserId)
        .filter(StrUtil::isNotBlank)
        .switchIfEmpty(
            Mono.defer(() -> session.close(CloseStatus.POLICY_VIOLATION).then(Mono.empty())))
        .flatMap(
            authenticatedUserId -> {
              if (!requestedUserId.equals(authenticatedUserId)) {
                log.warn(
                    "Reject websocket session due to user mismatch: requestedUserId={}, authenticatedUserId={}",
                    requestedUserId,
                    authenticatedUserId);
                return session.close(CloseStatus.POLICY_VIOLATION);
              }
              return registerAndHandle(session, authenticatedUserId);
            });
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

  private Mono<Void> registerAndHandle(WebSocketSession session, String userId) {
    String sessionId = session.getId();
    Sinks.Many<String> sink = sessionRegistry.register(userId, sessionId);

    Mono<Void> inbound =
        session
            .receive()
            .map(WebSocketMessage::getPayloadAsText)
            .doOnNext(
                payload ->
                    log.debug("WS inbound ignored: userId={}, size={}", userId, payload.length()))
            .then();

    return session
        .send(sink.asFlux().map(session::textMessage))
        .and(inbound)
        .doFinally(signal -> sessionRegistry.unregister(userId, sessionId));
  }

  private String resolveAuthenticatedUserId(Principal principal) {
    if (principal instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      return resolveUserId(jwtAuthenticationToken.getToken());
    }
    if (principal instanceof Authentication authentication) {
      Object authPrincipal = authentication.getPrincipal();
      if (authPrincipal instanceof Jwt jwt) {
        return resolveUserId(jwt);
      }
      String name = authentication.getName();
      return StrUtil.isBlank(name) ? null : name;
    }
    return principal == null ? null : principal.getName();
  }

  private String resolveUserId(Jwt jwt) {
    if (jwt == null) {
      return null;
    }
    String userId = jwt.getClaimAsString("user_id");
    if (StrUtil.isBlank(userId)) {
      userId = jwt.getClaimAsString("userId");
    }
    if (StrUtil.isBlank(userId)) {
      userId = jwt.getSubject();
    }
    return StrUtil.isBlank(userId) ? null : userId;
  }
}
