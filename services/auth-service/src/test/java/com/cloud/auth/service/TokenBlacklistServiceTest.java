package com.cloud.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private ValueOperations<String, Object> valueOperations;

  @Mock private HashOperations<String, Object, Object> hashOperations;

  private TokenBlacklistService tokenBlacklistService;

  @BeforeEach
  void setUp() {
    tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
  }

  @Test
  void addToBlacklist_emptyToken_skips() {
    tokenBlacklistService.addToBlacklist(" ", "sub", 60, "reason");

    verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
  }

  @Test
  void addToBlacklist_writesStats() {
    tokenBlacklistService.addToBlacklist("token", "sub", 60, "reason");

    verify(valueOperations).set(anyString(), any(), anyLong(), eq(TimeUnit.SECONDS));
    verify(hashOperations).increment("auth:blacklist:stats", "total_blacklisted", 1);
    verify(hashOperations).increment("auth:blacklist:stats", "active_blacklisted", 1);
    verify(hashOperations).put(eq("auth:blacklist:stats"), eq("last_updated"), any());
  }

  @Test
  void isBlacklisted_returnsTrueWhenKeyExists() {
    when(redisTemplate.hasKey(anyString())).thenReturn(true);

    boolean result = tokenBlacklistService.isBlacklisted("token");

    assertThat(result).isTrue();
  }
}
