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
import org.springframework.util.StringUtils;

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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings("unchecked")
public class ElasticsearchOptimizedService {

    private static final String PRODUCT_INDEX = "product_index";
    private static final String HOT_SEARCH_ZSET_KEY = "search:hot:zset";
    private static final String SEARCH_CACHE_KEY_PREFIX = "search:smart:";
    private static final String SUGGESTION_CACHE_KEY_PREFIX = "search:suggest:";
    private static final String HOT_CACHE_KEY_PREFIX = "search:hot:list:";
    private static final String RECOMMEND_CACHE_KEY_PREFIX = "search:recommend:";
    private static final String EMPTY_LIST_CACHE_MARKER = "__EMPTY__";
    private static final String METRIC_SEARCH_LATENCY = "search.request.latency";
    private static final String METRIC_ES_ERROR = "search.es.error.count";
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

    @Value("${search.optimized.elasticsearch.request-timeout-ms:700}")
    private int esRequestTimeoutMs;

    @Value("${search.optimized.cache.l1-max-entries:5000}")
    private int l1MaxEntries;

    @Value("${search.optimized.hot-keyword.expire-refresh-seconds:60}")
    private long hotKeywordExpireRefreshSeconds;

    @Value("${search.optimized.hot-keyword.cache-invalidate-interval-ms:3000}")
    private long hotKeywordCacheInvalidateIntervalMs;

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
    public List<String> getSearchSuggestions(String keyword, int limit) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String safeKeyword = normalizeKeyword(keyword);
        if (!StringUtils.hasText(safeKeyword)) {
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
        SearchResult l2 = getSmartSearchFromRedis(redisKey);
        if (l2 != null) {
            return l2;
        }
        SearchResult result = querySmartSearchFromElasticsearch(cacheKey);
        putSmartSearchToRedis(redisKey, result);
        return result;
    }

    private List<String> loadSuggestionsForCache(KeywordLimitCacheKey cacheKey) {
        String redisKey = buildSuggestionCacheKey(cacheKey);
        StringListCacheResult l2 = getStringListFromRedis(redisKey);
        if (l2.hit()) {
            return l2.value();
        }
        List<String> result = querySuggestionsFromElasticsearch(cacheKey.keyword(), cacheKey.limit());
        putStringListToRedis(redisKey, result, suggestionL2TtlSeconds);
        return result;
    }

    private List<String> loadHotKeywordsForCache(LimitCacheKey cacheKey) {
        String redisKey = buildHotKeywordCacheKey(cacheKey);
        StringListCacheResult l2 = getStringListFromRedis(redisKey);
        if (l2.hit()) {
            return l2.value();
        }
        List<String> result = queryHotKeywordsFromRedis(cacheKey.limit());
        putStringListToRedis(redisKey, result, hotKeywordsL2TtlSeconds);
        return result;
    }

    private List<String> loadRecommendationsForCache(KeywordLimitCacheKey cacheKey) {
        String redisKey = buildRecommendationCacheKey(cacheKey);
        StringListCacheResult l2 = getStringListFromRedis(redisKey);
        if (l2.hit()) {
            return l2.value();
        }
        List<String> result = computeRecommendations(cacheKey.keyword(), cacheKey.limit());
        putStringListToRedis(redisKey, result, recommendationL2TtlSeconds);
        return result;
    }

    private SearchResult querySmartSearchFromElasticsearch(SmartSearchCacheKey key) {
        Query query = buildProductSearchQuery(key.keyword(), key.categoryId(), key.minPrice(), key.maxPrice());
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(PRODUCT_INDEX)
                .query(query)
                .from(key.from())
                .size(key.size())
                .timeout(esRequestTimeoutMs + "ms")
                .sort(buildSortOptions(key.sortField(), key.sortOrder()))
                .highlight(buildHighlight())
                .aggregations(buildAggregations())
                .source(src -> src.fetch(true))
        );

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
            return SearchResult.builder()
                    .documents(products)
                    .total(total)
                    .from(key.from())
                    .size(key.size())
                    .aggregations(aggregations)
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
        Set<String> hotKeywords = redisTemplate.opsForZSet().reverseRange(HOT_SEARCH_ZSET_KEY, 0, limit - 1L);
        if (hotKeywords == null || hotKeywords.isEmpty()) {
            return List.of();
        }
        return hotKeywords.stream()
                .filter(StringUtils::hasText)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<String> computeRecommendations(String keyword, int limit) {
        LinkedHashSet<String> deduplicated = new LinkedHashSet<>();
        if (StringUtils.hasText(keyword)) {
            deduplicated.addAll(getSearchSuggestions(keyword, limit));
        }

        List<String> hotKeywords = getHotSearchKeywords(Math.min(limit * 3, maxSearchSize()));
        if (StringUtils.hasText(keyword)) {
            String lowered = keyword.toLowerCase();
            hotKeywords.stream()
                    .filter(item -> item.toLowerCase().contains(lowered))
                    .forEach(deduplicated::add);
        }

        hotKeywords.forEach(deduplicated::add);
        return deduplicated.stream()
                .filter(StringUtils::hasText)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Query buildProductSearchQuery(String keyword, Long categoryId, Double minPrice, Double maxPrice) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (StringUtils.hasText(keyword)) {
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

    private List<co.elastic.clients.elasticsearch._types.SortOptions> buildSortOptions(String sortField, String sortOrder) {
        List<co.elastic.clients.elasticsearch._types.SortOptions> sortOptions = new ArrayList<>();
        SortOrder order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;

        if (StringUtils.hasText(sortField)) {
            switch (sortField.toLowerCase()) {
                case "price" -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .field(f -> f.field("price").order(order))));
                case "sales" -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .field(f -> f.field("salesCount").order(order))));
                case "created" -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .field(f -> f.field("createdAt").order(order))));
                default -> sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .score(sc -> sc.order(SortOrder.Desc))));
            }
        } else {
            sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .score(sc -> sc.order(SortOrder.Desc))));
        }

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
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        try {
            String normalized = keyword.toLowerCase();
            redisTemplate.opsForZSet().incrementScore(HOT_SEARCH_ZSET_KEY, normalized, 1.0D);
            refreshHotKeywordExpiryIfNeeded();
            invalidateHotKeywordCachesIfNeeded();
        } catch (Exception e) {
            log.warn("Record hot search failed, keyword={}", keyword, e);
        }
    }

    private void refreshHotKeywordExpiryIfNeeded() {
        long now = System.currentTimeMillis();
        long refreshIntervalMillis = Math.max(1, hotKeywordExpireRefreshSeconds) * 1000L;
        long lastRefreshed = hotKeywordExpireRefreshedAt.get();
        if (now - lastRefreshed < refreshIntervalMillis) {
            return;
        }
        if (hotKeywordExpireRefreshedAt.compareAndSet(lastRefreshed, now)) {
            redisTemplate.expire(HOT_SEARCH_ZSET_KEY, 7, TimeUnit.DAYS);
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

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
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
            if (!StringUtils.hasText(json)) {
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
            if (!StringUtils.hasText(json)) {
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

        private SearchResult(List<Map<String, Object>> documents, long total, int from, int size, Map<String, Object> aggregations) {
            this.documents = documents;
            this.total = total;
            this.from = from;
            this.size = size;
            this.aggregations = aggregations;
        }

        public static SearchResultBuilder builder() {
            return new SearchResultBuilder();
        }

        public static SearchResult empty(int from, int size) {
            return new SearchResult(List.of(), 0, from, size, Map.of());
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

        public boolean hasMore() {
            return from + size < total;
        }

        public static class SearchResultBuilder {
            private List<Map<String, Object>> documents;
            private long total;
            private int from;
            private int size;
            private Map<String, Object> aggregations;

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

            public SearchResult build() {
                return new SearchResult(documents, total, from, size, aggregations);
            }
        }
    }
}
