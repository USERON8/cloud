package com.cloud.auth.config;

import com.cloud.auth.service.TokenBlacklistService;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class BlacklistAwareJwtDecoder implements JwtDecoder {

  private static final Duration LOCAL_BLACKLIST_GRACE_PERIOD = Duration.ofMinutes(10);

  private final JwtDecoder delegate;
  private final TokenBlacklistService tokenBlacklistService;
  private final ConcurrentMap<String, Instant> localBlacklistCache = new ConcurrentHashMap<>();

  @Override
  public Jwt decode(String token) throws JwtException {

    Jwt jwt = delegate.decode(token);

    String tokenValue = jwt.getTokenValue();
    evictExpiredLocalEntry(tokenValue);

    try {
      if (tokenBlacklistService.isBlacklisted(jwt)) {
        rememberBlacklistedToken(tokenValue, jwt);
        log.warn("JWT token blacklisted: subject={}, jti={}", jwt.getSubject(), jwt.getId());
        throw blacklistedException();
      }
      localBlacklistCache.remove(tokenValue);
      return jwt;
    } catch (JwtException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      if (isLocallyBlacklisted(tokenValue)) {
        log.warn(
            "JWT blacklist validation fell back to local cache: subject={}, jti={}",
            jwt.getSubject(),
            jwt.getId(),
            ex);
        throw blacklistedException();
      }
      log.error(
          "JWT blacklist validation failed, allow token temporarily: subject={}, jti={}",
          jwt.getSubject(),
          jwt.getId(),
          ex);
    }

    return jwt;
  }

  private JwtValidationException blacklistedException() {
    return new JwtValidationException(
        "JWT token has been revoked",
        OAuth2TokenValidatorResult.failure(
                new OAuth2Error("blacklisted", "Token is blacklisted", null))
            .getErrors());
  }

  private void rememberBlacklistedToken(String tokenValue, Jwt jwt) {
    Instant expiresAt = jwt.getExpiresAt();
    if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
      expiresAt = Instant.now().plus(LOCAL_BLACKLIST_GRACE_PERIOD);
    }
    localBlacklistCache.put(tokenValue, expiresAt);
  }

  private boolean isLocallyBlacklisted(String tokenValue) {
    Instant expiresAt = localBlacklistCache.get(tokenValue);
    if (expiresAt == null) {
      return false;
    }
    if (expiresAt.isBefore(Instant.now())) {
      localBlacklistCache.remove(tokenValue, expiresAt);
      return false;
    }
    return true;
  }

  private void evictExpiredLocalEntry(String tokenValue) {
    Instant expiresAt = localBlacklistCache.get(tokenValue);
    if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
      localBlacklistCache.remove(tokenValue, expiresAt);
    }
  }
}
