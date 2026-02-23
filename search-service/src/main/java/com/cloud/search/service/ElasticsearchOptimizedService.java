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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;








@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked") 
public class ElasticsearchOptimizedService {

    private static final String SEARCH_CACHE_KEY = "search:cache:";
    private static final String HOT_SEARCH_ZSET_KEY = "search:hot:zset";
    private static final int DEFAULT_SEARCH_SIZE = 20;
    private static final int MAX_SEARCH_SIZE = 100;
    private final ElasticsearchClient elasticsearchClient;
    private final StringRedisTemplate redisTemplate;

    













    @Transactional(readOnly = true)
    public SearchResult smartProductSearch(String keyword, Long categoryId,
                                           Double minPrice, Double maxPrice,
                                           String sortField, String sortOrder,
                                           int from, int size) {
        try {
            


            
            recordHotSearch(keyword);

            
            Query query = buildProductSearchQuery(keyword, categoryId, minPrice, maxPrice);

            
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

            
            List<Map<String, Object>> products = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> product = hit.source() != null ? new HashMap<>(hit.source()) : new HashMap<>();

                
                if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                    product.put("highlight", hit.highlight());
                }

                
                product.put("score", hit.score());
                products.add(product);
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0;

            
            Map<String, Object> aggregations = processAggregations(response.aggregations());

            

            return SearchResult.builder()
                    .documents(products)
                    .total(total)
                    .from(from)
                    .size(size)
                    .aggregations(aggregations)
                    .build();

        } catch (Exception e) {
            log.error("閺呴缚鍏橀崯鍡楁惂閹兼粎鍌ㄦ径杈Е - 閸忔娊鏁拠? {}, 闁挎瑨顕? {}", keyword, e.getMessage(), e);
            return SearchResult.empty(from, size);
        }
    }

    



    @Cacheable(value = "searchSuggestionCache", key = "#keyword", condition = "#keyword != null && #keyword.length() > 1")
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String keyword, int limit) {
        try {
            log.debug("閼惧嘲褰囬幖婊呭偍瀵ら缚顔?- 閸忔娊鏁拠? {}, 闂勬劕鍩? {}", keyword, limit);

            
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
                    .size(limit * 2) 
                    .source(src -> src
                            .filter(f -> f.includes("productName", "categoryName", "brandName"))
                    )
            );

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            
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
            log.debug("閹兼粎鍌ㄥ楦款唴鐎瑰本鍨?- 閸忔娊鏁拠? {}, 瀵ら缚顔呴弫? {}", keyword, result.size());

            return result;

        } catch (Exception e) {
            log.error("閼惧嘲褰囬幖婊呭偍瀵ら缚顔呮径杈Е - 閸忔娊鏁拠? {}, 闁挎瑨顕? {}", keyword, e.getMessage(), e);
            return List.of();
        }
    }

    


    @Cacheable(value = "hotSearchCache", key = "'hot_keywords'")
    @Transactional(readOnly = true)
    public List<String> getHotSearchKeywords(int limit) {
        try {
            log.debug("閼惧嘲褰囬悜顓㈡，閹兼粎鍌ㄧ拠?- 闂勬劕鍩? {}", limit);
            int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
            Set<String> hotKeywords = redisTemplate.opsForZSet().reverseRange(HOT_SEARCH_ZSET_KEY, 0, safeLimit - 1L);
            if (hotKeywords == null || hotKeywords.isEmpty()) {
                return List.of();
            }
            List<String> result = hotKeywords.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            log.debug("閻戭參妫幖婊呭偍鐠囧秷骞忛崣鏍х暚閹?- 閺佷即鍣? {}", result.size());
            return result;
        } catch (Exception e) {
            log.error("閼惧嘲褰囬悜顓㈡，閹兼粎鍌ㄧ拠宥呫亼鐠?- 闁挎瑨顕? {}", e.getMessage(), e);
            return List.of();
        }
    }

    private Query buildProductSearchQuery(String keyword, Long categoryId, Double minPrice, Double maxPrice) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        
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

        
        if (categoryId != null) {
            Query categoryQuery = Query.of(q -> q
                    .term(t -> t
                            .field("categoryId")
                            .value(FieldValue.of(categoryId))
                    )
            );
            boolQuery.filter(categoryQuery);
        }

        
        if (minPrice != null && maxPrice != null) {
            
            log.debug("娴犻攱鐗告潻鍥ㄦ姢: {} - {}", minPrice, maxPrice);
        }

        
        Query statusQuery = Query.of(q -> q
                .term(t -> t
                        .field("status")
                        .value(FieldValue.of(1))
                )
        );
        boolQuery.filter(statusQuery);

        return Query.of(q -> q.bool(boolQuery.build()));
    }

    


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
                    
                    sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                            .score(sc -> sc.order(SortOrder.Desc))
                    ));
            }
        } else {
            
            sortOptions.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .score(sc -> sc.order(SortOrder.Desc))
            ));
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
                .terms(t -> t
                        .field("categoryId")
                        .size(20)
                )
        ));

        
        aggregations.put("brands", Aggregation.of(a -> a
                .terms(t -> t
                        .field("brandName.keyword")
                        .size(20)
                )
        ));

        
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

    


    private void recordHotSearch(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            try {
                String normalized = keyword.trim().toLowerCase();
                redisTemplate.opsForZSet().incrementScore(HOT_SEARCH_ZSET_KEY, normalized, 1.0D);
                redisTemplate.expire(HOT_SEARCH_ZSET_KEY, 7, TimeUnit.DAYS);
            } catch (Exception e) {
                log.warn("鐠佹澘缍嶉悜顓㈡，閹兼粎鍌ㄦ径杈Е - 閸忔娊鏁拠? {}, 闁挎瑨顕? {}", keyword, e.getMessage());
            }
        }
    }

    public <T> boolean indexDocument(String indexName, String documentId, T document) {
        try {
            var request = co.elastic.clients.elasticsearch.core.IndexRequest.of(i -> i
                    .index(indexName)
                    .id(documentId)
                    .document(document)
                    .refresh(co.elastic.clients.elasticsearch._types.Refresh.WaitFor)
            );

            var response = elasticsearchClient.index(request);
            
            return true;

        } catch (Exception e) {
            log.error("缁便垹绱╅弬鍥ㄣ€傛径杈Е - 缁便垹绱? {}, ID: {}", indexName, documentId, e);
            return false;
        }
    }

    


    public <T> int bulkIndex(String indexName, List<T> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }

        try {
            var bulkRequest = new co.elastic.clients.elasticsearch.core.BulkRequest.Builder();

            for (int i = 0; i < documents.size(); i++) {
                T document = documents.get(i);
                String documentId = String.valueOf(i); 

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
                log.warn("閹靛綊鍣虹槐銏犵穿闁劌鍨庢径杈Е - 缁便垹绱? {}, 閺傚洦銆傞弫? {}", indexName, documents.size());
                return documents.size() - response.items().size();
            } else {
                
                return documents.size();
            }

        } catch (Exception e) {
            log.error("閹靛綊鍣虹槐銏犵穿婢惰精瑙?- 缁便垹绱? {}, 閺傚洦銆傞弫? {}", indexName, documents.size(), e);
            return 0;
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
