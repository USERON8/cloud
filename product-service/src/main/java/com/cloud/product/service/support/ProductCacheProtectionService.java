package com.cloud.product.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.product.mapper.ProductMapper;
import com.cloud.product.module.entity.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheProtectionService {

    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:detail:";
    private static final String PRODUCT_CACHE_LOCK_PREFIX = "product:detail:rebuild:lock:";
    private static final String PRODUCT_ID_BLOOM_KEY = "product:id:bloom";
    private static final String NULL_CACHE_MARKER = "__NULL__";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductMapper productMapper;
    private final ObjectProvider<RedissonClient> redissonClientProvider;

    @Value("${product.cache.guard.enabled:true}")
    private boolean guardEnabled;

    @Value("${product.cache.guard.bloom.enabled:true}")
    private boolean bloomEnabled;

    @Value("${product.cache.guard.bloom.rebuild-on-startup:true}")
    private boolean rebuildBloomOnStartup;

    @Value("${product.cache.guard.bloom.expected-insertions:500000}")
    private long bloomExpectedInsertions;

    @Value("${product.cache.guard.bloom.false-positive-rate:0.01}")
    private double bloomFalsePositiveRate;

    @Value("${product.cache.guard.detail-ttl-seconds:1800}")
    private long detailTtlSeconds;

    @Value("${product.cache.guard.detail-jitter-seconds:300}")
    private long detailJitterSeconds;

    @Value("${product.cache.guard.null-ttl-seconds:90}")
    private long nullTtlSeconds;

    @Value("${product.cache.guard.null-jitter-seconds:30}")
    private long nullJitterSeconds;

    @Value("${product.cache.guard.lock.wait-millis:120}")
    private long lockWaitMillis;

    @Value("${product.cache.guard.lock.lease-millis:3000}")
    private long lockLeaseMillis;

    @Value("${product.cache.guard.lock.retry-times:2}")
    private int lockRetryTimes;

    private RBloomFilter<String> bloomFilter;

    @PostConstruct
    public void initBloomFilter() {
        if (!guardEnabled || !bloomEnabled) {
            return;
        }

        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient == null) {
            log.warn("RedissonClient not available, bloom filter cache guard will be skipped");
            return;
        }

        bloomFilter = redissonClient.getBloomFilter(PRODUCT_ID_BLOOM_KEY);
        try {
            if (!bloomFilter.isExists()) {
                bloomFilter.tryInit(Math.max(10_000, bloomExpectedInsertions), bloomFalsePositiveRate);
            }
            if (rebuildBloomOnStartup) {
                rebuildBloomFilter();
            }
        } catch (Exception e) {
            log.warn("Initialize product bloom filter failed", e);
        }
    }

    public Optional<ProductVO> queryProductById(Long productId, Supplier<ProductVO> dbLoader) {
        if (!guardEnabled) {
            return Optional.ofNullable(dbLoader.get());
        }
        if (productId == null || productId <= 0) {
            return Optional.empty();
        }

        String productIdStr = String.valueOf(productId);
        String redisKey = PRODUCT_CACHE_KEY_PREFIX + productIdStr;

        if (isBloomFilterSurelyNotExist(productIdStr)) {
            cacheNull(redisKey);
            return Optional.empty();
        }

        CacheLookupResult cacheResult = readFromCache(redisKey);
        if (cacheResult.hit()) {
            return Optional.ofNullable(cacheResult.value());
        }

        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient == null) {
            return loadAndWriteCache(redisKey, productIdStr, dbLoader);
        }

        String lockKey = PRODUCT_CACHE_LOCK_PREFIX + productIdStr;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(Math.max(10L, lockWaitMillis), Math.max(500L, lockLeaseMillis), TimeUnit.MILLISECONDS);
            if (locked) {
                CacheLookupResult secondRead = readFromCache(redisKey);
                if (secondRead.hit()) {
                    return Optional.ofNullable(secondRead.value());
                }
                return loadAndWriteCache(redisKey, productIdStr, dbLoader);
            }

            int retries = Math.max(1, lockRetryTimes);
            long waitSlice = Math.max(20L, lockWaitMillis / retries);
            for (int i = 0; i < retries; i++) {
                sleepQuietly(waitSlice);
                CacheLookupResult retryRead = readFromCache(redisKey);
                if (retryRead.hit()) {
                    return Optional.ofNullable(retryRead.value());
                }
            }

            return loadAndWriteCache(redisKey, productIdStr, dbLoader);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.ofNullable(dbLoader.get());
        } catch (Exception e) {
            log.warn("Query product cache with lock failed, id={}", productId, e);
            return Optional.ofNullable(dbLoader.get());
        } finally {
            if (locked) {
                try {
                    lock.unlock();
                } catch (Exception unlockEx) {
                    log.warn("Unlock product cache rebuild lock failed, key={}", lockKey, unlockEx);
                }
            }
        }
    }

    public void markProductExists(Long productId) {
        if (!guardEnabled || !bloomEnabled || productId == null || productId <= 0 || bloomFilter == null) {
            return;
        }
        try {
            bloomFilter.add(String.valueOf(productId));
        } catch (Exception e) {
            log.warn("Add product id to bloom filter failed, id={}", productId, e);
        }
    }

    public void markProductsExist(Collection<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return;
        }
        for (Long productId : productIds) {
            markProductExists(productId);
        }
    }

    private Optional<ProductVO> loadAndWriteCache(String redisKey, String productIdStr, Supplier<ProductVO> dbLoader) {
        ProductVO value = dbLoader.get();
        if (value == null) {
            cacheNull(redisKey);
            return Optional.empty();
        }
        cacheValue(redisKey, value);
        markProductExists(Long.valueOf(productIdStr));
        return Optional.of(value);
    }

    private boolean isBloomFilterSurelyNotExist(String productIdStr) {
        if (!bloomEnabled || bloomFilter == null) {
            return false;
        }
        try {
            return !bloomFilter.contains(productIdStr);
        } catch (Exception e) {
            log.warn("Bloom filter contains check failed, id={}", productIdStr, e);
            return false;
        }
    }

    private CacheLookupResult readFromCache(String redisKey) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(redisKey);
            if (cached == null) {
                return CacheLookupResult.miss();
            }
            if (NULL_CACHE_MARKER.equals(cached)) {
                return CacheLookupResult.hit(null);
            }
            ProductVO value = objectMapper.readValue(cached, ProductVO.class);
            return CacheLookupResult.hit(value);
        } catch (Exception e) {
            stringRedisTemplate.delete(redisKey);
            log.warn("Read product cache failed, key={}", redisKey, e);
            return CacheLookupResult.miss();
        }
    }

    private void cacheValue(String redisKey, ProductVO value) {
        try {
            String serialized = objectMapper.writeValueAsString(value);
            long ttl = ttlWithJitter(detailTtlSeconds, detailJitterSeconds);
            stringRedisTemplate.opsForValue().set(redisKey, serialized, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Write product value cache failed, key={}", redisKey, e);
        }
    }

    private void cacheNull(String redisKey) {
        long ttl = ttlWithJitter(nullTtlSeconds, nullJitterSeconds);
        stringRedisTemplate.opsForValue().set(redisKey, NULL_CACHE_MARKER, ttl, TimeUnit.SECONDS);
    }

    private long ttlWithJitter(long base, long jitter) {
        long safeBase = Math.max(10L, base);
        long safeJitter = Math.max(1L, jitter);
        return safeBase + ThreadLocalRandom.current().nextLong(safeJitter + 1);
    }

    private void rebuildBloomFilter() {
        if (bloomFilter == null) {
            return;
        }
        try {
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(Product::getId);
            List<Product> products = productMapper.selectList(wrapper);
            if (CollectionUtils.isEmpty(products)) {
                return;
            }
            for (Product product : products) {
                if (product.getId() != null && product.getId() > 0) {
                    bloomFilter.add(String.valueOf(product.getId()));
                }
            }
            log.info("Product bloom filter rebuilt, size={}", products.size());
        } catch (Exception e) {
            log.warn("Rebuild product bloom filter failed", e);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record CacheLookupResult(boolean hit, ProductVO value) {
        private static CacheLookupResult miss() {
            return new CacheLookupResult(false, null);
        }

        private static CacheLookupResult hit(ProductVO value) {
            return new CacheLookupResult(true, value);
        }
    }
}
