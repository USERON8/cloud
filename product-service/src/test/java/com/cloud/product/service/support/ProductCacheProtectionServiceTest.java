package com.cloud.product.service.support;

import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.product.mapper.ProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private ObjectProvider<CacheManager> cacheManagerProvider;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache productCache;

    @Mock
    private Cache productListCache;

    @Mock
    private Cache productStatsCache;

    private ProductCacheProtectionService service;

    @BeforeEach
    void setUp() {
        service = new ProductCacheProtectionService(
                stringRedisTemplate,
                new ObjectMapper(),
                productMapper,
                redissonClientProvider,
                cacheManagerProvider
        );
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(cacheManagerProvider.getIfAvailable()).thenReturn(cacheManager);
        lenient().when(cacheManager.getCache("productCache")).thenReturn(productCache);
        lenient().when(cacheManager.getCache("productListCache")).thenReturn(productListCache);
        lenient().when(cacheManager.getCache("productStatsCache")).thenReturn(productStatsCache);

        ReflectionTestUtils.setField(service, "guardEnabled", true);
        ReflectionTestUtils.setField(service, "bloomEnabled", true);
        ReflectionTestUtils.setField(service, "bloomFilter", bloomFilter);
        ReflectionTestUtils.setField(service, "detailTtlSeconds", 1800L);
        ReflectionTestUtils.setField(service, "detailJitterSeconds", 300L);
        ReflectionTestUtils.setField(service, "nullTtlSeconds", 90L);
        ReflectionTestUtils.setField(service, "nullJitterSeconds", 30L);
        ReflectionTestUtils.setField(service, "listTtlSeconds", 900L);
        ReflectionTestUtils.setField(service, "listJitterSeconds", 180L);
        ReflectionTestUtils.setField(service, "statsTtlSeconds", 300L);
        ReflectionTestUtils.setField(service, "statsJitterSeconds", 60L);
        ReflectionTestUtils.setField(service, "lockWaitMillis", 120L);
        ReflectionTestUtils.setField(service, "lockLeaseMillis", 3000L);
        ReflectionTestUtils.setField(service, "lockRetryTimes", 1);
        ReflectionTestUtils.setField(service, "pubsubEnabled", true);
        ReflectionTestUtils.setField(service, "pubsubChannel", "product:cache:invalidate");
        ReflectionTestUtils.setField(service, "localNodeId", "node-a");
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

    @Test
    void shouldEvictRedisBeforeLocalCaffeine() {
        service.evictProductCaches(88L);

        InOrder inOrder = inOrder(stringRedisTemplate, productCache, productListCache, productStatsCache);
        inOrder.verify(stringRedisTemplate).delete("product:detail:88");
        inOrder.verify(stringRedisTemplate).convertAndSend(eq("product:cache:invalidate"), any(String.class));
        inOrder.verify(productCache).evict(88L);
        inOrder.verify(productListCache).clear();
        inOrder.verify(productStatsCache).clear();
    }

    @Test
    void shouldEvictLocalCacheWhenReceiveRemoteEvent() {
        String payload = "{\"nodeId\":\"node-b\",\"clearAll\":false,\"productIds\":[88],\"timestamp\":1}";

        service.handleLocalCacheInvalidationMessage(payload);

        verify(productCache).evict(88L);
        verify(productListCache).clear();
        verify(productStatsCache).clear();
    }

    @Test
    void shouldIgnoreLocalNodeEvent() {
        String payload = "{\"nodeId\":\"node-a\",\"clearAll\":false,\"productIds\":[88],\"timestamp\":1}";

        service.handleLocalCacheInvalidationMessage(payload);

        verify(productCache, never()).evict(88L);
        verify(productListCache, never()).clear();
        verify(productStatsCache, never()).clear();
    }

    @Test
    void shouldPreloadDetailIntoRedisAndCaffeine() {
        ProductVO vo = new ProductVO();
        vo.setId(9L);
        vo.setName("warmup");

        service.preloadProductDetailCache(9L, vo);

        verify(valueOperations).set(
                eq("product:detail:9"),
                any(String.class),
                longThat(ttl -> ttl >= 1800 && ttl <= 2101),
                eq(TimeUnit.SECONDS)
        );
        verify(productCache).put(9L, vo);
    }

    @Test
    void shouldPreloadListAndStatsIntoRedisAndCaffeine() {
        ProductVO vo = new ProductVO();
        vo.setId(7L);
        vo.setName("hot");

        service.preloadProductListCache("hot:products", List.of(vo));
        service.preloadProductStatsCache("home:snapshot", Map.of("enabledCount", 1));

        verify(valueOperations).set(
                eq("product:list:hot:products"),
                any(String.class),
                longThat(ttl -> ttl >= 900 && ttl <= 1081),
                eq(TimeUnit.SECONDS)
        );
        verify(productListCache).put(eq("hot:products"), any());

        verify(valueOperations).set(
                eq("product:stats:home:snapshot"),
                any(String.class),
                longThat(ttl -> ttl >= 300 && ttl <= 361),
                eq(TimeUnit.SECONDS)
        );
        verify(productStatsCache).put(eq("home:snapshot"), any());
    }
}
