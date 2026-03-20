package com.cloud.product.service.support;

import com.cloud.common.domain.vo.product.SkuDetailVO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductDetailCacheService {

  private static final String KEY_PREFIX = "product:detail:";
  private static final String HASH_FIELD_SPU = "spu";
  private static final String HASH_FIELD_SKUS = "skus";

  private final RedisTemplate<String, Object> redisTemplate;

  @Value("${product.cache.guard.detail-ttl-seconds:1800}")
  private long detailTtlSeconds;

  @Value("${product.cache.guard.detail-jitter-seconds:300}")
  private long detailJitterSeconds;

  @Value("${product.cache.guard.l1-max-size:2000}")
  private long l1MaxSize;

  private Cache<Long, SpuDetailVO> l1Cache;

  public ProductDetailCacheService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @PostConstruct
  public void init() {
    long safeMax = Math.max(100L, l1MaxSize);
    long ttlSeconds = Math.max(60L, detailTtlSeconds);
    this.l1Cache =
        Caffeine.newBuilder()
            .maximumSize(safeMax)
            .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
            .build();
  }

  public SpuDetailVO getOrLoad(Long spuId, Supplier<SpuDetailVO> loader) {
    if (spuId == null) {
      return null;
    }
    SpuDetailVO cached = l1Cache.getIfPresent(spuId);
    if (cached != null) {
      return cached;
    }

    String key = buildKey(spuId);
    try {
      SpuDetailVO cachedVo = readFromHash(key);
      if (cachedVo != null) {
        l1Cache.put(spuId, cachedVo);
        return cachedVo;
      }
    } catch (Exception ex) {
      log.warn("Read product detail cache failed: spuId={}", spuId, ex);
    }

    SpuDetailVO loaded = loader.get();
    if (loaded == null) {
      return null;
    }

    l1Cache.put(spuId, loaded);
    cacheToHash(key, loaded);
    return loaded;
  }

  public void evict(Long spuId) {
    if (spuId == null) {
      return;
    }
    l1Cache.invalidate(spuId);
    try {
      redisTemplate.delete(buildKey(spuId));
    } catch (Exception ex) {
      log.warn("Evict product detail cache failed: spuId={}", spuId, ex);
    }
  }

  private String buildKey(Long spuId) {
    return KEY_PREFIX + spuId;
  }

  private SpuDetailVO readFromHash(String key) {
    try {
      Object baseValue = redisTemplate.opsForHash().get(key, HASH_FIELD_SPU);
      if (!(baseValue instanceof SpuDetailVO base)) {
        return null;
      }
      Object skusValue = redisTemplate.opsForHash().get(key, HASH_FIELD_SKUS);
      List<SkuDetailVO> skus = null;
      if (skusValue instanceof List<?> list) {
        @SuppressWarnings("unchecked")
        List<SkuDetailVO> casted = (List<SkuDetailVO>) list;
        skus = casted;
      }
      SpuDetailVO merged = copyBase(base);
      merged.setSkus(skus);
      return merged;
    } catch (Exception ex) {
      log.warn("Read product detail hash cache failed: key={}", key, ex);
      return null;
    }
  }

  private void cacheToHash(String key, SpuDetailVO loaded) {
    if (loaded == null) {
      return;
    }
    try {
      long ttlSeconds = addJitter(detailTtlSeconds, detailJitterSeconds);
      SpuDetailVO base = copyBase(loaded);
      List<SkuDetailVO> skus =
          loaded.getSkus() == null ? Collections.emptyList() : loaded.getSkus();
      Map<String, Object> payload = new HashMap<>(4);
      payload.put(HASH_FIELD_SPU, base);
      payload.put(HASH_FIELD_SKUS, skus);
      redisTemplate.delete(key);
      redisTemplate.opsForHash().putAll(key, payload);
      redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    } catch (Exception ex) {
      log.warn("Write product detail hash cache failed: key={}", key, ex);
    }
  }

  private SpuDetailVO copyBase(SpuDetailVO source) {
    SpuDetailVO target = new SpuDetailVO();
    target.setSpuId(source.getSpuId());
    target.setSpuName(source.getSpuName());
    target.setSubtitle(source.getSubtitle());
    target.setCategoryId(source.getCategoryId());
    target.setCategoryName(source.getCategoryName());
    target.setBrandId(source.getBrandId());
    target.setBrandName(source.getBrandName());
    target.setMerchantId(source.getMerchantId());
    target.setStatus(source.getStatus());
    target.setDescription(source.getDescription());
    target.setMainImage(source.getMainImage());
    target.setTags(source.getTags());
    target.setRating(source.getRating());
    target.setReviewCount(source.getReviewCount());
    target.setRecommended(source.getRecommended());
    target.setIsHot(source.getIsHot());
    target.setCreatedAt(source.getCreatedAt());
    target.setUpdatedAt(source.getUpdatedAt());
    target.setSkus(null);
    return target;
  }

  private long addJitter(long baseSeconds, long jitterSeconds) {
    long safeBase = Math.max(30L, baseSeconds);
    long safeJitter = Math.max(0L, jitterSeconds);
    if (safeJitter <= 0L) {
      return safeBase;
    }
    return safeBase + ThreadLocalRandom.current().nextLong(safeJitter + 1);
  }
}
