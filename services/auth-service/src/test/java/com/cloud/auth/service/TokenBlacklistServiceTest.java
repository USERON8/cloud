package com.cloud.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void addToBlacklist_emptyToken_skips() {
        tokenBlacklistService.addToBlacklist(" ", "sub", 60, "reason");

        verify(valueOperations, never()).set(anyString(), any(), any(), any());
    }

    @Test
    void addToBlacklist_writesStats() {
        tokenBlacklistService.addToBlacklist("token", "sub", 60, "reason");

        verify(valueOperations).set(anyString(), any(), any(), any());
        verify(hashOperations).increment("oauth2:blacklist:stats", "total_blacklisted", 1);
        verify(hashOperations).increment("oauth2:blacklist:stats", "active_blacklisted", 1);
        verify(hashOperations).put(anyString(), anyString(), any());
    }

    @Test
    void isBlacklisted_returnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted("token");

        assertThat(result).isTrue();
    }
}
