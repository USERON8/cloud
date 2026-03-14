package com.cloud.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.cloud.search.dto.ProductSearchRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;
import com.cloud.search.service.support.HotKeywordKeys;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings("unchecked")
public class ElasticsearchOptimizedService {

    private static final String PRODUCT_INDEX = "product_index";
    private static final String SEARCH_CACHE_KEY_PREFIX = "search:smart:";
    private static final String SUGGESTION_CACHE_KEY_PREFIX = "search:suggest:";
    private static final String HOT_CACHE_KEY_PREFIX = "search:hot:list:";
    private static final String RECOMMEND_CACHE_KEY_PREFIX = "search:recommend:";
    private static final String EMPTY_LIST_CACHE_MARKER = "__EMPTY__";
    private static final String METRIC_SEARCH_LATENCY = "search.request.latency";
    private static final String METRIC_ES_ERROR = "search.es.error.count";
    private static final String CACHE_REBUILD_LOCK_PREFIX = "search:cache:rebuild:lock:";
    private static final String CACHE_NAME_SMART = "search.smart";
    private static final String CACHE_NAME_SUGGESTIONS = "search.suggestions";
    private static final String CACHE_NAME_HOT = "search.hotKeywords";
    private static final String CACHE_NAME_RECOMMEND = "search.recommendations";

    @Value("${search.optimized.cache.smart-search.l2-ttl-seconds:120}")
    private long smartSearchL2TtlSeconds;

    @Value("${search.optimized.cache.suggestions.l2-ttl-seconds:120}")
    private long suggestionL2TtlSeconds;

    @Value("${search.optimized.cache.hot-keywords.l2-ttl-seconds:30}")
    private long hotKeywordsL2TtlSeconds;

    @Value("${search.optimized.cache.recommendations.l2-ttl-seconds:60}")
    private long recommendationL2TtlSeconds;

    @Value("${search.optimized.cache.smart-search.l1-expire-after-write-ms:${search.optimized.cache.smart-search.l1-ttl-millis:30000}}")
    private long smartSearchL1ExpireAfterWriteMs;

    @Value("${search.optimized.cache.suggestions.l1-expire-after-write-ms:${search.optimized.cache.suggestions.l1-ttl-millis:20000}}")
    private long suggestionL1ExpireAfterWriteMs;

    @Value("${search.optimized.cache.hot-keywords.l1-expire-after-write-ms:${search.optimized.cache.hot-keywords.l1-ttl-millis:15000}}")
    private long hotKeywordsL1ExpireAfterWriteMs;

    @Value("${search.optimized.cache.recommendations.l1-expire-after-write-ms:${search.optimized.cache.recommendations.l1-ttl-millis:20000}}")
    private long recommendationL1ExpireAfterWriteMs;

    @Value("${search.optimized.cache.smart-search.l1-refresh-after-write-ms:10000}")
    private long smartSearchL1RefreshAfterWriteMs;

    @Value("${search.optimized.cache.suggestions.l1-refresh-after-write-ms:8000}")
    private long suggestionL1RefreshAfterWriteMs;

    @Value("${search.optimized.cache.hot-keywords.l1-refresh-after-write-ms:5000}")
    private long hotKeywordsL1RefreshAfterWriteMs;

    @Value("${search.optimized.cache.recommendations.l1-refresh-after-write-ms:8000}")
    private long recommendationL1RefreshAfterWriteMs;

    @Value("${search.optimized.cache.record-stats:true}")
    private boolean l1RecordStats;

    @Value("${search.optimized.limit.default-search-size:20}")
    private int defaultSearchSize;

    @Value("${search.optimized.limit.default-keyword-size:10}")
    private int defaultKeywordSize;

    @Value("${search.optimized.limit.max-search-size:100}")
    private int maxSearchSize;

    @Value("${search.optimized.limit.max-from:10000}")
    private int maxSearchFrom;

    @Value("${search.optimized.elasticsearch.request-timeout-ms:700}")
    private int esRequestTimeoutMs;

    @Value("${search.optimized.cache.l1-max-entries:5000}")
    private int l1MaxEntries;

    @Value("${search.optimized.hot-keyword.expire-refresh-seconds:60}")
    private long hotKeywordExpireRefreshSeconds;

    @Value("${search.optimized.hot-keyword.cache-invalidate-interval-ms:3000}")
    private long hotKeywordCacheInvalidateIntervalMs;

    @Value("${search.hot-keyword.daily-ttl-days:7}")
    private long hotKeywordDailyTtlDays;

    @Value("${search.optimized.cache.lock.wait-ms:120}")
    private long cacheLockWaitMs;

    @Value("${search.optimized.cache.lock.lease-ms:3000}")
    private long cacheLockLeaseMs;

    @Value("${search.optimized.cache.lock.retry-times:2}")
    private int cacheLockRetryTimes;

    private final ElasticsearchClient elasticsearchClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Executor cacheRefreshExecutor;

    private LoadingCache<SmartSearchCacheKey, SearchResult> l1SmartSearchCache;
    private LoadingCache<KeywordLimitCacheKey, List<String>> l1SuggestionsCache;
    private LoadingCache<LimitCacheKey, List<String>> l1HotKeywordsCache;
    private LoadingCache<KeywordLimitCacheKey, List<String>> l1RecommendationsCache;
    private final Map<String, Timer> searchLatencyTimers = new ConcurrentHashMap<>();
    private final Map<String, Counter> esErrorCounters = new ConcurrentHashMap<>();
    private final AtomicLong hotKeywordExpireRefreshedAt = new AtomicLong(0L);
    private final AtomicReference<String> hotKeywordExpireKey = new AtomicReference<>("");
    private final AtomicLong hotKeywordCacheInvalidatedAt = new AtomicLong(0L);

    public ElasticsearchOptimizedService(ElasticsearchClient elasticsearchClient,
                                         StringRedisTemplate redisTemplate,
                                         ObjectMapper objectMapper,
                                         MeterRegistry meterRegistry,
                                         @Qualifier("searchCacheRefreshExecutor") Executor cacheRefreshExecutor) {
        this.elasticsearchClient = elasticsearchClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.cacheRefreshExecutor = cacheRefreshExecutor;
    }

    @PostConstruct
    public void initL1Caches() {
        long maxEntries = Math.max(100, l1MaxEntries);
        l1SmartSearchCache = buildLoadingCache(
                CACHE_NAME_SMART,
                maxEntries,
                smartSearchL1ExpireAfterWriteMs,
                smartSearchL1RefreshAfterWriteMs,
                this::loadSmartSearchForCache
        );
        l1SuggestionsCache = buildLoadingCache(
                CACHE_NAME_SUGGESTIONS,
                maxEntries,
                suggestionL1ExpireAfterWriteMs,
                suggestionL1RefreshAfterWriteMs,
                this::loadSuggestionsForCache
        );
        l1HotKeywordsCache = buildLoadingCache(
                CACHE_NAME_HOT,
                maxEntries,
                hotKeywordsL1ExpireAfterWriteMs,
                hotKeywordsL1RefreshAfterWriteMs,
                this::loadHotKeywordsForCache
        );
        l1RecommendationsCache = buildLoadingCache(
                CACHE_NAME_RECOMMEND,
                maxEntries,
                recommendationL1ExpireAfterWriteMs,
                recommendationL1RefreshAfterWriteMs,
                this::loadRecommendationsForCache
        );
    }

    private <K, V> LoadingCache<K, V> buildLoadingCache(String cacheName,
                                                        long maximumSize,
                                                        long expireAfterWriteMs,
                                                        long refreshAfterWriteMs,
                                                        Function<K, V> loader) {
        long safeExpire = Math.max(1_000L, expireAfterWriteMs);
        long safeRefresh = Math.max(500L, Math.min(refreshAfterWriteMs, safeExpire - 1));

        @SuppressWarnings("unchecked")
        Caffeine<K, V> builder = (Caffeine<K, V>) Caffeine.newBuilder()
                .maximumSize(Math.max(100L, maximumSize))
                .expireAfterWrite(Duration.ofMillis(safeExpire))
                .refreshAfterWrite(Duration.ofMillis(safeRefresh))
                .executor(cacheRefreshExecutor);

        if (l1RecordStats) {
            builder.recordStats();
        }

        LoadingCache<K, V> cache = builder
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> loader.apply(key), executor))
                .synchronous();
        CaffeineCacheMetrics.monitor(meterRegistry, cache, cacheName);
        return cache;
    }

    @Transactional(readOnly = true)
    public SearchResult smartProductSearch(String keyword, Long categoryId,
                                           Double minPrice, Double maxPrice,
                                           String sortField, String sortOrder,
                                           int from, int size) {
        Timer.Sample sample = Timer.start(meterRegistry);
        int safeFrom = Math.max(0, from);
        int safeSize = size <= 0 ? defaultSearchSize() : Math.min(size, maxSearchSize());
        if (safeFrom >= Math.max(1, maxSearchFrom)) {
            log.warn("Smart search deep pagination blocked, from={}, size={}, maxFrom={}", safeFrom, safeSize, maxSearchFrom);
            recordTimer(sample, "smart-search", "deep-page-blocked");
            return SearchResult.empty(safeFrom, safeSize);
        }
        String safeKeyword = normalizeKeyword(keyword);
        SmartSearchCacheKey cacheKey = new SmartSearchCacheKey(
                safeKeyword, categoryId, minPrice, maxPrice, sortField, sortOrder, safeFrom, safeSize
        );

        SearchResult l1Cached = l1SmartSearchCache.getIfPresent(cacheKey);
        if (l1Cached != null) {
            recordTimer(sample, "smart-search", "l1-hit");
            return l1SmartSearchCache.get(cacheKey);
        }

        String redisKey = buildSmartSearchCacheKey(cacheKey);
        SearchResult l2Cached = getSmartSearchFromRedis(redisKey);
        if (l2Cached != null) {
            l1SmartSearchCache.put(cacheKey, l2Cached);
            recordTimer(sample, "smart-search", "l2-hit");
            return l2Cached;
        }

        try {
            SearchResult result = querySmartSearchFromElasticsearch(cacheKey);
            putSmartSearchToRedis(redisKey, result);
            l1SmartSearchCache.put(cacheKey, result);
            recordHotSearch(safeKeyword);
            recordTimer(sample, "smart-search", "es-hit");
            return result;

        } catch (Exception e) {
            incrementEsError("smart-search");
            log.error("Smart product search failed, keyword={}, categoryId={}, from={}, size={}",
                    safeKeyword, categoryId, safeFrom, safeSize, e);
            recordTimer(sample, "smart-search", "error");
            return SearchResult.empty(safeFrom, safeSize);
        }
    }

    @Transactional(readOnly = true)
    public SearchResult smartProductSearchAfter(String keyword, Long categoryId,
                                                Double minPrice, Double maxPrice,
                                                String sortField, String sortOrder,
                                                List<Object> searchAfterValues, int size) {
        Timer.Sample sample = Timer.start(meterRegistry);
        int safeSize = size <= 0 ? defaultSearchSize() : Math.min(size, maxSearchSize());
        String safeKeyword = normalizeKeyword(keyword);
        SmartSearchCacheKey cacheKey = new SmartSearchCacheKey(
                safeKeyword, categoryId, minPrice, maxPrice, sortField, sortOrder, 0, safeSize
        );

        try {
            List<FieldValue> searchAfter = toSearchAfterValues(searchAfterValues);
            SearchResult result = querySmartSearchFromElasticsearch(cacheKey, searchAfter);
            recordHotSearch(safeKeyword);
            recordTimer(sample, "smart-search", "search-after");
            return result;

        } catch (Exception e) {
            incrementEsError("smart-search");
            log.error("Smart product search (search_after) failed, keyword={}, categoryId={}, size={}",
                    safeKeyword, categoryId, safeSize, e);
            recordTimer(sample, "smart-search", "error");
            return SearchResult.empty(0, safeSize);
        }
    }

    @Transactional(readOnly = true)
    public SearchResult productSearchAfter(ProductSearchRequest request, List<Object> searchAfterValues) {
        Timer.Sample sample = Timer.start(meterRegistry);
        ProductSearchRequest safeRequest = request == null ? new ProductSearchRequest() : request;
        int safeSize = safeRequest.getSize() == null || safeRequest.getSize() <= 0
                ? defaultSearchSize()
                : Math.min(safeRequest.getSize(), maxSearchSize());
        int safePage = safeRequest.getPage() == null ? 0 : Math.max(0, safeRequest.getPage());
        int safeFrom = safePage * safeSize;
        List<FieldValue> searchAfter = toSearchAfterValues(searchAfterValues);
        boolean useSearchAfter = searchAfter != null && !searchAfter.isEmpty();

        if (!useSearchAfter && safeFrom >= Math.max(1, maxSearchFrom)) {
            log.warn("Product search deep pagination blocked, from={}, size={}, maxFrom={}", safeFrom, safeSize, maxSearchFrom);
            recordTimer(sample, "product-search", "deep-page-blocked");
            return SearchResult.empty(safeFrom, safeSize);
        }

        Query query = buildProductSearchQuery(safeRequest);
        SearchRequest searchRequest = SearchRequest.of(s -> {
            SearchRequest.Builder builder = s
                    .index(PRODUCT_INDEX)
                    .query(query)
                    .size(safeSize)
                    .timeout(esRequestTimeoutMs + "ms")
                    .sort(useSearchAfter
                            ? buildSortOptionsWithTieBreaker(safeRequest.getSortBy(), safeRequest.getSortOrder())
                            : buildSortOptions(safeRequest.getSortBy(), safeRequest.getSortOrder()))
                    .source(src -> src.fetch(true));
            if (Boolean.TRUE.equals(safeRequest.getHighlight())) {
                builder.highlight(buildHighlight());
            }
            if (Boolean.TRUE.equals(safeRequest.getIncludeAggregations())) {
                builder.aggregations(buildAggregations());
            }
            if (useSearchAfter) {
                builder.searchAfter(searchAfter);
            } else {
                builder.from(safeFrom);
            }
            return builder;
        });

        try {
            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);
            List<Map<String, Object>> products = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> product = hit.source() != null ? new HashMap<>(hit.source()) : new HashMap<>();
                if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                    product.put("highlight", hit.highlight());
                }
                product.put("score", hit.score());
                products.add(product);
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0L;
            Map<String, Object> aggregations = Boolean.TRUE.equals(safeRequest.getIncludeAggregations())
                    ? processAggregations(response.aggregations())
                    : Map.of();
            List<Object> nextSearchAfter = resolveNextSearchAfter(response.hits().hits(), safeSize);

            if (StrUtil.isNotBlank(safeRequest.getKeyword())) {
                recordHotSearch(safeRequest.getKeyword());
            }
            recordTimer(sample, "product-search", useSearchAfter ? "search-after" : "from");
            return SearchResult.builder()
                    .documents(products)
                    .total(total)
                    .from(useSearchAfter ? 0 : safeFrom)
                    .size(safeSize)
                    .aggregations(aggregations)
                    .searchAfter(nextSearchAfter)
                    .build();
        } catch (Exception e) {
            incrementEsError("product-search");
            log.error("Product search failed, request={}, size={}", safeRequest, safeSize, e);
            recordTimer(sample, "product-search", "error");
            return SearchResult.empty(safeFrom, safeSize);
        }
    }

    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String keyword, int limit) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String safeKeyword = normalizeKeyword(keyword);
        if (StrUtil.isBlank(safeKeyword)) {
            recordTimer(sample, "suggestions", "empty");
            return List.of();
        }

        int safeLimit = normalizeKeywordLimit(limit);
        KeywordLimitCacheKey cacheKey = new KeywordLimitCacheKey(safeKeyword.toLowerCase(), safeLimit);

        List<String> l1Cached = l1SuggestionsCache.getIfPresent(cacheKey);
        if (l1Cached != null) {
            recordTimer(sample, "suggestions", "l1-hit");
            return l1SuggestionsCache.get(cacheKey);
        }

        String redisKey = buildSuggestionCacheKey(cacheKey);
        StringListCacheResult l2Cached = getStringListFromRedis(redisKey);
        if (l2Cached.hit()) {
            l1SuggestionsCache.put(cacheKey, l2Cached.value());
            recordTimer(sample, "suggestions", "l2-hit");
            return l2Cached.value();
        }

        try {
            List<String> result = querySuggestionsFromElasticsearch(cacheKey.keyword(), cacheKey.limit());
            putStringListToRedis(redisKey, result, suggestionL2TtlSeconds);
            l1SuggestionsCache.put(cacheKey, result);
            recordTimer(sample, "suggestions", "es-hit");
            return result;

        } catch (Exception e) {
            incrementEsError("suggestions");
            log.error("Get search suggestions failed, keyword={}, limit={}", safeKeyword, safeLimit, e);
            recordTimer(sample, "suggestions", "error");
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<String> getHotSearchKeywords(int limit) {
        Timer.Sample sample = Timer.start(meterRegistry);
        int safeLimit = normalizeKeywordLimit(limit);
        LimitCacheKey cacheKey = new LimitCacheKey(safeLimit);

        List<String> l1Cached = l1HotKeywordsCache.getIfPresent(cacheKey);
        if (l1Cached != null) {
            recordTimer(sample, "hot-keywords", "l1-hit");
            return l1HotKeywordsCache.get(cacheKey);
        }

        String redisKey = buildHotKeywordCacheKey(cacheKey);
        StringListCacheResult l2Cached = getStringListFromRedis(redisKey);
        if (l2Cached.hit()) {
            l1HotKeywordsCache.put(cacheKey, l2Cached.value());
            recordTimer(sample, "hot-keywords", "l2-hit");
            return l2Cached.value();
        }

        try {
            List<String> result = queryHotKeywordsFromRedis(cacheKey.limit());
            if (result.isEmpty()) {
                recordTimer(sample, "hot-keywords", "empty");
                return List.of();
            }
            putStringListToRedis(redisKey, result, hotKeywordsL2TtlSeconds);
            l1HotKeywordsCache.put(cacheKey, result);
            recordTimer(sample, "hot-keywords", "redis-hit");
            return result;
        } catch (Exception e) {
            log.error("Get hot search keywords failed, limit={}", safeLimit, e);
            recordTimer(sample, "hot-keywords", "error");
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<String> getKeywordRecommendations(String keyword, int limit) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String safeKeyword = normalizeKeyword(keyword);
        int safeLimit = normalizeKeywordLimit(limit);
        KeywordLimitCacheKey cacheKey = new KeywordLimitCacheKey(safeKeyword.toLowerCase(), safeLimit);

        List<String> l1Cached = l1RecommendationsCache.getIfPresent(cacheKey);
        if (l1Cached != null) {
            recordTimer(sample, "recommendations", "l1-hit");
            return l1RecommendationsCache.get(cacheKey);
        }

        String redisKey = buildRecommendationCacheKey(cacheKey);
        StringListCacheResult l2Cached = getStringListFromRedis(redisKey);
        if (l2Cached.hit()) {
            l1RecommendationsCache.put(cacheKey, l2Cached.value());
            recordTimer(sample, "recommendations", "l2-hit");
            return l2Cached.value();
        }

        List<String> result = computeRecommendations(cacheKey.keyword(), cacheKey.limit());
        putStringListToRedis(redisKey, result, recommendationL2TtlSeconds);
        l1RecommendationsCache.put(cacheKey, result);
        recordTimer(sample, "recommendations", "computed");
        return result;
    }

    private SearchResult loadSmartSearchForCache(SmartSearchCacheKey cacheKey) {
        String redisKey = buildSmartSearchCacheKey(cacheKey);
        return withCacheRebuildMutex(
                buildRebuildLockKey(redisKey),
                () -> getSmartSearchFromRedis(redisKey),
                () -> {
                    SearchResult l2 = getSmartSearchFromRedis(redisKey);
                    if (l2 != null) {
                        return l2;
                    }
                    SearchResult result = querySmartSearchFromElasticsearch(cacheKey);
                    putSmartSearchToRedis(redisKey, result);
                    return result;
                }
        );
    }

    private List<String> loadSuggestionsForCache(KeywordLimitCacheKey cacheKey) {
        String redisKey = buildSuggestionCacheKey(cacheKey);
        return withCacheRebuildMutex(
                buildRebuildLockKey(redisKey),
                () -> readStringListCacheIfHit(redisKey),
                () -> {
                    List<String> l2 = readStringListCacheIfHit(redisKey);
                    if (l2 != null) {
                        return l2;
                    }
                    List<String> result = querySuggestionsFromElasticsearch(cacheKey.keyword(), cacheKey.limit());
                    putStringListToRedis(redisKey, result, suggestionL2TtlSeconds);
                    return result;
                }
        );
    }

    private List<String> loadHotKeywordsForCache(LimitCacheKey cacheKey) {
        String redisKey = buildHotKeywordCacheKey(cacheKey);
        return withCacheRebuildMutex(
                buildRebuildLockKey(redisKey),
                () -> readStringListCacheIfHit(redisKey),
                () -> {
                    List<String> l2 = readStringListCacheIfHit(redisKey);
                    if (l2 != null) {
                        return l2;
                    }
                    List<String> result = queryHotKeywordsFromRedis(cacheKey.limit());
                    putStringListToRedis(redisKey, result, hotKeywordsL2TtlSeconds);
                    return result;
                }
        );
    }

    private List<String> loadRecommendationsForCache(KeywordLimitCacheKey cacheKey) {
        String redisKey = buildRecommendationCacheKey(cacheKey);
        return withCacheRebuildMutex(
                buildRebuildLockKey(redisKey),
                () -> readStringListCacheIfHit(redisKey),
                () -> {
                    List<String> l2 = readStringListCacheIfHit(redisKey);
                    if (l2 != null) {
                        return l2;
                    }
                    List<String> result = computeRecommendations(cacheKey.keyword(), cacheKey.limit());
                    putStringListToRedis(redisKey, result, recommendationL2TtlSeconds);
                    return result;
                }
        );
    }

    private SearchResult querySmartSearchFromElasticsearch(SmartSearchCacheKey key) {
        return querySmartSearchFromElasticsearch(key, List.of());
    }

    private SearchResult querySmartSearchFromElasticsearch(SmartSearchCacheKey key, List<FieldValue> searchAfter) {
        Query query = buildProductSearchQuery(key.keyword(), key.categoryId(), key.minPrice(), key.maxPrice());
        boolean useSearchAfter = searchAfter != null && !searchAfter.isEmpty();
        SearchRequest searchRequest = SearchRequest.of(s -> {
            SearchRequest.Builder builder = s
                    .index(PRODUCT_INDEX)
                    .query(query)
                    .size(key.size())
                    .timeout(esRequestTimeoutMs + "ms")
                    .sort(useSearchAfter
                            ? buildSortOptionsWithTieBreaker(key.sortField(), key.sortOrder())
                            : buildSortOptions(key.sortField(), key.sortOrder()))
                    .highlight(buildHighlight())
                    .aggregations(buildAggregations())
                    .source(src -> src.fetch(true));
            if (useSearchAfter) {
                builder.searchAfter(searchAfter);
            } else {
                builder.from(key.from());
            }
            return builder;
        });

        try {
            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            List<Map<String, Object>> products = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> product = hit.source() != null ? new HashMap<>(hit.source()) : new HashMap<>();
                if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                    product.put("highlight", hit.highlight());
                }
                product.put("score", hit.score());
                products.add(product);
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0L;
            Map<String, Object> aggregations = processAggregations(response.aggregations());
            List<Object> nextSearchAfter = resolveNextSearchAfter(response.hits().hits(), key.size());
            return SearchResult.builder()
                    .documents(products)
                    .total(total)
                    .from(key.from())
                    .size(key.size())
                    .aggregations(aggregations)
                    .searchAfter(nextSearchAfter)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("smart search query failed", ex);
        }
    }

    private List<String> querySuggestionsFromElasticsearch(String keyword, int limit) {
        Query query = Query.of(q -> q
                .multiMatch(m -> m
                        .query(keyword)
                        .fields("productName^3", "productName.pinyin^2", "categoryName", "brandName")
                        .type(TextQueryType.BoolPrefix)
                        .fuzziness("AUTO")
                )
        );

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(PRODUCT_INDEX)
                .query(query)
                .size(Math.min(limit * 2, maxSearchSize()))
                .timeout(esRequestTimeoutMs + "ms")
                .source(src -> src.filter(f -> f.includes("productName", "categoryName", "brandName")))
        );

        try {
            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);
            Set<String> suggestions = new LinkedHashSet<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source() != null ? hit.source() : Collections.emptyMap();
                addKeywordIfPresent(suggestions, source.get("productName"));
                addKeywordIfPresent(suggestions, source.get("categoryName"));
                addKeywordIfPresent(suggestions, source.get("brandName"));
                if (suggestions.size() >= limit) {
                    break;
                }
            }
            return suggestions.stream().limit(limit).toList();
        } catch (Exception ex) {
            throw new IllegalStateException("suggestions query failed", ex);
        }
    }

    private List<String> queryHotKeywordsFromRedis(int limit) {
        Set<String> hotKeywords = redisTemplate.opsForZSet().reverseRange(HotKeywordKeys.TOTAL_KEY, 0, limit - 1L);
        if (hotKeywords == null || hotKeywords.isEmpty()) {
            return List.of();
        }
        return hotKeywords.stream()
                .filter(StrUtil::isNotBlank)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<String> computeRecommendations(String keyword, int limit) {
        LinkedHashSet<String> deduplicated = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(keyword)) {
            deduplicated.addAll(getSearchSuggestions(keyword, limit));
        }

        List<String> hotKeywords = getHotSearchKeywords(Math.min(limit * 3, maxSearchSize()));
        if (StrUtil.isNotBlank(keyword)) {
            String lowered = keyword.toLowerCase();
            hotKeywords.stream()
                    .filter(item -> item.toLowerCase().contains(lowered))
                    .forEach(deduplicated::add);
        }

        hotKeywords.forEach(deduplicated::add);
        return deduplicated.stream()
                .filter(StrUtil::isNotBlank)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Query buildProductSearchQuery(String keyword, Long categoryId, Double minPrice, Double maxPrice) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (StrUtil.isNotBlank(keyword)) {
            Query keywordQuery = Query.of(q -> q.multiMatch(m -> m
                    .query(keyword)
                    .fields("productName^3", "productName.pinyin^2", "description", "categoryName", "brandName")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                    .minimumShouldMatch("75%")));
            boolQuery.must(keywordQuery);
        }

        if (categoryId != null) {
            Query categoryQuery = Query.of(q -> q.term(t -> t.field("categoryId").value(FieldValue.of(categoryId))));
            boolQuery.filter(categoryQuery);
        }

        if (minPrice != null || maxPrice != null) {
            Query priceQuery = Query.of(q -> q.range(r -> r.number(n -> {
                n.field("price");
                if (minPrice != null) {
                    n.gte(minPrice);
                }
                if (maxPrice != null) {
                    n.lte(maxPrice);
                }
                return n;
            })));
            boolQuery.filter(priceQuery);
        }

        Query statusQuery = Query.of(q -> q.term(t -> t.field("status").value(FieldValue.of(1))));
        boolQuery.filter(statusQuery);

        return Query.of(q -> q.bool(boolQuery.build()));
    }

    private Query buildProductSearchQuery(ProductSearchRequest request) {
        ProductSearchRequest safeRequest = request == null ? new ProductSearchRequest() : request;
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        String keyword = normalizeKeyword(safeRequest.getKeyword());

        if (StrUtil.isNotBlank(keyword)) {
            Query keywordQuery = Query.of(q -> q.multiMatch(m -> m
                    .query(keyword)
                    .fields("productName^3", "productName.pinyin^2", "description", "categoryName", "brandName")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                    .minimumShouldMatch("75%")));
            boolQuery.must(keywordQuery);
        }

        if (safeRequest.getCategoryId() != null) {
            boolQuery.filter(Query.of(q -> q.term(t -> t.field("categoryId").value(FieldValue.of(safeRequest.getCategoryId())))));
        }
        if (StrUtil.isNotBlank(safeRequest.getCategoryName())) {
            boolQuery.filter(Query.of(q -> q.match(m -> m.field("categoryName").query(safeRequest.getCategoryName()))));
        }

        if (safeRequest.getBrandId() != null) {
            boolQuery.filter(Query.of(q -> q.term(t -> t.field("brandId").value(FieldValue.of(safeRequest.getBrandId())))));
        }
        if (StrUtil.isNotBlank(safeRequest.getBrandName())) {
            boolQuery.filter(Query.of(q -> q.match(m -> m.field("brandName").query(safeRequest.getBrandName()))));
        }

        if (safeRequest.getShopId() != null) {
            boolQuery.filter(Query.of(q -> q.term(t -> t.field("shopId").value(FieldValue.of(safeRequest.getShopId())))));
        }
        if (StrUtil.isNotBlank(safeRequest.getShopName())) {
            boolQuery.filter(Query.of(q -> q.match(m -> m.field("shopName").query(safeRequest.getShopName()))));
        }

        Double minPrice = toDouble(safeRequest.getMinPrice());
        Double maxPrice = toDouble(safeRequest.getMaxPrice());
        if (minPrice != null || maxPrice != null) {
            boolQuery.filter(Query.of(q -> q.range(r -> r.number(n -> {
                n.field("price");
                if (minPrice != null) {
                    n.gte(minPrice);
                }
                if (maxPrice != null) {
                    n.lte(maxPrice);
                }
                return n;
            }))));
        }

        Integer status = safeRequest.getStatus() != null ? safeRequest.getStatus() : 1;
        boolQuery.filter(Query.of(q -> q.term(t -> t.field("status").value(FieldValue.of(status)))));

        if (safeRequest.getStockStatus() != null) {
            if (safeRequest.getStockStatus() > 0) {
                boolQuery.filter(Query.of(q -> q.range(r -> r.number(n -> n.field("stockQuantity").gte(1.0)))));
            } else {
                boolQuery.filter(Query.of(q -> q.range(r -> r.number(n -> n.field("stockQuantity").lte(0.0)))));
            }
        }

        if (safeRequest.getRecommended() != null) {
            boolQuery.filter(Query.of(q -> q.term(t -> t.field("recommended").value(FieldValue.of(safeRequest.getRecommended())))));
        }
        if (safeRequest.getIsNew() != null) {
            boolQuery.filter(Query.of(q -> q.term(t -> t.field("isNew").value(FieldValue.of(safeRequest.getIsNew())))));
        }
        if (safeRequest.getIsHot() != null) {
            boolQuery.filter(Query.of(q -> q.term(t -> t.field("isHot").value(FieldValue.of(safeRequest.getIsHot())))));
        }

        if (safeRequest.getMinSalesCount() != null) {
            boolQuery.filter(Query.of(q -> q.range(r -> r.number(n -> n
                    .field("salesCount")
                    .gte((double) Math.max(0, safeRequest.getMinSalesCount()))))));
        }

        Double minRating = toDouble(safeRequest.getMinRating());
        if (minRating != null) {
            boolQuery.filter(Query.of(q -> q.range(r -> r.number(n -> n.field("rating").gte(minRating)))));
        }

        if (safeRequest.getTags() != null && !safeRequest.getTags().isEmpty()) {
            List<FieldValue> tagValues = safeRequest.getTags().stream()
                    .filter(StrUtil::isNotBlank)
                    .map(tag -> FieldValue.of(tag.trim()))
                    .toList();
            if (!tagValues.isEmpty()) {
                boolQuery.filter(Query.of(q -> q.terms(t -> t.field("tags").terms(v -> v.value(tagValues)))));
            }
        }

        return Query.of(q -> q.bool(boolQuery.build()));
    }

    private List<co.elastic.clients.elasticsearch._types.SortOptions> buildSortOptions(String sortField, String sortOrder) {
        List<co.elastic.clients.elasticsearch._types.SortOptions> sortOptions = new ArrayList<>();
        SortOrder order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;
        String normalized = sortField == null ? "" : sortField.trim().toLowerCase();

        if (normalized.isEmpty()) {
            sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .score(sc -> sc.order(SortOrder.Desc))));
            return sortOptions;
        }

        switch (normalized) {
            case "score", "_score" -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .score(sc -> sc.order(order))));
            case "price" -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("price").order(order))));
            case "sales", "salescount", "sales_count" -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("salesCount").order(order))));
            case "created", "createdat", "created_at" -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("createdAt").order(order))));
            case "hotscore", "hot_score", "hot" -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("hotScore").order(order))));
            default -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field(sortField).order(order))));
        }

        return sortOptions;
    }

    private List<co.elastic.clients.elasticsearch._types.SortOptions> buildSortOptionsWithTieBreaker(String sortField, String sortOrder) {
        List<co.elastic.clients.elasticsearch._types.SortOptions> sortOptions = new ArrayList<>(buildSortOptions(sortField, sortOrder));
        SortOrder order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;
        sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                .field(f -> f.field("_id").order(order))));
        return sortOptions;
    }

    private Highlight buildHighlight() {
        return Highlight.of(h -> h
                .fields("productName", HighlightField.of(hf -> hf
                        .preTags("<em class='highlight'>")
                        .postTags("</em>")
                        .fragmentSize(100)
                        .numberOfFragments(1)
                ))
                .fields("description", HighlightField.of(hf -> hf
                        .preTags("<em class='highlight'>")
                        .postTags("</em>")
                        .fragmentSize(200)
                        .numberOfFragments(1)
                ))
        );
    }

    private Map<String, Aggregation> buildAggregations() {
        Map<String, Aggregation> aggregations = new HashMap<>();

        aggregations.put("categories", Aggregation.of(a -> a
                .terms(t -> t.field("categoryId").size(20))));

        aggregations.put("brands", Aggregation.of(a -> a
                .terms(t -> t.field("brandName.keyword").size(20))));

        aggregations.put("priceRanges", Aggregation.of(a -> a
                .range(r -> r
                        .field("price")
                        .ranges(range -> range.to(100.0))
                        .ranges(range -> range.from(100.0).to(500.0))
                        .ranges(range -> range.from(500.0).to(1000.0))
                        .ranges(range -> range.from(1000.0)))));

        return aggregations;
    }

    private Map<String, Object> processAggregations(Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations) {
        Map<String, Object> result = new HashMap<>();

        if (aggregations == null || aggregations.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> entry : aggregations.entrySet()) {
            String name = entry.getKey();
            co.elastic.clients.elasticsearch._types.aggregations.Aggregate aggregate = entry.getValue();

            if (aggregate.isSterms()) {
                StringTermsAggregate termsAggregate = aggregate.sterms();
                List<Map<String, Object>> buckets = new ArrayList<>();
                for (StringTermsBucket bucket : termsAggregate.buckets().array()) {
                    Map<String, Object> bucketData = new HashMap<>();
                    bucketData.put("key", bucket.key());
                    bucketData.put("count", bucket.docCount());
                    buckets.add(bucketData);
                }
                result.put(name, buckets);
            }
        }

        return result;
    }

    private void recordHotSearch(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        try {
            String normalized = keyword.toLowerCase();
            String dailyKey = HotKeywordKeys.todayKey();
            redisTemplate.opsForZSet().incrementScore(dailyKey, normalized, 1.0D);
            refreshHotKeywordExpiryIfNeeded(dailyKey);
            redisTemplate.opsForZSet().incrementScore(HotKeywordKeys.TOTAL_KEY, normalized, 1.0D);
            invalidateHotKeywordCachesIfNeeded();
        } catch (Exception e) {
            log.warn("Record hot search failed, keyword={}", keyword, e);
        }
    }

    private void refreshHotKeywordExpiryIfNeeded(String dailyKey) {
        if (StrUtil.isBlank(dailyKey)) {
            return;
        }
        long now = System.currentTimeMillis();
        long refreshIntervalMillis = Math.max(1, hotKeywordExpireRefreshSeconds) * 1000L;
        long lastRefreshed = hotKeywordExpireRefreshedAt.get();
        String lastKey = hotKeywordExpireKey.get();
        boolean keyChanged = !dailyKey.equals(lastKey);
        if (!keyChanged && now - lastRefreshed < refreshIntervalMillis) {
            return;
        }
        if (keyChanged) {
            hotKeywordExpireKey.set(dailyKey);
            hotKeywordExpireRefreshedAt.set(now);
            redisTemplate.expire(dailyKey, Math.max(1, hotKeywordDailyTtlDays), TimeUnit.DAYS);
            return;
        }
        if (hotKeywordExpireRefreshedAt.compareAndSet(lastRefreshed, now)) {
            redisTemplate.expire(dailyKey, Math.max(1, hotKeywordDailyTtlDays), TimeUnit.DAYS);
        }
    }

    private void invalidateHotKeywordCachesIfNeeded() {
        long now = System.currentTimeMillis();
        long intervalMillis = Math.max(500L, hotKeywordCacheInvalidateIntervalMs);
        long lastInvalidated = hotKeywordCacheInvalidatedAt.get();
        if (now - lastInvalidated < intervalMillis) {
            return;
        }
        if (hotKeywordCacheInvalidatedAt.compareAndSet(lastInvalidated, now)) {
            clearL1HotCache();
        }
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String normalizeKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return "";
        }
        return keyword.trim();
    }

    private int normalizeKeywordLimit(int limit) {
        if (limit <= 0) {
            return defaultKeywordSize();
        }
        return Math.min(limit, maxSearchSize());
    }

    private void addKeywordIfPresent(Set<String> collector, Object value) {
        if (value == null) {
            return;
        }
        String candidate = value.toString().trim();
        if (!candidate.isEmpty()) {
            collector.add(candidate);
        }
    }

    private String buildSmartSearchCacheKey(SmartSearchCacheKey key) {
        return SEARCH_CACHE_KEY_PREFIX + key.keyword() + ':' + String.valueOf(key.categoryId()) + ':'
                + String.valueOf(key.minPrice()) + ':' + String.valueOf(key.maxPrice()) + ':'
                + String.valueOf(key.sortField()) + ':' + String.valueOf(key.sortOrder()) + ':'
                + key.from() + ':' + key.size();
    }

    private String buildSuggestionCacheKey(KeywordLimitCacheKey key) {
        return SUGGESTION_CACHE_KEY_PREFIX + key.keyword() + ':' + key.limit();
    }

    private String buildHotKeywordCacheKey(LimitCacheKey key) {
        return HOT_CACHE_KEY_PREFIX + key.limit();
    }

    private String buildRecommendationCacheKey(KeywordLimitCacheKey key) {
        return RECOMMEND_CACHE_KEY_PREFIX + key.keyword() + ':' + key.limit();
    }

    private SearchResult getSmartSearchFromRedis(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (StrUtil.isBlank(json)) {
                return null;
            }
            JsonNode root = objectMapper.readTree(json);
            List<Map<String, Object>> documents = objectMapper.convertValue(
                    root.path("documents"), new TypeReference<List<Map<String, Object>>>() {
                    }
            );
            long total = root.path("total").asLong(0);
            int from = root.path("from").asInt(0);
            int size = root.path("size").asInt(defaultSearchSize());
            List<Object> searchAfter = objectMapper.convertValue(
                    root.path("searchAfter"), new TypeReference<List<Object>>() {
                    }
            );
            Map<String, Object> aggregations = objectMapper.convertValue(
                    root.path("aggregations"), new TypeReference<Map<String, Object>>() {
                    }
            );

            return SearchResult.builder()
                    .documents(documents == null ? List.of() : documents)
                    .total(total)
                    .from(from)
                    .size(size)
                    .aggregations(aggregations == null ? Map.of() : aggregations)
                    .searchAfter(searchAfter == null ? List.of() : searchAfter)
                    .build();
        } catch (Exception e) {
            log.warn("Read smart search cache failed, key={}", key, e);
            return null;
        }
    }

    private void putSmartSearchToRedis(String key, SearchResult result) {
        if (result == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(key, json, addJitterTtlSeconds(smartSearchL2TtlSeconds), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Write smart search cache failed, key={}", key, e);
        }
    }

    private StringListCacheResult getStringListFromRedis(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (StrUtil.isBlank(json)) {
                return StringListCacheResult.miss();
            }
            List<String> value = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            if (value == null || value.isEmpty()) {
                return StringListCacheResult.hit(List.of());
            }
            List<String> normalized = value.stream()
                    .filter(Objects::nonNull)
                    .filter(v -> !EMPTY_LIST_CACHE_MARKER.equals(v))
                    .toList();
            return StringListCacheResult.hit(normalized);
        } catch (Exception e) {
            log.warn("Read string list cache failed, key={}", key, e);
            return StringListCacheResult.miss();
        }
    }

    private void putStringListToRedis(String key, List<String> value, long ttlSeconds) {
        if (value == null) {
            return;
        }
        try {
            List<String> cacheValue = value.isEmpty() ? List.of(EMPTY_LIST_CACHE_MARKER) : value;
            String json = objectMapper.writeValueAsString(cacheValue);
            redisTemplate.opsForValue().set(key, json, addJitterTtlSeconds(ttlSeconds), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Write string list cache failed, key={}", key, e);
        }
    }

    private List<String> readStringListCacheIfHit(String redisKey) {
        StringListCacheResult result = getStringListFromRedis(redisKey);
        return result.hit() ? result.value() : null;
    }

    private String buildRebuildLockKey(String redisKey) {
        return CACHE_REBUILD_LOCK_PREFIX + redisKey;
    }

    private <T> T withCacheRebuildMutex(String lockKey, java.util.function.Supplier<T> readFromCache, java.util.function.Supplier<T> rebuild) {
        String token = java.util.UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, token, Math.max(500L, cacheLockLeaseMs), TimeUnit.MILLISECONDS);

        if (Boolean.TRUE.equals(acquired)) {
            try {
                return rebuild.get();
            } finally {
                safeUnlock(lockKey, token);
            }
        }

        int retries = Math.max(1, cacheLockRetryTimes);
        long waitSlice = Math.max(20L, cacheLockWaitMs / retries);
        for (int i = 0; i < retries; i++) {
            sleepQuietly(waitSlice);
            T cached = readFromCache.get();
            if (cached != null) {
                return cached;
            }
        }
        return rebuild.get();
    }

    private void safeUnlock(String lockKey, String token) {
        try {
            String current = redisTemplate.opsForValue().get(lockKey);
            if (token.equals(current)) {
                redisTemplate.delete(lockKey);
            }
        } catch (Exception e) {
            log.warn("Unlock cache rebuild key failed, key={}", lockKey, e);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long addJitterTtlSeconds(long ttlSeconds) {
        long safeBase = Math.max(5, ttlSeconds);
        long jitterUpper = Math.max(2, safeBase / 10);
        return safeBase + ThreadLocalRandom.current().nextLong(jitterUpper);
    }

    private void clearL1HotCache() {
        l1HotKeywordsCache.invalidateAll();
        l1RecommendationsCache.invalidateAll();
    }

    private void recordTimer(Timer.Sample sample, String operation, String result) {
        String timerKey = operation + ':' + result;
        Timer timer = searchLatencyTimers.computeIfAbsent(timerKey, key ->
                Timer.builder(METRIC_SEARCH_LATENCY)
                        .description("Search request latency")
                        .tag("operation", operation)
                        .tag("result", result)
                        .register(meterRegistry)
        );
        sample.stop(timer);
    }

    private void incrementEsError(String operation) {
        Counter counter = esErrorCounters.computeIfAbsent(operation, key ->
                Counter.builder(METRIC_ES_ERROR)
                        .description("Elasticsearch error count")
                        .tag("operation", operation)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    private int defaultSearchSize() {
        return defaultSearchSize <= 0 ? 20 : defaultSearchSize;
    }

    private int defaultKeywordSize() {
        return defaultKeywordSize <= 0 ? 10 : defaultKeywordSize;
    }

    private int maxSearchSize() {
        int safeMax = maxSearchSize <= 0 ? 100 : maxSearchSize;
        return Math.max(safeMax, defaultSearchSize());
    }

    private List<FieldValue> toSearchAfterValues(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<FieldValue> converted = new ArrayList<>();
        for (Object value : values) {
            if (value == null) {
                converted.add(FieldValue.NULL);
            } else if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
                converted.add(FieldValue.of(((Number) value).longValue()));
            } else if (value instanceof Number) {
                converted.add(FieldValue.of(((Number) value).doubleValue()));
            } else if (value instanceof Boolean) {
                converted.add(FieldValue.of((Boolean) value));
            } else {
                converted.add(FieldValue.of(value.toString()));
            }
        }
        return converted;
    }

    private List<Object> resolveNextSearchAfter(List<Hit<Map>> hits, int size) {
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }
        if (hits.size() < size) {
            return List.of();
        }
        Hit<Map> lastHit = hits.get(hits.size() - 1);
        if (lastHit == null || lastHit.sort() == null || lastHit.sort().isEmpty()) {
            return List.of();
        }
        List<Object> values = new ArrayList<>();
        for (FieldValue value : lastHit.sort()) {
            values.add(toSearchAfterValue(value));
        }
        return values;
    }

    private Object toSearchAfterValue(FieldValue value) {
        if (value == null) {
            return null;
        }
        if (value.isString()) {
            return value.stringValue();
        }
        if (value.isLong()) {
            return value.longValue();
        }
        if (value.isDouble()) {
            return value.doubleValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isAny()) {
            try {
                return objectMapper.readValue(value._toJsonString(), Object.class);
            } catch (Exception ex) {
                return value._toJsonString();
            }
        }
        if (value.isNull()) {
            return null;
        }
        return value._get();
    }

    private record SmartSearchCacheKey(String keyword,
                                       Long categoryId,
                                       Double minPrice,
                                       Double maxPrice,
                                       String sortField,
                                       String sortOrder,
                                       int from,
                                       int size) {
    }

    private record KeywordLimitCacheKey(String keyword, int limit) {
    }

    private record LimitCacheKey(int limit) {
    }

    private record StringListCacheResult(boolean hit, List<String> value) {
        private static StringListCacheResult miss() {
            return new StringListCacheResult(false, List.of());
        }

        private static StringListCacheResult hit(List<String> value) {
            return new StringListCacheResult(true, value == null ? List.of() : value);
        }
    }

    public static class SearchResult {
        private final List<Map<String, Object>> documents;
        private final long total;
        private final int from;
        private final int size;
        private final Map<String, Object> aggregations;
        private final List<Object> searchAfter;

        private SearchResult(List<Map<String, Object>> documents, long total, int from, int size,
                             Map<String, Object> aggregations, List<Object> searchAfter) {
            this.documents = documents;
            this.total = total;
            this.from = from;
            this.size = size;
            this.aggregations = aggregations;
            this.searchAfter = searchAfter;
        }

        public static SearchResultBuilder builder() {
            return new SearchResultBuilder();
        }

        public static SearchResult empty(int from, int size) {
            return new SearchResult(List.of(), 0, from, size, Map.of(), List.of());
        }

        public List<Map<String, Object>> getDocuments() {
            return documents;
        }

        public long getTotal() {
            return total;
        }

        public int getFrom() {
            return from;
        }

        public int getSize() {
            return size;
        }

        public Map<String, Object> getAggregations() {
            return aggregations;
        }

        public List<Object> getSearchAfter() {
            return searchAfter;
        }

        public boolean hasMore() {
            if (searchAfter != null && !searchAfter.isEmpty()) {
                return documents != null && documents.size() >= size;
            }
            return from + size < total;
        }

        public static class SearchResultBuilder {
            private List<Map<String, Object>> documents;
            private long total;
            private int from;
            private int size;
            private Map<String, Object> aggregations;
            private List<Object> searchAfter;

            public SearchResultBuilder documents(List<Map<String, Object>> documents) {
                this.documents = documents;
                return this;
            }

            public SearchResultBuilder total(long total) {
                this.total = total;
                return this;
            }

            public SearchResultBuilder from(int from) {
                this.from = from;
                return this;
            }

            public SearchResultBuilder size(int size) {
                this.size = size;
                return this;
            }

            public SearchResultBuilder aggregations(Map<String, Object> aggregations) {
                this.aggregations = aggregations;
                return this;
            }

            public SearchResultBuilder searchAfter(List<Object> searchAfter) {
                this.searchAfter = searchAfter;
                return this;
            }

            public SearchResult build() {
                return new SearchResult(documents, total, from, size, aggregations, searchAfter);
            }
        }
    }
}



