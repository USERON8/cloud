package com.cloud.product.service.support;

import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
public class ProductDetailCacheService {

    private static final String KEY_PREFIX = "product:detail:";

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
        this.l1Cache = Caffeine.newBuilder()
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
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof SpuDetailVO cachedVo) {
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
        try {
            long ttlSeconds = addJitter(detailTtlSeconds, detailJitterSeconds);
            redisTemplate.opsForValue().set(key, loaded, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("Write product detail cache failed: spuId={}", spuId, ex);
        }
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

    private long addJitter(long baseSeconds, long jitterSeconds) {
        long safeBase = Math.max(30L, baseSeconds);
        long safeJitter = Math.max(0L, jitterSeconds);
        if (safeJitter <= 0L) {
            return safeBase;
        }
        return safeBase + ThreadLocalRandom.current().nextLong(safeJitter + 1);
    }
}
