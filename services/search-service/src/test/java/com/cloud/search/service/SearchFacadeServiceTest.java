package com.cloud.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.mapper.SearchRequestMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  private SearchFacadeService searchFacadeService;

  @BeforeEach
  void setUp() {
    searchFacadeService =
        new SearchFacadeService(
            productSearchService,
            elasticsearchOptimizedService,
            searchRequestMapper,
            new ObjectMapper());
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
}
