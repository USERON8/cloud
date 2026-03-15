package com.cloud.payment.service.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSecurityCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private PaymentSecurityCacheService paymentSecurityCacheService;

    @Test
    void tryAcquireIdempotent_blankOrderKey_returnsTrue() {
        boolean result = paymentSecurityCacheService.tryAcquireIdempotent(" ", "key");
        assertThat(result).isTrue();
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void cacheStatus_finalStatus_ignored() {
        paymentSecurityCacheService.cacheStatus("pay1", 1L, "PAID");

        verify(stringRedisTemplate, never()).delete(org.mockito.ArgumentMatchers.<String>any());
        verify(hashOperations, never()).putAll(any(), any(Map.class));
    }

    @Test
    void cacheStatus_nonFinal_writesHash() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        paymentSecurityCacheService.cacheStatus("pay2", 2L, "CREATED");

        verify(stringRedisTemplate).delete(org.mockito.ArgumentMatchers.<String>eq("pay:status:pay2"));
        verify(hashOperations).putAll(eq("pay:status:pay2"), any(Map.class));
        verify(stringRedisTemplate).expire(eq("pay:status:pay2"), any(Duration.class));
    }

    @Test
    void allowRateLimit_scriptAllows_returnsTrue() {
        ReflectionTestUtils.setField(paymentSecurityCacheService, "rateLimitEnabled", true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("pay:rate:9")).thenReturn(1L);

        boolean allowed = paymentSecurityCacheService.allowRateLimit(9L);

        assertThat(allowed).isTrue();
    }

    @Test
    void getCachedResult_parsesValue() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("pay:result:order1")).thenReturn("123");

        Long result = paymentSecurityCacheService.getCachedResult("order1");

        assertThat(result).isEqualTo(123L);
    }
}
