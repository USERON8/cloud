package com.cloud.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.impl.ProductSearchServiceImpl;
import com.cloud.search.service.support.HotKeywordKeys;
import com.cloud.search.service.support.SearchHotDataCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceImplTest {

  @Mock private ProductDocumentRepository productDocumentRepository;

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ElasticsearchOptimizedService elasticsearchOptimizedService;

  @Mock private ZSetOperations<String, String> zSetOperations;

  @Mock private SearchHotDataCacheService searchHotDataCacheService;

  private ProductSearchServiceImpl productSearchService;

  @BeforeEach
  void setUp() {
    productSearchService =
        new ProductSearchServiceImpl(
            productDocumentRepository,
            redisTemplate,
            elasticsearchOptimizedService,
            searchHotDataCacheService,
            new ObjectMapper());
  }

  @Test
  void searchProducts_withKeyword_recordsHotSearch() {
    ProductSearchRequest request = new ProductSearchRequest();
    request.setKeyword("Laptop");
    request.setPage(0);
    request.setSize(10);

    when(productDocumentRepository.combinedSearch(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(new ProductDocument()), PageRequest.of(0, 10), 1));
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    ReflectionTestUtils.setField(productSearchService, "hotKeywordDailyTtlDays", 7L);

    productSearchService.searchProducts(request);

    String dailyKey = HotKeywordKeys.todayKey();
    verify(zSetOperations).incrementScore(dailyKey, "laptop", 1.0D);
    verify(redisTemplate).expire(dailyKey, 7L, TimeUnit.DAYS);
    verify(zSetOperations).incrementScore(HotKeywordKeys.TOTAL_KEY, "laptop", 1.0D);
  }

  @Test
  void getSearchSuggestions_repositoryError_returnsEmpty() {
    when(productDocumentRepository.findSuggestions("phone"))
        .thenThrow(new RuntimeException("boom"));

    List<String> result = productSearchService.getSearchSuggestions("phone", 5);

    assertThat(result).isEmpty();
  }

  @Test
  void getHotProducts_shouldSortByHotScore() {
    when(productDocumentRepository.findByIsHotTrueAndStatus(any(), any()))
        .thenReturn(new PageImpl<>(List.of(new ProductDocument()), PageRequest.of(0, 10), 1));

    productSearchService.getHotProducts(0, 10);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(productDocumentRepository)
        .findByIsHotTrueAndStatus(org.mockito.ArgumentMatchers.eq(1), pageableCaptor.capture());
    Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("hotScore");
    assertThat(order).isNotNull();
    assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void getRecommendedProducts_shouldUseActiveStatus() {
    when(productDocumentRepository.findByRecommendedTrueAndStatus(any(), any()))
        .thenReturn(new PageImpl<>(List.of(new ProductDocument()), PageRequest.of(0, 5), 1));

    productSearchService.getRecommendedProducts(0, 5);

    verify(productDocumentRepository)
        .findByRecommendedTrueAndStatus(org.mockito.ArgumentMatchers.eq(1), any());
  }

  @Test
  void getNewProducts_shouldUseActiveStatus() {
    when(productDocumentRepository.findByIsNewTrueAndStatus(any(), any()))
        .thenReturn(new PageImpl<>(List.of(new ProductDocument()), PageRequest.of(0, 5), 1));

    productSearchService.getNewProducts(0, 5);

    verify(productDocumentRepository)
        .findByIsNewTrueAndStatus(org.mockito.ArgumentMatchers.eq(1), any());
  }

  @Test
  void getTodayHotSellingProducts_shouldUseRedisRankOrder() {
    ProductDocument first = new ProductDocument();
    first.setId("1002");
    first.setStatus(1);
    ProductDocument second = new ProductDocument();
    second.setId("1001");
    second.setStatus(1);

    when(searchHotDataCacheService.getTodayHotProductIds(any()))
        .thenReturn(List.of("1001", "1002"));
    when(productDocumentRepository.findAllById(List.of("1001", "1002")))
        .thenReturn(List.of(second, first));

    var result = productSearchService.getTodayHotSellingProducts(0, 2);

    assertThat(result.getTotal()).isEqualTo(2L);
    assertThat(result.getList()).extracting(ProductDocument::getId).containsExactly("1001", "1002");
  }

  @Test
  void getTodayHotSellingProducts_shouldFilterInactiveProductsFromTotal() {
    ProductDocument active = new ProductDocument();
    active.setId("1001");
    active.setStatus(1);
    ProductDocument inactive = new ProductDocument();
    inactive.setId("1002");
    inactive.setStatus(0);

    when(searchHotDataCacheService.getTodayHotProductIds(any()))
        .thenReturn(List.of("1001", "1002"));
    when(productDocumentRepository.findAllById(List.of("1001", "1002")))
        .thenReturn(List.of(active, inactive));

    var result = productSearchService.getTodayHotSellingProducts(0, 10);

    assertThat(result.getTotal()).isEqualTo(1L);
    assertThat(result.getList()).extracting(ProductDocument::getId).containsExactly("1001");
    assertThat(result.getHasNext()).isFalse();
  }

  @Test
  void getProductFilters_shouldUseElasticsearchAggregations() {
    ProductSearchRequest request = new ProductSearchRequest();
    request.setKeyword("phone");
    request.setPage(1);
    request.setSize(5);
    request.setIncludeAggregations(false);

    var esResult =
        ElasticsearchOptimizedService.SearchResultDTO.builder()
            .documents(List.of(Map.of("productName", "Cloud Phone", "brandName", "Cloud")))
            .total(9L)
            .from(5)
            .size(5)
            .aggregations(Map.of("categories", Map.of("Phones", 9L), "brands", Map.of("Cloud", 7L)))
            .searchAfter(List.of())
            .build();

    when(elasticsearchOptimizedService.productSearchAfter(any(), eq(List.of())))
        .thenReturn(esResult);

    var result = productSearchService.getProductFilters(request);

    ArgumentCaptor<ProductSearchRequest> captor =
        ArgumentCaptor.forClass(ProductSearchRequest.class);
    verify(elasticsearchOptimizedService).productSearchAfter(captor.capture(), eq(List.of()));
    verifyNoInteractions(productDocumentRepository);
    assertThat(captor.getValue().getIncludeAggregations()).isTrue();
    assertThat(result.getAggregations())
        .isEqualTo(Map.of("categories", Map.of("Phones", 9L), "brands", Map.of("Cloud", 7L)));
    assertThat(result.getList())
        .extracting(ProductDocument::getProductName)
        .containsExactly("Cloud Phone");
  }

  @Test
  void getProductFilters_shouldExposeHighlightsFromElasticsearch() {
    ProductSearchRequest request = new ProductSearchRequest();
    request.setKeyword("cloud");
    request.setPage(0);
    request.setSize(5);

    var esResult =
        ElasticsearchOptimizedService.SearchResultDTO.builder()
            .documents(List.of(Map.of("id", "1001", "productName", "Cloud Phone")))
            .total(1L)
            .from(0)
            .size(5)
            .aggregations(Map.of())
            .highlights(Map.of("1001", List.of("<em class='highlight'>Cloud</em> Phone")))
            .searchAfter(List.of())
            .build();

    when(elasticsearchOptimizedService.productSearchAfter(any(), eq(List.of())))
        .thenReturn(esResult);

    var result = productSearchService.getProductFilters(request);

    assertThat(result.getHighlights())
        .containsEntry("1001", List.of("<em class='highlight'>Cloud</em> Phone"));
    assertThat(result.getList()).extracting(ProductDocument::getId).containsExactly("1001");
  }
}
