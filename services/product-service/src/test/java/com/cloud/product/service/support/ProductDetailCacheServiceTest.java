package com.cloud.product.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.product.converter.ProductDetailConverter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class ProductDetailCacheServiceTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private HashOperations<String, Object, Object> hashOperations;
  @Mock private TaskScheduler taskScheduler;
  @Mock private ProductDetailConverter productDetailConverter;

  private ProductDetailCacheService productDetailCacheService;

  @BeforeEach
  void setUp() {
    lenient()
        .when(productDetailConverter.copyBase(org.mockito.ArgumentMatchers.any(SpuDetailVO.class)))
        .thenAnswer(
            invocation -> {
              SpuDetailVO source = invocation.getArgument(0);
              SpuDetailVO target = new SpuDetailVO();
              target.setSpuId(source.getSpuId());
              target.setSpuName(source.getSpuName());
              return target;
            });
    productDetailCacheService =
        new ProductDetailCacheService(redisTemplate, taskScheduler, productDetailConverter);
    productDetailCacheService.init();
  }

  @Test
  void getOrLoad_readsFromHashCache() {
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);

    SpuDetailVO base = new SpuDetailVO();
    base.setSpuId(10L);
    base.setSpuName("spu");
    List<SkuDetailVO> skus = List.of(new SkuDetailVO());

    when(hashOperations.get("product:detail:10", "spu")).thenReturn(base);
    when(hashOperations.get("product:detail:10", "skus")).thenReturn(skus);

    AtomicInteger loaderCalled = new AtomicInteger();
    SpuDetailVO result =
        productDetailCacheService.getOrLoad(
            10L,
            () -> {
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
