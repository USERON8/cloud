package com.cloud.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cloud.auth.service.TokenBlacklistService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

@ExtendWith(MockitoExtension.class)
class BlacklistAwareJwtDecoderTest {

  @Mock private JwtDecoder delegate;

  @Mock private TokenBlacklistService tokenBlacklistService;

  private BlacklistAwareJwtDecoder decoder;

  @BeforeEach
  void setUp() {
    decoder = new BlacklistAwareJwtDecoder(delegate, tokenBlacklistService);
  }

  @Test
  void decodeShouldAllowTokenWhenBlacklistLookupFails() {
    Jwt jwt = buildJwt("token-a");
    when(delegate.decode("token-a")).thenReturn(jwt);
    when(tokenBlacklistService.isBlacklisted(jwt))
        .thenThrow(new IllegalStateException("redis unavailable"));

    Jwt result = decoder.decode("token-a");

    assertThat(result).isSameAs(jwt);
  }

  @Test
  void decodeShouldRejectTokenWhenLocalBlacklistCacheExists() {
    Jwt jwt = buildJwt("token-b");
    when(delegate.decode("token-b")).thenReturn(jwt);
    when(tokenBlacklistService.isBlacklisted(jwt))
        .thenReturn(true)
        .thenThrow(new IllegalStateException("redis unavailable"));

    assertThatThrownBy(() -> decoder.decode("token-b"))
        .isInstanceOf(JwtValidationException.class)
        .hasMessageContaining("JWT token has been revoked");

    assertThatThrownBy(() -> decoder.decode("token-b"))
        .isInstanceOf(JwtValidationException.class)
        .hasMessageContaining("JWT token has been revoked");
  }

  private Jwt buildJwt(String tokenValue) {
    Instant now = Instant.now();
    return Jwt.withTokenValue(tokenValue)
        .header("alg", "none")
        .subject("tester")
        .claim("jti", tokenValue + "-jti")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
