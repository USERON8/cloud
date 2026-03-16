package com.cloud.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cloud.user.mapper.UserMapper;
import com.cloud.user.service.support.AuthPrincipalService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class UserStatisticsServiceImplTest {

  @Mock private UserMapper userMapper;

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private AuthPrincipalService authPrincipalService;

  @Mock private CacheManager cacheManager;

  private UserStatisticsServiceImpl userStatisticsService;

  @BeforeEach
  void setUp() {
    userStatisticsService =
        new UserStatisticsServiceImpl(
            userMapper, redisTemplate, authPrincipalService, cacheManager);
  }

  @Test
  void calculateUserGrowthRate_previousZero_returns100() {
    when(userMapper.selectCount(any())).thenReturn(10L, 0L);

    Double growthRate = userStatisticsService.calculateUserGrowthRate(7);

    assertThat(growthRate).isEqualTo(100.0);
  }

  @Test
  void getUserStatusDistribution_countsActiveInactive() {
    when(userMapper.selectCount(any())).thenReturn(5L, 3L);

    Map<String, Long> distribution = userStatisticsService.getUserStatusDistribution();

    assertThat(distribution).containsEntry("active", 3L).containsEntry("inactive", 2L);
  }
}
