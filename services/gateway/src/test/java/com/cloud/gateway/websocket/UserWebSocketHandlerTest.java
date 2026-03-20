package com.cloud.gateway.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@ExtendWith(MockitoExtension.class)
class UserWebSocketHandlerTest {

  @Mock private WebSocketSessionRegistry sessionRegistry;
  @Mock private WebSocketSession session;
  @Mock private HandshakeInfo handshakeInfo;
  @Mock private Sinks.Many<String> sink;

  private UserWebSocketHandler handler;

  @BeforeEach
  void setUp() {
    handler = new UserWebSocketHandler(sessionRegistry);
  }

  @Test
  void shouldRejectAnonymousWebSocketHandshake() {
    when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
    when(handshakeInfo.getUri()).thenReturn(URI.create("ws://localhost/ws/1001"));
    when(handshakeInfo.getPrincipal()).thenReturn(Mono.empty());
    when(session.close(CloseStatus.POLICY_VIOLATION)).thenReturn(Mono.empty());

    handler.handle(session).block(Duration.ofSeconds(1));

    verify(sessionRegistry, never()).register(any(), any());
    verify(sessionRegistry, never()).unregister(any(), any());
  }

  @Test
  void shouldRejectMismatchedAuthenticatedUserId() {
    when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
    when(handshakeInfo.getUri()).thenReturn(URI.create("ws://localhost/ws/1001"));
    when(handshakeInfo.getPrincipal()).thenReturn(Mono.just(jwtAuthentication("2002")));
    when(session.close(CloseStatus.POLICY_VIOLATION)).thenReturn(Mono.empty());

    handler.handle(session).block(Duration.ofSeconds(1));

    verify(sessionRegistry, never()).register(any(), any());
    verify(sessionRegistry, never()).unregister(any(), any());
  }

  @Test
  void shouldRegisterAuthenticatedUserIdWhenPathMatches() {
    when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
    when(handshakeInfo.getUri()).thenReturn(URI.create("ws://localhost/ws/1001"));
    when(handshakeInfo.getPrincipal()).thenReturn(Mono.just(jwtAuthentication("1001")));
    when(session.getId()).thenReturn("session-1");
    when(session.receive()).thenReturn(Flux.empty());
    when(sink.asFlux()).thenReturn(Flux.empty());
    when(session.send(any())).thenReturn(Mono.empty());
    when(sessionRegistry.register("1001", "session-1")).thenReturn(sink);

    handler.handle(session).block(Duration.ofSeconds(1));

    ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> sessionIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(sessionRegistry).register(userIdCaptor.capture(), sessionIdCaptor.capture());
    assertThat(userIdCaptor.getValue()).isEqualTo("1001");
    assertThat(sessionIdCaptor.getValue()).isEqualTo("session-1");
    verify(sessionRegistry).unregister("1001", "session-1");
  }

  private Authentication jwtAuthentication(String userId) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("user_id", userId)
            .claim("userId", userId)
            .subject(userId)
            .issuedAt(java.time.Instant.now())
            .expiresAt(java.time.Instant.now().plusSeconds(300))
            .build();
    return new JwtAuthenticationToken(jwt, List.of());
  }
}
