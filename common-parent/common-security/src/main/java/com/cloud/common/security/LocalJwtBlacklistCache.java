package com.cloud.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.security.oauth2.jwt.Jwt;

public class LocalJwtBlacklistCache {

  private static final Duration DEFAULT_GRACE_PERIOD = Duration.ofMinutes(10);

  private final Duration gracePeriod;
  private final ConcurrentMap<String, Instant> entries = new ConcurrentHashMap<>();

  public LocalJwtBlacklistCache() {
    this(DEFAULT_GRACE_PERIOD);
  }

  public LocalJwtBlacklistCache(Duration gracePeriod) {
    this.gracePeriod =
        gracePeriod == null || gracePeriod.isNegative() ? DEFAULT_GRACE_PERIOD : gracePeriod;
  }

  public void remember(String tokenValue, Jwt jwt) {
    if (tokenValue == null || tokenValue.isBlank() || jwt == null) {
      return;
    }
    Instant expiresAt = jwt.getExpiresAt();
    if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
      expiresAt = Instant.now().plus(gracePeriod);
    }
    entries.put(tokenValue, expiresAt);
  }

  public void clear(String tokenValue) {
    if (tokenValue == null || tokenValue.isBlank()) {
      return;
    }
    entries.remove(tokenValue);
  }

  public boolean contains(String tokenValue) {
    if (tokenValue == null || tokenValue.isBlank()) {
      return false;
    }
    Instant expiresAt = entries.get(tokenValue);
    if (expiresAt == null) {
      return false;
    }
    if (expiresAt.isBefore(Instant.now())) {
      entries.remove(tokenValue, expiresAt);
      return false;
    }
    return true;
  }

  public void evictExpired(String tokenValue) {
    if (tokenValue == null || tokenValue.isBlank()) {
      return;
    }
    Instant expiresAt = entries.get(tokenValue);
    if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
      entries.remove(tokenValue, expiresAt);
    }
  }
}
