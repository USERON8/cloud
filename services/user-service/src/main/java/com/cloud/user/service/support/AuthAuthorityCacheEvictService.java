package com.cloud.user.service.support;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthAuthorityCacheEvictService {

  private static final String AUTH_USER_PREFIX = "auth:user:";
  private static final String EVICT_CHANNEL = "auth:cache:evict";

  private final RedisTemplate<String, Object> redisTemplate;

  public AuthAuthorityCacheEvictService(
      @Qualifier("securityRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void evictUser(Long userId) {
    if (userId == null) {
      return;
    }
    redisTemplate.delete(AUTH_USER_PREFIX + userId);
    redisTemplate.convertAndSend(EVICT_CHANNEL, userId.toString());
  }

  public void evictUsers(Collection<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return;
    }
    Set<String> keys = new LinkedHashSet<>();
    for (Long userId : userIds) {
      if (userId == null) {
        continue;
      }
      keys.add(AUTH_USER_PREFIX + userId);
    }
    if (keys.isEmpty()) {
      return;
    }
    redisTemplate.delete(keys);
    redisTemplate.convertAndSend(EVICT_CHANNEL, String.join(",", keys));
  }
}
