package com.cloud.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MessageIdempotencyServiceTest {

  @Mock private StringRedisTemplate stringRedisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  private MessageIdempotencyService service;

  @BeforeEach
  void setUp() {
    service = new MessageIdempotencyService(stringRedisTemplate);
    ReflectionTestUtils.setField(service, "idempotentEnabled", true);
    ReflectionTestUtils.setField(service, "idempotentExpireSeconds", 3600L);
    ReflectionTestUtils.setField(service, "processingExpireSeconds", 300L);
  }

  @Test
  void tryAcquireShouldReturnFalseWhenMessageIsAlreadyProcessed() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(eq("mq:consumed:order:evt-1"), eq("PROCESSING"), any()))
        .thenReturn(false);
    when(valueOperations.get("mq:consumed:order:evt-1")).thenReturn("SUCCESS");

    boolean acquired = service.tryAcquire("order", "evt-1");

    assertThat(acquired).isFalse();
  }

  @Test
  void tryAcquireShouldFallBackToLocalGuardWhenRedisIsUnavailable() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(eq("mq:consumed:order:evt-2"), eq("PROCESSING"), any()))
        .thenThrow(new IllegalStateException("redis down"));

    boolean firstAcquire = service.tryAcquire("order", "evt-2");
    boolean secondAcquire = service.tryAcquire("order", "evt-2");

    assertThat(firstAcquire).isTrue();
    assertThat(secondAcquire).isFalse();
  }

  @Test
  void markSuccessShouldFallBackToLocalSuccessMarkerWhenRedisIsUnavailable() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(eq("mq:consumed:order:evt-3"), eq("PROCESSING"), any()))
        .thenThrow(new IllegalStateException("redis down"));
    doThrow(new IllegalStateException("redis down"))
        .when(valueOperations)
        .set(eq("mq:consumed:order:evt-3"), eq("SUCCESS"), any());

    assertThat(service.tryAcquire("order", "evt-3")).isTrue();

    service.markSuccess("order", "evt-3");

    assertThat(service.tryAcquire("order", "evt-3")).isFalse();
  }

  @Test
  void releaseShouldClearLocalFallbackState() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(eq("mq:consumed:order:evt-4"), eq("PROCESSING"), any()))
        .thenThrow(new IllegalStateException("redis down"));

    assertThat(service.tryAcquire("order", "evt-4")).isTrue();

    service.release("order", "evt-4");

    assertThat(service.tryAcquire("order", "evt-4")).isTrue();
  }
}
