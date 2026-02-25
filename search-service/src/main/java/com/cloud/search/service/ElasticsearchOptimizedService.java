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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class ElasticsearchOptimizedService {

    private static final String PRODUCT_INDEX = "product_index";
    private static final String HOT_SEARCH_ZSET_KEY = "search:hot:zset";
    private static final String SEARCH_CACHE_KEY_PREFIX = "search:smart:";
    private static final String SUGGESTION_CACHE_KEY_PREFIX = "search:suggest:";
    private static final String HOT_CACHE_KEY_PREFIX = "search:hot:list:";
    private static final String RECOMMEND_CACHE_KEY_PREFIX = "search:recommend:";
    private static final String METRIC_SEARCH_LATENCY = "search.request.latency";
    private static final String METRIC_ES_ERROR = "search.es.error.count";

    @Value("${search.optimized.cache.smart-search.l2-ttl-seconds:120}")
    private long smartSearchL2TtlSeconds;

    @Value("${search.optimized.cache.suggestions.l2-ttl-seconds:120}")
    private long suggestionL2TtlSeconds;

    @Value("${search.optimized.cache.hot-keywords.l2-ttl-seconds:30}")
    private long hotKeywordsL2TtlSeconds;

    @Value("${search.optimized.cache.recommendations.l2-ttl-seconds:60}")
    private long recommendationL2TtlSeconds;

    @Value("${search.optimized.cache.smart-search.l1-ttl-millis:30000}")
    private long smartSearchL1TtlMillis;

    @Value("${search.optimized.cache.suggestions.l1-ttl-millis:20000}")
    private long suggestionL1TtlMillis;

    @Value("${search.optimized.cache.hot-keywords.l1-ttl-millis:15000}")
    private long hotKeywordsL1TtlMillis;

    @Value("${search.optimized.cache.recommendations.l1-ttl-millis:20000}")
    private long recommendationL1TtlMillis;

    @Value("${search.optimized.limit.default-search-size:20}")
    private int defaultSearchSize;

    @Value("${search.optimized.limit.default-keyword-size:10}")
    private int defaultKeywordSize;

    @Value("${search.optimized.limit.max-search-size:100}")
    private int maxSearchSize;

    @Value("${search.optimized.elasticsearch.request-timeout-ms:700}")
    private int esRequestTimeoutMs;

    private final ElasticsearchClient elasticsearchClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final Map<String, CacheEntry<SearchResult>> l1SmartSearchCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<String>>> l1SuggestionsCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<String>>> l1HotKeywordsCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<String>>> l1RecommendationsCache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public SearchResult smartProductSearch(String keyword, Long categoryId,
                                           Double minPrice, Double maxPrice,
                                           String sortField, String sortOrder,
                                           int from, int size) {
        Timer.Sample sample = Timer.start(meterRegistry);
        int safeFrom = Math.max(0, from);
        int safeSize = size <= 0 ? defaultSearchSize() : Math.min(size, maxSearchSize());
        String safeKeyword = normalizeKeyword(keyword);
        String cacheKey = buildSmartSearchCacheKey(safeKeyword, categoryId, minPrice, maxPrice, sortField, sortOrder, safeFrom, safeSize);

        SearchResult l1Cached = getL1Cache(l1SmartSearchCache, cacheKey);
        if (l1Cached != null) {
            recordTimer(sample, "smart-search", "l1-hit");
            return l1Cached;
        }

        SearchResult l2Cached = getSmartSearchFromRedis(cacheKey);
        if (l2Cached != null) {
            putL1Cache(l1SmartSearchCache, cacheKey, l2Cached, smartSearchL1TtlMillis);
            recordTimer(sample, "smart-search", "l2-hit");
            return l2Cached;
        }

        try {
            Query query = buildProductSearchQuery(safeKeyword, categoryId, minPrice, maxPrice);
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(PRODUCT_INDEX)
                    .query(query)
                    .from(safeFrom)
                    .size(safeSize)
                    .timeout(esRequestTimeoutMs + "ms")
                    .sort(buildSortOptions(sortField, sortOrder))
                    .highlight(buildHighlight())
                    .aggregations(buildAggregations())
                    .source(src -> src.fetch(true))
            );

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

            SearchResult result = SearchResult.builder()
                    .documents(products)
                    .total(total)
                    .from(safeFrom)
                    .size(safeSize)
                    .aggregations(aggregations)
                    .build();

            putSmartSearchToRedis(cacheKey, result);
            putL1Cache(l1SmartSearchCache, cacheKey, result, smartSearchL1TtlMillis);
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
        String cacheKey = buildSuggestionCacheKey(safeKeyword, safeLimit);

        List<String> l1Cached = getL1Cache(l1SuggestionsCache, cacheKey);
        if (l1Cached != null) {
            recordTimer(sample, "suggestions", "l1-hit");
            return l1Cached;
        }

        List<String> l2Cached = getStringListFromRedis(cacheKey);
        if (!l2Cached.isEmpty()) {
            putL1Cache(l1SuggestionsCache, cacheKey, l2Cached, suggestionL1TtlMillis);
            recordTimer(sample, "suggestions", "l2-hit");
            return l2Cached;
        }

        try {
            Query query = Query.of(q -> q
                    .multiMatch(m -> m
                            .query(safeKeyword)
                            .fields("productName^3", "productName.pinyin^2", "categoryName", "brandName")
                            .type(TextQueryType.BoolPrefix)
                            .fuzziness("AUTO")
                    )
            );

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(PRODUCT_INDEX)
                    .query(query)
                    .size(Math.min(safeLimit * 2, maxSearchSize()))
                    .timeout(esRequestTimeoutMs + "ms")
                    .source(src -> src.filter(f -> f.includes("productName", "categoryName", "brandName")))
            );

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);
            Set<String> suggestions = new LinkedHashSet<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source() != null ? hit.source() : Collections.emptyMap();
                addKeywordIfPresent(suggestions, source.get("productName"));
                addKeywordIfPresent(suggestions, source.get("categoryName"));
                addKeywordIfPresent(suggestions, source.get("brandName"));
                if (suggestions.size() >= safeLimit) {
                    break;
                }
            }

            List<String> result = suggestions.stream().limit(safeLimit).toList();
            putStringListToRedis(cacheKey, result, suggestionL2TtlSeconds);
            putL1Cache(l1SuggestionsCache, cacheKey, result, suggestionL1TtlMillis);
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
        String cacheKey = buildHotKeywordCacheKey(safeLimit);

        List<String> l1Cached = getL1Cache(l1HotKeywordsCache, cacheKey);
        if (l1Cached != null) {
            recordTimer(sample, "hot-keywords", "l1-hit");
            return l1Cached;
        }

        List<String> l2Cached = getStringListFromRedis(cacheKey);
        if (!l2Cached.isEmpty()) {
            putL1Cache(l1HotKeywordsCache, cacheKey, l2Cached, hotKeywordsL1TtlMillis);
            recordTimer(sample, "hot-keywords", "l2-hit");
            return l2Cached;
        }

        try {
            Set<String> hotKeywords = redisTemplate.opsForZSet().reverseRange(HOT_SEARCH_ZSET_KEY, 0, safeLimit - 1L);
            if (hotKeywords == null || hotKeywords.isEmpty()) {
                recordTimer(sample, "hot-keywords", "empty");
                return List.of();
            }

            List<String> result = hotKeywords.stream()
                    .filter(StringUtils::hasText)
                    .limit(safeLimit)
                    .collect(Collectors.toList());

            putStringListToRedis(cacheKey, result, hotKeywordsL2TtlSeconds);
            putL1Cache(l1HotKeywordsCache, cacheKey, result, hotKeywordsL1TtlMillis);
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
        String cacheKey = buildRecommendationCacheKey(safeKeyword, safeLimit);

        List<String> l1Cached = getL1Cache(l1RecommendationsCache, cacheKey);
        if (l1Cached != null) {
            recordTimer(sample, "recommendations", "l1-hit");
            return l1Cached;
        }

        List<String> l2Cached = getStringListFromRedis(cacheKey);
        if (!l2Cached.isEmpty()) {
            putL1Cache(l1RecommendationsCache, cacheKey, l2Cached, recommendationL1TtlMillis);
            recordTimer(sample, "recommendations", "l2-hit");
            return l2Cached;
        }

        LinkedHashSet<String> deduplicated = new LinkedHashSet<>();
        if (StringUtils.hasText(safeKeyword)) {
            deduplicated.addAll(getSearchSuggestions(safeKeyword, safeLimit));
        }

        List<String> hotKeywords = getHotSearchKeywords(Math.min(safeLimit * 3, maxSearchSize()));
        if (StringUtils.hasText(safeKeyword)) {
            String lowered = safeKeyword.toLowerCase();
            hotKeywords.stream()
                    .filter(item -> item.toLowerCase().contains(lowered))
                    .forEach(deduplicated::add);
        }

        hotKeywords.forEach(deduplicated::add);

        List<String> result = deduplicated.stream()
                .filter(StringUtils::hasText)
                .limit(safeLimit)
                .collect(Collectors.toList());

        putStringListToRedis(cacheKey, result, recommendationL2TtlSeconds);
        putL1Cache(l1RecommendationsCache, cacheKey, result, recommendationL1TtlMillis);
        recordTimer(sample, "recommendations", "computed");
        return result;
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
            redisTemplate.expire(HOT_SEARCH_ZSET_KEY, 7, TimeUnit.DAYS);
            clearL1HotCache();
        } catch (Exception e) {
            log.warn("Record hot search failed, keyword={}", keyword, e);
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

    private String buildSmartSearchCacheKey(String keyword, Long categoryId, Double minPrice, Double maxPrice,
                                            String sortField, String sortOrder, int from, int size) {
        return SEARCH_CACHE_KEY_PREFIX + keyword + ':' + String.valueOf(categoryId) + ':' + String.valueOf(minPrice)
                + ':' + String.valueOf(maxPrice) + ':' + String.valueOf(sortField) + ':' + String.valueOf(sortOrder)
                + ':' + from + ':' + size;
    }

    private String buildSuggestionCacheKey(String keyword, int limit) {
        return SUGGESTION_CACHE_KEY_PREFIX + keyword.toLowerCase() + ':' + limit;
    }

    private String buildHotKeywordCacheKey(int limit) {
        return HOT_CACHE_KEY_PREFIX + limit;
    }

    private String buildRecommendationCacheKey(String keyword, int limit) {
        return RECOMMEND_CACHE_KEY_PREFIX + keyword.toLowerCase() + ':' + limit;
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
            redisTemplate.opsForValue().set(key, json, smartSearchL2TtlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Write smart search cache failed, key={}", key, e);
        }
    }

    private List<String> getStringListFromRedis(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return List.of();
            }
            List<String> value = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return value == null ? List.of() : value.stream().filter(Objects::nonNull).toList();
        } catch (Exception e) {
            log.warn("Read string list cache failed, key={}", key, e);
            return List.of();
        }
    }

    private void putStringListToRedis(String key, List<String> value, long ttlSeconds) {
        if (value == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Write string list cache failed, key={}", key, e);
        }
    }

    private <T> T getL1Cache(Map<String, CacheEntry<T>> cache, String key) {
        CacheEntry<T> cacheEntry = cache.get(key);
        if (cacheEntry == null) {
            return null;
        }
        if (cacheEntry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return cacheEntry.value();
    }

    private <T> void putL1Cache(Map<String, CacheEntry<T>> cache, String key, T value, long ttlMillis) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMillis));
    }

    private void clearL1HotCache() {
        l1HotKeywordsCache.clear();
        l1RecommendationsCache.clear();
    }

    private void recordTimer(Timer.Sample sample, String operation, String result) {
        sample.stop(Timer.builder(METRIC_SEARCH_LATENCY)
                .description("Search request latency")
                .tag("operation", operation)
                .tag("result", result)
                .register(meterRegistry));
    }

    private void incrementEsError(String operation) {
        Counter.builder(METRIC_ES_ERROR)
                .description("Elasticsearch error count")
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
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

    private record CacheEntry<T>(T value, long expiresAtMillis) {
        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
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
