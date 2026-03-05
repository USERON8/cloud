package com.cloud.product.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.vo.product.ProductVO;
import com.cloud.common.utils.RedisKeyScanUtils;
import com.cloud.product.mapper.ProductMapper;
import com.cloud.product.module.entity.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheProtectionService {

    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:detail:";
    private static final String PRODUCT_CACHE_LOCK_PREFIX = "product:detail:rebuild:lock:";
    private static final String PRODUCT_CACHE_PATTERN = PRODUCT_CACHE_KEY_PREFIX + "*";
    private static final String PRODUCT_LIST_CACHE_KEY_PREFIX = "product:list:";
    private static final String PRODUCT_LIST_CACHE_PATTERN = PRODUCT_LIST_CACHE_KEY_PREFIX + "*";
    private static final String PRODUCT_STATS_CACHE_KEY_PREFIX = "product:stats:";
    private static final String PRODUCT_STATS_CACHE_PATTERN = PRODUCT_STATS_CACHE_KEY_PREFIX + "*";
    private static final String PRODUCT_ID_BLOOM_KEY = "product:id:bloom";
    private static final String NULL_CACHE_MARKER = "__NULL__";
    private static final String PRODUCT_CACHE_NAME = "productCache";
    private static final String PRODUCT_LIST_CACHE_NAME = "productListCache";
    private static final String PRODUCT_STATS_CACHE_NAME = "productStatsCache";
    private static final String DEFAULT_PUBSUB_CHANNEL = "product:cache:invalidate";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductMapper productMapper;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final ObjectProvider<CacheManager> cacheManagerProvider;

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

    @Value("${product.cache.guard.list-ttl-seconds:900}")
    private long listTtlSeconds;

    @Value("${product.cache.guard.list-jitter-seconds:180}")
    private long listJitterSeconds;

    @Value("${product.cache.guard.stats-ttl-seconds:300}")
    private long statsTtlSeconds;

    @Value("${product.cache.guard.stats-jitter-seconds:60}")
    private long statsJitterSeconds;

    @Value("${product.cache.guard.lock.wait-millis:120}")
    private long lockWaitMillis;

    @Value("${product.cache.guard.lock.lease-millis:3000}")
    private long lockLeaseMillis;

    @Value("${product.cache.guard.lock.retry-times:2}")
    private int lockRetryTimes;

    @Value("${product.cache.guard.redis.scan-count:500}")
    private long redisScanCount;

    @Value("${product.cache.guard.redis.pipeline-batch-size:200}")
    private int redisPipelineBatchSize;

    @Value("${product.cache.guard.pubsub.enabled:true}")
    private boolean pubsubEnabled;

    @Value("${product.cache.guard.pubsub.channel:" + DEFAULT_PUBSUB_CHANNEL + "}")
    private String pubsubChannel;

    @Value("${product.cache.guard.pubsub.node-id:}")
    private String localNodeId;

    private RBloomFilter<String> bloomFilter;

    @PostConstruct
    public void initBloomFilter() {
        if (!StringUtils.hasText(localNodeId)) {
            localNodeId = "product-node-" + UUID.randomUUID();
        }

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

    public void evictProductCaches(Long productId) {
        if (productId == null || productId <= 0) {
            evictAllProductCaches();
            return;
        }
        List<Long> productIds = List.of(productId);
        evictRedisProductDetail(productId);
        evictAllRedisProductListAndStats();
        publishLocalCacheInvalidation(false, productIds);
        evictLocalProductCaches(productIds);
    }

    public void evictProductCaches(Collection<Long> productIds) {
        List<Long> validIds = normalizeProductIds(productIds);
        if (CollectionUtils.isEmpty(validIds)) {
            evictAllProductCaches();
            return;
        }
        evictRedisProductDetails(validIds);
        evictAllRedisProductListAndStats();
        publishLocalCacheInvalidation(false, validIds);
        evictLocalProductCaches(validIds);
    }

    public void evictAllProductCaches() {
        evictAllRedisProductDetails();
        evictAllRedisProductListAndStats();
        publishLocalCacheInvalidation(true, List.of());
        evictAllLocalProductCaches();
    }

    public void preloadProductDetailCache(Long productId, ProductVO productVO) {
        if (productId == null || productId <= 0 || productVO == null) {
            return;
        }
        markProductExists(productId);
        cacheValue(PRODUCT_CACHE_KEY_PREFIX + productId, productVO);
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        if (cacheManager == null) {
            return;
        }
        Cache productCache = cacheManager.getCache(PRODUCT_CACHE_NAME);
        if (productCache != null) {
            productCache.put(productId, productVO);
        }
    }

    public void preloadProductListCache(String cacheKey, Object value) {
        if (!StringUtils.hasText(cacheKey) || value == null) {
            return;
        }
        String redisKey = PRODUCT_LIST_CACHE_KEY_PREFIX + cacheKey;
        cacheObject(redisKey, value, listTtlSeconds, listJitterSeconds);
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        if (cacheManager == null) {
            return;
        }
        Cache listCache = cacheManager.getCache(PRODUCT_LIST_CACHE_NAME);
        if (listCache != null) {
            listCache.put(cacheKey, value);
        }
    }

    public void preloadProductStatsCache(String cacheKey, Object value) {
        if (!StringUtils.hasText(cacheKey) || value == null) {
            return;
        }
        String redisKey = PRODUCT_STATS_CACHE_KEY_PREFIX + cacheKey;
        cacheObject(redisKey, value, statsTtlSeconds, statsJitterSeconds);
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        if (cacheManager == null) {
            return;
        }
        Cache statsCache = cacheManager.getCache(PRODUCT_STATS_CACHE_NAME);
        if (statsCache != null) {
            statsCache.put(cacheKey, value);
        }
    }

    public void handleLocalCacheInvalidationMessage(String payload) {
        if (!pubsubEnabled || !StringUtils.hasText(payload)) {
            return;
        }
        try {
            LocalCacheInvalidationEvent event = objectMapper.readValue(payload, LocalCacheInvalidationEvent.class);
            if (!isValidEvent(event)) {
                return;
            }
            if (StringUtils.hasText(localNodeId) && localNodeId.equals(event.nodeId())) {
                return;
            }
            if (event.clearAll()) {
                evictAllLocalProductCaches();
                return;
            }
            evictLocalProductCaches(event.productIds());
        } catch (Exception e) {
            log.warn("Handle product cache invalidation event failed, payload={}", payload, e);
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

    private void cacheObject(String redisKey, Object value, long baseTtl, long jitterTtl) {
        try {
            String serialized = objectMapper.writeValueAsString(value);
            long ttl = ttlWithJitter(baseTtl, jitterTtl);
            stringRedisTemplate.opsForValue().set(redisKey, serialized, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Write product object cache failed, key={}", redisKey, e);
        }
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

    private void evictRedisProductDetails(Collection<Long> productIds) {
        List<Long> validIds = normalizeProductIds(productIds);
        List<String> keys = validIds.stream()
                .map(id -> PRODUCT_CACHE_KEY_PREFIX + id)
                .toList();
        if (keys.isEmpty()) {
            return;
        }
        try {
            RedisKeyScanUtils.deleteKeysInPipeline(stringRedisTemplate, keys, redisPipelineBatchSize);
        } catch (Exception e) {
            log.warn("Evict product detail redis cache failed, keys={}", keys, e);
        }
    }

    private void evictRedisProductDetail(Long productId) {
        if (productId == null || productId <= 0) {
            return;
        }
        try {
            stringRedisTemplate.delete(PRODUCT_CACHE_KEY_PREFIX + productId);
        } catch (Exception e) {
            log.warn("Evict single product detail redis cache failed, id={}", productId, e);
        }
    }

    private void evictAllRedisProductDetails() {
        try {
            RedisKeyScanUtils.deleteByPattern(
                    stringRedisTemplate,
                    PRODUCT_CACHE_PATTERN,
                    redisScanCount,
                    redisPipelineBatchSize
            );
        } catch (Exception e) {
            log.warn("Evict all product detail redis cache failed, pattern={}", PRODUCT_CACHE_PATTERN, e);
        }
    }

    private void evictAllRedisProductListAndStats() {
        try {
            RedisKeyScanUtils.deleteByPattern(
                    stringRedisTemplate,
                    PRODUCT_LIST_CACHE_PATTERN,
                    redisScanCount,
                    redisPipelineBatchSize
            );
            RedisKeyScanUtils.deleteByPattern(
                    stringRedisTemplate,
                    PRODUCT_STATS_CACHE_PATTERN,
                    redisScanCount,
                    redisPipelineBatchSize
            );
        } catch (Exception e) {
            log.warn("Evict product list/stats redis cache failed", e);
        }
    }

    private void evictLocalProductCaches(Collection<Long> productIds) {
        List<Long> validIds = normalizeProductIds(productIds);
        if (validIds.isEmpty()) {
            return;
        }
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        if (cacheManager == null) {
            return;
        }
        Cache productCache = cacheManager.getCache(PRODUCT_CACHE_NAME);
        if (productCache != null) {
            for (Long productId : validIds) {
                productCache.evict(productId);
            }
        }
        clearCache(cacheManager.getCache(PRODUCT_LIST_CACHE_NAME));
        clearCache(cacheManager.getCache(PRODUCT_STATS_CACHE_NAME));
    }

    private void evictAllLocalProductCaches() {
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        if (cacheManager == null) {
            return;
        }
        clearCache(cacheManager.getCache(PRODUCT_CACHE_NAME));
        clearCache(cacheManager.getCache(PRODUCT_LIST_CACHE_NAME));
        clearCache(cacheManager.getCache(PRODUCT_STATS_CACHE_NAME));
    }

    private void clearCache(Cache cache) {
        if (cache == null) {
            return;
        }
        try {
            cache.clear();
        } catch (Exception e) {
            log.warn("Clear local cache failed, cache={}", cache.getName(), e);
        }
    }

    private void publishLocalCacheInvalidation(boolean clearAll, Collection<Long> productIds) {
        if (!pubsubEnabled) {
            return;
        }
        try {
            List<Long> validIds = clearAll ? List.of() : normalizeProductIds(productIds);
            LocalCacheInvalidationEvent event = new LocalCacheInvalidationEvent(
                    localNodeId,
                    clearAll,
                    validIds,
                    System.currentTimeMillis()
            );
            String payload = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(resolvePubsubChannel(), payload);
        } catch (Exception e) {
            log.warn("Publish product cache invalidation event failed", e);
        }
    }

    private String resolvePubsubChannel() {
        if (!StringUtils.hasText(pubsubChannel)) {
            return DEFAULT_PUBSUB_CHANNEL;
        }
        return pubsubChannel;
    }

    private List<Long> normalizeProductIds(Collection<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return List.of();
        }
        List<Long> validIds = new ArrayList<>();
        for (Long productId : productIds) {
            if (productId != null && productId > 0 && !validIds.contains(productId)) {
                validIds.add(productId);
            }
        }
        return validIds;
    }

    private boolean isValidEvent(LocalCacheInvalidationEvent event) {
        if (event == null) {
            return false;
        }
        if (event.clearAll()) {
            return true;
        }
        return !CollectionUtils.isEmpty(event.productIds());
    }

    private record LocalCacheInvalidationEvent(String nodeId, boolean clearAll, List<Long> productIds, long timestamp) {
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
