package com.cloud.common.util;

import java.time.Duration;

public final class CacheTtlSupport {

  private static final long DEFAULT_MIN_TTL_SECONDS = 60L;

  private CacheTtlSupport() {}

  public static Duration ttlDuration(long ttlSeconds) {
    return ttlDuration(ttlSeconds, DEFAULT_MIN_TTL_SECONDS);
  }

  public static Duration ttlDuration(long ttlSeconds, long minTtlSeconds) {
    return Duration.ofSeconds(Math.max(minTtlSeconds, ttlSeconds));
  }
}
