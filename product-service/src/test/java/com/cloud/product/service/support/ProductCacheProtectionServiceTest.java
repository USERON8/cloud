package com.cloud.product.service.support;

import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.product.mapper.ProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCacheProtectionServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ObjectProvider<RedissonClient> redissonClientProvider;

    @Mock
    private RBloomFilter<String> bloomFilter;

    private ProductCacheProtectionService service;

    @BeforeEach
    void setUp() {
        service = new ProductCacheProtectionService(
                stringRedisTemplate,
                new ObjectMapper(),
                productMapper,
                redissonClientProvider
        );
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        ReflectionTestUtils.setField(service, "guardEnabled", true);
        ReflectionTestUtils.setField(service, "bloomEnabled", true);
        ReflectionTestUtils.setField(service, "bloomFilter", bloomFilter);
        ReflectionTestUtils.setField(service, "detailTtlSeconds", 1800L);
        ReflectionTestUtils.setField(service, "detailJitterSeconds", 300L);
        ReflectionTestUtils.setField(service, "nullTtlSeconds", 90L);
        ReflectionTestUtils.setField(service, "nullJitterSeconds", 30L);
        ReflectionTestUtils.setField(service, "lockWaitMillis", 120L);
        ReflectionTestUtils.setField(service, "lockLeaseMillis", 3000L);
        ReflectionTestUtils.setField(service, "lockRetryTimes", 1);
    }

    @Test
    void shouldReturnCachedValueWithoutCallingLoader() throws Exception {
        ProductVO vo = new ProductVO();
        vo.setId(200L);
        vo.setName("demo");
        vo.setPrice(BigDecimal.valueOf(12.5));
        String payload = new ObjectMapper().writeValueAsString(vo);

        when(bloomFilter.contains("200")).thenReturn(true);
        when(valueOperations.get("product:detail:200")).thenReturn(payload);

        AtomicBoolean invoked = new AtomicBoolean(false);
        Optional<ProductVO> result = service.queryProductById(200L, () -> {
            invoked.set(true);
            return null;
        });

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(200L);
        assertThat(invoked).isFalse();
    }

    @Test
    void shouldCacheNullWhenBloomFilterRejectsId() {
        when(bloomFilter.contains("404")).thenReturn(false);

        Optional<ProductVO> result = service.queryProductById(404L, () -> {
            throw new AssertionError("loader should not be called when bloom rejects");
        });

        assertThat(result).isEmpty();
        verify(valueOperations).set(
                eq("product:detail:404"),
                eq("__NULL__"),
                longThat(ttl -> ttl >= 90 && ttl <= 121),
                eq(TimeUnit.SECONDS)
        );
        verify(redissonClientProvider, never()).getIfAvailable();
    }
}
