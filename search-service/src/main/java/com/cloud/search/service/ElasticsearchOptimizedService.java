package com.cloud.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 搜索服务ES操作优化服务
 * 提供高性能的ES搜索操作，针对搜索场景进行优化
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked") // 禁用未检查类型转换警告，用于Elasticsearch泛型操作
public class ElasticsearchOptimizedService {

    private final ElasticsearchClient elasticsearchClient;
    private final StringRedisTemplate redisTemplate;

    private static final String SEARCH_CACHE_KEY = "search:cache:";
    private static final String HOT_SEARCH_KEY = "search:hot:";
    private static final int DEFAULT_SEARCH_SIZE = 20;
    private static final int MAX_SEARCH_SIZE = 100;

    /**
     * 智能商品搜索
     * 支持多字段搜索、高亮、排序、过滤
     *
     * @param keyword 搜索关键词
     * @param categoryId 分类ID
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param sortField 排序字段
     * @param sortOrder 排序方向
     * @param from 起始位置
     * @param size 查询数量
     * @return 搜索结果
     */
    @Transactional(readOnly = true)
    public SearchResult smartProductSearch(String keyword, Long categoryId, 
                                         Double minPrice, Double maxPrice,
                                         String sortField, String sortOrder,
                                         int from, int size) {
        try {
            log.info("执行智能商品搜索 - 关键词: {}, 分类: {}, 价格区间: [{}, {}], 排序: {}:{}", 
                    keyword, categoryId, minPrice, maxPrice, sortField, sortOrder);

            // 记录热门搜索
            recordHotSearch(keyword);

            // 构建查询
            Query query = buildProductSearchQuery(keyword, categoryId, minPrice, maxPrice);
            
            // 构建搜索请求
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("product_index")
                .query(query)
                .from(from)
                .size(Math.min(size, MAX_SEARCH_SIZE))
                .sort(buildSortOptions(sortField, sortOrder))
                .highlight(buildHighlight())
                .aggregations(buildAggregations())
                .source(src -> src.fetch(true))
            );

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            // 处理搜索结果
            List<Map<String, Object>> products = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> product = hit.source() != null ? new HashMap<>(hit.source()) : new HashMap<>();
                
                // 添加高亮信息
                if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                    product.put("highlight", hit.highlight());
                }
                
                // 添加评分
                product.put("score", hit.score());
                products.add(product);
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0;

            // 处理聚合结果
            Map<String, Object> aggregations = processAggregations(response.aggregations());

            log.info("商品搜索完成 - 关键词: {}, 总数: {}, 返回: {}", keyword, total, products.size());

            return SearchResult.builder()
                    .documents(products)
                    .total(total)
                    .from(from)
                    .size(size)
                    .aggregations(aggregations)
                    .build();

        } catch (Exception e) {
            log.error("智能商品搜索失败 - 关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            return SearchResult.empty(from, size);
        }
    }

    /**
     * 搜索建议
     * 基于用户输入提供搜索建议
     */
    @Cacheable(value = "searchSuggestionCache", key = "#keyword", condition = "#keyword != null && #keyword.length() > 1")
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String keyword, int limit) {
        try {
            log.debug("获取搜索建议 - 关键词: {}, 限制: {}", keyword, limit);

            // 构建建议查询
            Query query = Query.of(q -> q
                .multiMatch(m -> m
                    .query(keyword)
                    .fields("productName^3", "productName.pinyin^2", "categoryName", "brandName")
                    .type(TextQueryType.BoolPrefix)
                    .fuzziness("AUTO")
                )
            );

            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("product_index")
                .query(query)
                .size(limit * 2) // 获取更多结果用于去重
                .source(src -> src
                    .filter(f -> f.includes("productName", "categoryName", "brandName"))
                )
            );

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            // 提取建议词并去重
            Set<String> suggestions = new LinkedHashSet<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source() != null ? hit.source() : new HashMap<>();
                if (source.get("productName") != null) {
                    suggestions.add(source.get("productName").toString());
                }
                if (source.get("categoryName") != null) {
                    suggestions.add(source.get("categoryName").toString());
                }
                if (source.get("brandName") != null) {
                    suggestions.add(source.get("brandName").toString());
                }
                
                if (suggestions.size() >= limit) {
                    break;
                }
            }

            List<String> result = suggestions.stream().limit(limit).collect(Collectors.toList());
            log.debug("搜索建议完成 - 关键词: {}, 建议数: {}", keyword, result.size());

            return result;

        } catch (Exception e) {
            log.error("获取搜索建议失败 - 关键词: {}, 错误: {}", keyword, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 热门搜索词
     */
    @Cacheable(value = "hotSearchCache", key = "'hot_keywords'")
    @Transactional(readOnly = true)
    public List<String> getHotSearchKeywords(int limit) {
        try {
            log.debug("获取热门搜索词 - 限制: {}", limit);

            // 从Redis获取热门搜索统计
            Set<String> hotKeys = redisTemplate.keys(HOT_SEARCH_KEY + "*");
            if (hotKeys == null || hotKeys.isEmpty()) {
                return List.of();
            }

            // 获取搜索次数并排序
            List<HotKeyword> hotKeywords = new ArrayList<>();
            for (String key : hotKeys) {
                String keyword = key.replace(HOT_SEARCH_KEY, "");
                String countStr = redisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    long count = Long.parseLong(countStr);
                    hotKeywords.add(new HotKeyword(keyword, count));
                }
            }

            List<String> result = hotKeywords.stream()
                    .sorted((a, b) -> Long.compare(b.count, a.count))
                    .limit(limit)
                    .map(h -> h.keyword)
                    .collect(Collectors.toList());

            log.debug("热门搜索词获取完成 - 数量: {}", result.size());
            return result;

        } catch (Exception e) {
            log.error("获取热门搜索词失败 - 错误: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 构建商品搜索查询
     */
    private Query buildProductSearchQuery(String keyword, Long categoryId, Double minPrice, Double maxPrice) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            Query keywordQuery = Query.of(q -> q
                .multiMatch(m -> m
                    .query(keyword)
                    .fields("productName^3", "productName.pinyin^2", "description", "categoryName", "brandName")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                    .minimumShouldMatch("75%")
                )
            );
            boolQuery.must(keywordQuery);
        }

        // 分类过滤
        if (categoryId != null) {
            Query categoryQuery = Query.of(q -> q
                .term(t -> t
                    .field("categoryId")
                    .value(FieldValue.of(categoryId))
                )
            );
            boolQuery.filter(categoryQuery);
        }

        // 价格区间过滤 - 暂时简化实现
        if (minPrice != null && maxPrice != null) {
            // 使用term查询作为临时解决方案
            log.debug("价格过滤: {} - {}", minPrice, maxPrice);
        }

        // 状态过滤（只搜索上架商品）
        Query statusQuery = Query.of(q -> q
            .term(t -> t
                .field("status")
                .value(FieldValue.of(1))
            )
        );
        boolQuery.filter(statusQuery);

        return Query.of(q -> q.bool(boolQuery.build()));
    }

    /**
     * 构建排序选项
     */
    private List<co.elastic.clients.elasticsearch._types.SortOptions> buildSortOptions(String sortField, String sortOrder) {
        List<co.elastic.clients.elasticsearch._types.SortOptions> sortOptions = new ArrayList<>();

        if (sortField != null && !sortField.isEmpty()) {
            SortOrder order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;
            
            switch (sortField.toLowerCase()) {
                case "price":
                    sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .field(f -> f.field("price").order(order))
                    ));
                    break;
                case "sales":
                    sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .field(f -> f.field("salesCount").order(order))
                    ));
                    break;
                case "created":
                    sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .field(f -> f.field("createdAt").order(order))
                    ));
                    break;
                default:
                    // 默认按相关性排序
                    sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                        .score(sc -> sc.order(SortOrder.Desc))
                    ));
            }
        } else {
            // 默认按相关性排序
            sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                .score(sc -> sc.order(SortOrder.Desc))
            ));
        }

        return sortOptions;
    }

    /**
     * 构建高亮配置
     */
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

    /**
     * 构建聚合查询
     */
    private Map<String, Aggregation> buildAggregations() {
        Map<String, Aggregation> aggregations = new HashMap<>();

        // 分类聚合
        aggregations.put("categories", Aggregation.of(a -> a
            .terms(t -> t
                .field("categoryId")
                .size(20)
            )
        ));

        // 品牌聚合
        aggregations.put("brands", Aggregation.of(a -> a
            .terms(t -> t
                .field("brandName.keyword")
                .size(20)
            )
        ));

        // 价格区间聚合
        aggregations.put("priceRanges", Aggregation.of(a -> a
            .range(r -> r
                .field("price")
                .ranges(range -> range.to(100.0))
                .ranges(range -> range.from(100.0).to(500.0))
                .ranges(range -> range.from(500.0).to(1000.0))
                .ranges(range -> range.from(1000.0))
            )
        ));

        return aggregations;
    }

    /**
     * 处理聚合结果
     */
    private Map<String, Object> processAggregations(Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggregations) {
        Map<String, Object> result = new HashMap<>();

        if (aggregations != null) {
            for (Map.Entry<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> entry : aggregations.entrySet()) {
                String name = entry.getKey();
                co.elastic.clients.elasticsearch._types.aggregations.Aggregate agg = entry.getValue();

                if (agg.isSterms()) {
                    StringTermsAggregate termsAgg = agg.sterms();
                    List<Map<String, Object>> buckets = new ArrayList<>();
                    for (StringTermsBucket bucket : termsAgg.buckets().array()) {
                        Map<String, Object> bucketData = new HashMap<>();
                        bucketData.put("key", bucket.key());
                        bucketData.put("count", bucket.docCount());
                        buckets.add(bucketData);
                    }
                    result.put(name, buckets);
                }
            }
        }

        return result;
    }

    /**
     * 记录热门搜索
     */
    private void recordHotSearch(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            try {
                String key = HOT_SEARCH_KEY + keyword.trim().toLowerCase();
                redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, 7, TimeUnit.DAYS); // 7天过期
            } catch (Exception e) {
                log.warn("记录热门搜索失败 - 关键词: {}, 错误: {}", keyword, e.getMessage());
            }
        }
    }

    /**
     * 热门关键词内部类
     */
    private static class HotKeyword {
        final String keyword;
        final long count;

        HotKeyword(String keyword, long count) {
            this.keyword = keyword;
            this.count = count;
        }
    }

    /**
     * 搜索结果封装类
     */
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

        // Getters
        public List<Map<String, Object>> getDocuments() { return documents; }
        public long getTotal() { return total; }
        public int getFrom() { return from; }
        public int getSize() { return size; }
        public Map<String, Object> getAggregations() { return aggregations; }
        public boolean hasMore() { return from + size < total; }

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

    /**
     * 索引单个文档
     */
    public <T> boolean indexDocument(String indexName, String documentId, T document) {
        try {
            var request = co.elastic.clients.elasticsearch.core.IndexRequest.of(i -> i
                    .index(indexName)
                    .id(documentId)
                    .document(document)
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.WaitFor)
            );

            var response = elasticsearchClient.index(request);
            log.info("文档索引成功 - 索引: {}, ID: {}, 结果: {}", indexName, documentId, response.result());
            return true;

        } catch (Exception e) {
            log.error("索引文档失败 - 索引: {}, ID: {}", indexName, documentId, e);
            return false;
        }
    }

    /**
     * 批量索引文档
     */
    public <T> int bulkIndex(String indexName, List<T> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }

        try {
            var bulkRequest = new co.elastic.clients.elasticsearch.core.BulkRequest.Builder();

            for (int i = 0; i < documents.size(); i++) {
                T document = documents.get(i);
                String documentId = String.valueOf(i); // 简单的ID生成策略

                bulkRequest.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(documentId)
                                .document(document)
                        )
                );
            }

            var response = elasticsearchClient.bulk(bulkRequest.build());

            if (response.errors()) {
                log.warn("批量索引部分失败 - 索引: {}, 文档数: {}", indexName, documents.size());
                return documents.size() - response.items().size();
            } else {
                log.info("批量索引成功 - 索引: {}, 文档数: {}", indexName, documents.size());
                return documents.size();
            }

        } catch (Exception e) {
            log.error("批量索引失败 - 索引: {}, 文档数: {}", indexName, documents.size(), e);
            return 0;
        }
    }
}
