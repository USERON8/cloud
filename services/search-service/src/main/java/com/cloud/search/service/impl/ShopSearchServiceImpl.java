package com.cloud.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.SearchResultDTO;
import com.cloud.search.dto.ShopSearchRequest;
import com.cloud.search.repository.ShopDocumentRepository;
import com.cloud.search.service.ShopSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopSearchServiceImpl implements ShopSearchService {

  private static final int ACTIVE_STATUS = 1;
  private static final String SHOP_INDEX = "shop_index";
  private static final String PROCESSED_EVENT_BUCKET_PREFIX = "search:shop:processed:bucket:";
  private static final long PROCESSED_EVENT_TTL_SECONDS = 24 * 60 * 60;
  private static final int PROCESSED_LOOKBACK_DAYS = 1;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

  private final ShopDocumentRepository shopDocumentRepository;
  private final ElasticsearchClient elasticsearchClient;
  private final ElasticsearchOperations elasticsearchOperations;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteShop(Long shopId) {
    try {
      shopDocumentRepository.deleteById(String.valueOf(shopId));
    } catch (Exception e) {
      throw new RuntimeException("Delete shop from index failed", e);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateShopStatus(Long shopId, Integer status) {
    try {
      ShopDocument document = findByShopId(shopId);
      if (document == null) {
        return;
      }
      document.setStatus(status);
      shopDocumentRepository.save(document);
    } catch (Exception e) {
      throw new RuntimeException("Update shop status in index failed", e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public ShopDocument findByShopId(Long shopId) {
    return shopDocumentRepository.findById(String.valueOf(shopId)).orElse(null);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void batchDeleteShops(List<Long> shopIds) {
    try {
      List<String> ids =
          shopIds == null
              ? Collections.emptyList()
              : shopIds.stream().map(String::valueOf).toList();
      if (!ids.isEmpty()) {
        shopDocumentRepository.deleteAllById(ids);
      }
    } catch (Exception e) {
      throw new RuntimeException("Batch delete shops from index failed", e);
    }
  }

  @Override
  public boolean isEventProcessed(String traceId) {
    if (StrUtil.isBlank(traceId)) {
      return false;
    }
    try {
      for (int i = 0; i <= PROCESSED_LOOKBACK_DAYS; i++) {
        String bucketKey = buildProcessedBucketKey(i);
        Boolean exists = redisTemplate.opsForHash().hasKey(bucketKey, traceId);
        if (Boolean.TRUE.equals(exists)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      log.warn("Check shop processed event failed: traceId={}", traceId, e);
      return false;
    }
  }

  @Override
  public void markEventProcessed(String traceId) {
    if (StrUtil.isBlank(traceId)) {
      return;
    }
    try {
      String bucketKey = buildProcessedBucketKey(0);
      redisTemplate.opsForHash().put(bucketKey, traceId, "1");
      redisTemplate.expire(bucketKey, PROCESSED_EVENT_TTL_SECONDS, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.warn("Mark shop event processed failed: traceId={}", traceId, e);
    }
  }

  private String buildProcessedBucketKey(int offsetDays) {
    LocalDate date = LocalDate.now().minusDays(offsetDays);
    return PROCESSED_EVENT_BUCKET_PREFIX + date.format(DATE_FORMATTER);
  }

  @Override
  public void rebuildShopIndex() {
    if (indexExists()) {
      deleteShopIndex();
    }
    createShopIndex();
  }

  @Override
  public boolean indexExists() {
    try {
      return elasticsearchOperations.indexOps(ShopDocument.class).exists();
    } catch (Exception e) {
      log.error("Check shop index existence failed", e);
      return false;
    }
  }

  @Override
  public void createShopIndex() {
    try {
      elasticsearchOperations.indexOps(ShopDocument.class).create();
      elasticsearchOperations.indexOps(ShopDocument.class).putMapping();
    } catch (Exception e) {
      throw new RuntimeException("Create shop index failed", e);
    }
  }

  @Override
  public void deleteShopIndex() {
    try {
      elasticsearchOperations.indexOps(ShopDocument.class).delete();
    } catch (Exception e) {
      throw new RuntimeException("Delete shop index failed", e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ShopDocument> searchShops(ShopSearchRequest request) {
    ShopSearchRequest safeRequest = request == null ? new ShopSearchRequest() : request;
    long start = System.currentTimeMillis();
    if (requiresOptimizedShopSearch(safeRequest)) {
      return searchShopsViaElasticsearch(safeRequest, start);
    }

    Pageable pageable =
        PageRequest.of(
            normalizePage(safeRequest.getPage()),
            normalizeSize(safeRequest.getSize()),
            buildSort(safeRequest.getSortBy(), safeRequest.getSortOrder()));

    Page<ShopDocument> page = selectPage(safeRequest, pageable);
    long took = System.currentTimeMillis() - start;

    return SearchResultDTO.of(
        page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), took);
  }

  private SearchResultDTO<ShopDocument> searchShopsViaElasticsearch(
      ShopSearchRequest request, long start) {
    int pageNum = normalizePage(request.getPage());
    int pageSize = normalizeSize(request.getSize());
    int from = pageNum * pageSize;
    try {
      SearchResponse<Map> response =
          elasticsearchClient.search(
              SearchRequest.of(
                  s -> {
                    SearchRequest.Builder builder =
                        s.index(SHOP_INDEX)
                            .query(buildShopQuery(request))
                            .from(from)
                            .size(pageSize)
                            .source(src -> src.fetch(true));
                    for (SortOptions sortOption :
                        buildShopSortOptions(request.getSortBy(), request.getSortOrder())) {
                      builder.sort(sortOption);
                    }
                    return builder;
                  }),
              Map.class);
      List<ShopDocument> list =
          response.hits().hits().stream()
              .map(
                  hit ->
                      hit.source() == null
                          ? null
                          : objectMapper.convertValue(hit.source(), ShopDocument.class))
              .filter(Objects::nonNull)
              .toList();
      long total = response.hits().total() == null ? list.size() : response.hits().total().value();
      return SearchResultDTO.of(list, total, pageNum, pageSize, System.currentTimeMillis() - start);
    } catch (Exception e) {
      log.error("Optimized shop search failed", e);
      return SearchResultDTO.of(
          List.of(), 0L, pageNum, pageSize, System.currentTimeMillis() - start);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getSearchSuggestions(String keyword, Integer size) {
    if (StrUtil.isBlank(keyword)) {
      return Collections.emptyList();
    }
    int limit = size == null || size <= 0 ? 10 : Math.min(size, 50);
    try {
      Page<ShopDocument> page =
          shopDocumentRepository.findByShopNameContainingAndStatus(
              keyword, ACTIVE_STATUS, PageRequest.of(0, limit));
      return page.getContent().stream()
          .map(ShopDocument::getShopName)
          .filter(StrUtil::isNotBlank)
          .distinct()
          .limit(limit)
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Get shop suggestions failed", e);
      return Collections.emptyList();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShopDocument> getHotShops(Integer size) {
    int limit = size == null || size <= 0 ? 10 : Math.min(size, 100);
    try {
      Page<ShopDocument> page =
          shopDocumentRepository.findByStatus(
              ACTIVE_STATUS, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "hotScore")));
      return page.getContent();
    } catch (Exception e) {
      log.error("Get hot shops failed", e);
      return Collections.emptyList();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public SearchResultDTO<ShopDocument> getShopFilters(ShopSearchRequest request) {
    SearchResultDTO<ShopDocument> base = searchShops(request);
    base.setAggregations(queryShopAggregations(request));
    return base;
  }

  private Page<ShopDocument> selectPage(ShopSearchRequest request, Pageable pageable) {
    int effectiveStatus = request.getStatus() != null ? request.getStatus() : ACTIVE_STATUS;
    if (StrUtil.isNotBlank(request.getKeyword())) {
      return shopDocumentRepository.searchByKeywordAndStatus(
          request.getKeyword(), effectiveStatus, pageable);
    }
    if (request.getMerchantId() != null) {
      return shopDocumentRepository.findByMerchantIdAndStatus(
          request.getMerchantId(), effectiveStatus, pageable);
    }
    if (request.getRecommended() != null) {
      return shopDocumentRepository.findByRecommendedAndStatus(
          request.getRecommended(), effectiveStatus, pageable);
    }
    if (StrUtil.isNotBlank(request.getAddressKeyword())) {
      return shopDocumentRepository.findByAddressContainingAndStatus(
          request.getAddressKeyword(), effectiveStatus, pageable);
    }
    if (request.getMinRating() != null) {
      return shopDocumentRepository.advancedSearch(
          StrUtil.isNotBlank(request.getKeyword()) ? request.getKeyword() : "",
          request.getMinRating(),
          effectiveStatus,
          pageable);
    }
    return shopDocumentRepository.findByStatus(effectiveStatus, pageable);
  }

  private Map<String, Object> queryShopAggregations(ShopSearchRequest request) {
    try {
      SearchResponse<Map> response =
          elasticsearchClient.search(
              SearchRequest.of(
                  s ->
                      s.index(SHOP_INDEX)
                          .query(buildShopQuery(request))
                          .size(0)
                          .aggregations(buildShopAggregations())),
              Map.class);
      return processShopAggregations(response.aggregations());
    } catch (Exception e) {
      log.error("Query shop aggregations failed", e);
      return Map.of();
    }
  }

  private Query buildShopQuery(ShopSearchRequest request) {
    ShopSearchRequest safeRequest = request == null ? new ShopSearchRequest() : request;
    int effectiveStatus = safeRequest.getStatus() != null ? safeRequest.getStatus() : ACTIVE_STATUS;
    BoolQuery.Builder boolQuery = new BoolQuery.Builder();

    if (StrUtil.isNotBlank(safeRequest.getKeyword())) {
      boolQuery.must(
          Query.of(
              q ->
                  q.multiMatch(
                      m ->
                          m.query(safeRequest.getKeyword())
                              .fields("shopName^3", "description", "address")
                              .type(TextQueryType.BestFields)
                              .fuzziness("AUTO"))));
    }

    boolQuery.filter(
        Query.of(q -> q.term(t -> t.field("status").value(FieldValue.of(effectiveStatus)))));

    if (safeRequest.getMerchantId() != null) {
      boolQuery.filter(
          Query.of(
              q ->
                  q.term(
                      t ->
                          t.field("merchantId")
                              .value(FieldValue.of(safeRequest.getMerchantId())))));
    }
    if (safeRequest.getRecommended() != null) {
      boolQuery.filter(
          Query.of(
              q ->
                  q.term(
                      t ->
                          t.field("recommended")
                              .value(FieldValue.of(safeRequest.getRecommended())))));
    }
    if (StrUtil.isNotBlank(safeRequest.getAddressKeyword())) {
      boolQuery.filter(
          Query.of(q -> q.match(m -> m.field("address").query(safeRequest.getAddressKeyword()))));
    }
    if (safeRequest.getMinRating() != null) {
      boolQuery.filter(
          Query.of(
              q ->
                  q.range(
                      r ->
                          r.number(
                              n ->
                                  n.field("rating")
                                      .gte(safeRequest.getMinRating().doubleValue())))));
    }
    if (safeRequest.getMinProductCount() != null) {
      boolQuery.filter(
          Query.of(
              q ->
                  q.range(
                      r ->
                          r.number(
                              n ->
                                  n.field("productCount")
                                      .gte(
                                          (double)
                                              Math.max(0, safeRequest.getMinProductCount()))))));
    }
    if (safeRequest.getMinFollowCount() != null) {
      boolQuery.filter(
          Query.of(
              q ->
                  q.range(
                      r ->
                          r.number(
                              n ->
                                  n.field("followCount")
                                      .gte(
                                          (double)
                                              Math.max(0, safeRequest.getMinFollowCount()))))));
    }
    return Query.of(q -> q.bool(boolQuery.build()));
  }

  private Map<String, Aggregation> buildShopAggregations() {
    Map<String, Aggregation> aggregations = new LinkedHashMap<>();
    aggregations.put("statusCount", Aggregation.of(a -> a.terms(t -> t.field("status").size(10))));
    aggregations.put(
        "recommendCount", Aggregation.of(a -> a.terms(t -> t.field("recommended").size(2))));
    return aggregations;
  }

  private Map<String, Object> processShopAggregations(Map<String, Aggregate> aggregations) {
    if (aggregations == null || aggregations.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {
      Aggregate aggregate = entry.getValue();
      if (!aggregate.isLterms()) {
        continue;
      }
      LongTermsAggregate terms = aggregate.lterms();
      Map<Object, Long> buckets = new LinkedHashMap<>();
      for (LongTermsBucket bucket : terms.buckets().array()) {
        buckets.put(normalizeShopAggregationKey(entry.getKey(), bucket.key()), bucket.docCount());
      }
      result.put(entry.getKey(), buckets);
    }
    return result;
  }

  private Object normalizeShopAggregationKey(String aggregationName, long key) {
    if ("recommendCount".equals(aggregationName)) {
      return key != 0L;
    }
    return Math.toIntExact(key);
  }

  private boolean requiresOptimizedShopSearch(ShopSearchRequest request) {
    if (request == null) {
      return false;
    }
    if (request.getMinProductCount() != null || request.getMinFollowCount() != null) {
      return true;
    }
    int filterCount = 0;
    if (StrUtil.isNotBlank(request.getKeyword())) {
      filterCount++;
    }
    if (request.getMerchantId() != null) {
      filterCount++;
    }
    if (request.getRecommended() != null) {
      filterCount++;
    }
    if (StrUtil.isNotBlank(request.getAddressKeyword())) {
      filterCount++;
    }
    if (request.getMinRating() != null) {
      filterCount++;
    }
    return filterCount > 1;
  }

  private List<SortOptions> buildShopSortOptions(String sortBy, String sortOrder) {
    List<SortOptions> sortOptions = new ArrayList<>();
    String field = StrUtil.isNotBlank(sortBy) ? sortBy : "createdAt";
    SortOrder order = "asc".equalsIgnoreCase(sortOrder) ? SortOrder.Asc : SortOrder.Desc;
    sortOptions.add(SortOptions.of(s -> s.field(f -> f.field(field).order(order))));
    return sortOptions;
  }

  private int normalizePage(Integer page) {
    return page == null || page < 0 ? 0 : page;
  }

  private int normalizeSize(Integer size) {
    if (size == null || size <= 0) {
      return 20;
    }
    return Math.min(size, 100);
  }

  private Sort buildSort(String sortBy, String sortOrder) {
    String field = StrUtil.isNotBlank(sortBy) ? sortBy : "createdAt";
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
    return Sort.by(direction, field);
  }
}
