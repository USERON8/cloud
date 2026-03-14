package com.cloud.product.service.support;

import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductDetailCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private ProductDetailCacheService productDetailCacheService;

    @BeforeEach
    void setUp() {
        productDetailCacheService = new ProductDetailCacheService(redisTemplate);
        productDetailCacheService.init();
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void getOrLoad_readsFromHashCache() {
        SpuDetailVO base = new SpuDetailVO();
        base.setSpuId(10L);
        base.setSpuName("spu");
        List<SkuDetailVO> skus = List.of(new SkuDetailVO());

        when(hashOperations.get("product:detail:10", "spu")).thenReturn(base);
        when(hashOperations.get("product:detail:10", "skus")).thenReturn(skus);

        AtomicInteger loaderCalled = new AtomicInteger();
        SpuDetailVO result = productDetailCacheService.getOrLoad(10L, () -> {
            loaderCalled.incrementAndGet();
            return null;
        });

        assertThat(result).isNotNull();
        assertThat(result.getSkus()).hasSize(1);
        assertThat(loaderCalled.get()).isEqualTo(0);
    }

    @Test
    void evict_removesRedisKey() {
        productDetailCacheService.evict(11L);

        verify(redisTemplate).delete("product:detail:11");
    }
}
