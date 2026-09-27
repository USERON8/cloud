package com.cloud.auth.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthVersionStateService {

  private static final String AUTH_VERSION_KEY_PREFIX = "auth:version:";

  private final StringRedisTemplate redisTemplate;

  public AuthVersionStateService(
      @Qualifier("securityStringRedisTemplate") StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void publish(Long userId, Long authVersion) {
    if (userId == null || authVersion == null) {
      return;
    }
    redisTemplate
        .opsForValue()
        .set(AUTH_VERSION_KEY_PREFIX + userId, String.valueOf(authVersion));
  }
}
