package com.cloud.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.mapper.SearchRequestMapper;
import com.cloud.search.service.support.SearchHotDataCacheService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchFacadeServiceTest {

  @Mock private ProductSearchService productSearchService;

  @Mock private ElasticsearchOptimizedService elasticsearchOptimizedService;

  @Mock private SearchRequestMapper searchRequestMapper;

  @Mock private SearchHotDataCacheService searchHotDataCacheService;

  private SearchFacadeService searchFacadeService;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    searchFacadeService =
        new SearchFacadeService(
            productSearchService,
            elasticsearchOptimizedService,
            searchRequestMapper,
            searchHotDataCacheService,
            objectMapper);
  }

  @Test
  void getHotSearchKeywordsShouldUseRedisHotCacheService() {
    when(searchHotDataCacheService.getHotKeywords(eq(6), any()))
        .thenReturn(List.of("iphone", "ipad"));

    List<String> result = searchFacadeService.getHotSearchKeywords(6);

    assertThat(result).containsExactly("iphone", "ipad");
    verify(searchHotDataCacheService).getHotKeywords(eq(6), any());
    verifyNoInteractions(productSearchService);
  }

  @Test
  void getProductFiltersShouldUseElasticsearchAggregations() {
    ProductSearchRequest request = new ProductSearchRequest();
    request.setKeyword("phone");
    request.setPage(2);
    request.setSize(5);
    request.setIncludeAggregations(false);

    Map<String, Object> aggregations =
        Map.of("categories", Map.of("Phones", 9L), "brands", Map.of("Cloud", 7L));
    ElasticsearchOptimizedService.SearchResultDTO esResult =
        ElasticsearchOptimizedService.SearchResultDTO.builder()
            .documents(List.of(Map.of("productName", "Cloud Phone")))
            .total(9L)
            .from(10)
            .size(5)
            .aggregations(aggregations)
            .searchAfter(List.of())
            .build();

    when(elasticsearchOptimizedService.productSearchAfter(any(), eq(List.of())))
        .thenReturn(esResult);

    var result = searchFacadeService.getProductFilters(request, null);

    ArgumentCaptor<ProductSearchRequest> captor =
        ArgumentCaptor.forClass(ProductSearchRequest.class);
    verify(elasticsearchOptimizedService).productSearchAfter(captor.capture(), eq(List.of()));
    verifyNoInteractions(productSearchService);
    assertThat(captor.getValue().getIncludeAggregations()).isTrue();
    assertThat(result.getAggregations()).isEqualTo(aggregations);
    assertThat(result.getList()).hasSize(1);
    assertThat(result.getList().get(0).getProductName()).isEqualTo("Cloud Phone");
  }

  @Test
  void searchProductsShouldUseOptimizedServiceForExtendedFilters() {
    ProductSearchRequest request = new ProductSearchRequest();
    request.setKeyword("phone");
    request.setMinSalesCount(3);
    request.setPage(1);
    request.setSize(10);

    ElasticsearchOptimizedService.SearchResultDTO esResult =
        ElasticsearchOptimizedService.SearchResultDTO.builder()
            .documents(List.of(Map.of("productName", "Sales Phone")))
            .total(1L)
            .from(10)
            .size(10)
            .aggregations(Map.of())
            .searchAfter(List.of())
            .build();

    when(elasticsearchOptimizedService.productSearchAfter(eq(request), eq(List.of())))
        .thenReturn(esResult);

    var result = searchFacadeService.searchProducts(request, null);

    verify(elasticsearchOptimizedService).productSearchAfter(eq(request), eq(List.of()));
    verifyNoInteractions(productSearchService);
    assertThat(result.getList()).extracting("productName").containsExactly("Sales Phone");
  }

  @Test
  void searchProductsShouldStopUsingPageFallbackForCursorPagination() {
    ProductSearchRequest request = new ProductSearchRequest();
    request.setKeyword("phone");
    request.setPage(0);
    request.setSize(10);

    ElasticsearchOptimizedService.SearchResultDTO esResult =
        ElasticsearchOptimizedService.SearchResultDTO.builder()
            .documents(List.of(Map.of("productName", "Paged Phone")))
            .total(25L)
            .from(0)
            .size(10)
            .aggregations(Map.of())
            .searchAfter(List.of())
            .build();

    when(elasticsearchOptimizedService.productSearchAfter(any(), any())).thenReturn(esResult);

    var result = searchFacadeService.searchProducts(request, "[1,\"cursor\"]");

    verify(elasticsearchOptimizedService).productSearchAfter(any(), any());
    verifyNoInteractions(productSearchService);
    assertThat(result.getHasPrevious()).isTrue();
    assertThat(result.getHasNext()).isFalse();
  }

  @Test
  void searchProductsShouldExposeHighlightsFromElasticsearch() {
    ProductSearchRequest request = new ProductSearchRequest();
    request.setKeyword("cloud");
    request.setHighlight(true);
    request.setPage(0);
    request.setSize(10);

    ElasticsearchOptimizedService.SearchResultDTO esResult =
        ElasticsearchOptimizedService.SearchResultDTO.builder()
            .documents(List.of(Map.of("id", "1001", "productName", "Cloud Phone")))
            .total(1L)
            .from(0)
            .size(10)
            .aggregations(Map.of())
            .highlights(Map.of("1001", List.of("<em class='highlight'>Cloud</em> Phone")))
            .searchAfter(List.of())
            .build();

    when(elasticsearchOptimizedService.productSearchAfter(eq(request), eq(List.of())))
        .thenReturn(esResult);

    var result = searchFacadeService.searchProducts(request, null);

    assertThat(result.getHighlights())
        .containsEntry("1001", List.of("<em class='highlight'>Cloud</em> Phone"));
    assertThat(result.getList()).extracting("id").containsExactly("1001");
  }

  @Test
  void searchProductsShouldAcceptLegacyDateTimeFormatFromElasticsearchDocuments() {
    ProductSearchRequest request = new ProductSearchRequest();
    request.setKeyword("watch");
    request.setPage(0);
    request.setSize(10);

    ElasticsearchOptimizedService.SearchResultDTO esResult =
        ElasticsearchOptimizedService.SearchResultDTO.builder()
            .documents(
                List.of(
                    Map.of(
                        "id", "2001",
                        "productName", "Legacy Watch",
                        "createdAt", "2026-04-01 03:04:49",
                        "updatedAt", "2026-04-02 05:06:07")))
            .total(1L)
            .from(0)
            .size(10)
            .aggregations(Map.of())
            .searchAfter(List.of())
            .build();

    when(elasticsearchOptimizedService.productSearchAfter(eq(request), any())).thenReturn(esResult);

    var result = searchFacadeService.searchProducts(request, "[1]");

    assertThat(result.getList()).hasSize(1);
    assertThat(result.getList().get(0).getCreatedAt())
        .isEqualTo(LocalDateTime.of(2026, 4, 1, 3, 4, 49));
    assertThat(result.getList().get(0).getUpdatedAt())
        .isEqualTo(LocalDateTime.of(2026, 4, 2, 5, 6, 7));
  }
}
