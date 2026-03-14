package com.cloud.user.service.support;

import com.cloud.user.module.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInfoHashCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private UserInfoHashCacheService userInfoHashCacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        ReflectionTestUtils.setField(userInfoHashCacheService, "ttlSeconds", 120L);
    }

    @Test
    void getById_parsesCache() {
        when(hashOperations.entries("user:info:1"))
                .thenReturn(Map.of(
                        "id", "1",
                        "username", "u1",
                        "email", "u1@example.com",
                        "status", "1"
                ));

        UserInfoHashCacheService.UserCache cache = userInfoHashCacheService.getById(1L);

        assertThat(cache).isNotNull();
        assertThat(cache.id()).isEqualTo(1L);
        assertThat(cache.username()).isEqualTo("u1");
        verify(redisTemplate, times(2)).expire(anyString(), any(Duration.class));
    }

    @Test
    void put_writesBothKeys() {
        User user = new User();
        user.setId(2L);
        user.setUsername("user2");
        user.setEmail("user2@example.com");
        user.setStatus(1);

        userInfoHashCacheService.put(user);

        verify(hashOperations, times(2)).putAll(anyString(), any(Map.class));
        verify(redisTemplate, times(2)).expire(anyString(), any(Duration.class));
    }
}
