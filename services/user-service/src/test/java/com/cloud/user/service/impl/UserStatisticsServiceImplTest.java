package com.cloud.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cloud.user.mapper.UserMapper;
import com.cloud.user.service.cache.UserStatisticsCacheService;
import com.cloud.user.service.support.AuthPrincipalService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class UserStatisticsServiceImplTest {

  @Mock private UserMapper userMapper;

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private AuthPrincipalService authPrincipalService;

  @Mock private UserStatisticsCacheService userStatisticsCacheService;

  private UserStatisticsServiceImpl userStatisticsService;

  @BeforeEach
  void setUp() {
    userStatisticsService =
        new UserStatisticsServiceImpl(
            userMapper, redisTemplate, authPrincipalService, userStatisticsCacheService);
  }

  @Test
  void calculateUserGrowthRate_previousZero_returns100() {
    when(userMapper.selectCount(nullable(Wrapper.class))).thenReturn(10L, 0L);

    Double growthRate = userStatisticsService.calculateUserGrowthRate(7);

    assertThat(growthRate).isEqualTo(100.0);
  }

  @Test
  void getUserStatusDistribution_cacheHit_returnsCachedValue() {
    Map<String, Long> cached = Map.of("active", 3L, "inactive", 2L);
    when(userStatisticsCacheService.getStatusDistribution()).thenReturn(cached);

    Map<String, Long> distribution = userStatisticsService.getUserStatusDistribution();

    assertThat(distribution).containsEntry("active", 3L).containsEntry("inactive", 2L);
    verifyNoInteractions(userMapper);
  }
}
