package com.cloud.order.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.order.dto.OrderAggregateResponse;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderAggregateCacheServiceTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private ValueOperations<String, Object> valueOperations;

  @InjectMocks private OrderAggregateCacheService orderAggregateCacheService;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void get_nonResponse_returnsNull() {
    when(valueOperations.get("order:aggregate:1")).thenReturn("not");

    OrderAggregateResponse result = orderAggregateCacheService.get(1L);

    assertThat(result).isNull();
  }

  @Test
  void put_usesMinimumTtl() {
    ReflectionTestUtils.setField(orderAggregateCacheService, "aggregateTtlSeconds", 30L);
    OrderAggregateResponse response = new OrderAggregateResponse();

    orderAggregateCacheService.put(2L, response);

    verify(valueOperations)
        .set(eq("order:aggregate:2"), eq(response), eq(60L), eq(TimeUnit.SECONDS));
  }
}
