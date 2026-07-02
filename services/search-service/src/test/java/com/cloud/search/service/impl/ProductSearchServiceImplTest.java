package com.cloud.search.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.SearchResultDTO;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.cloud.search.service.support.SearchHotDataCacheService;
import com.cloud.search.service.support.SearchResultSupport;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceImplTest {

  @Mock private ProductDocumentRepository productDocumentRepository;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ElasticsearchOptimizedService elasticsearchOptimizedService;
  @Mock private SearchHotDataCacheService searchHotDataCacheService;
  @Mock private SearchResultSupport searchResultSupport;
  @Mock private ZSetOperations<String, String> zSetOperations;

  @Test
  void getSearchSuggestionsReturnsDistinctNonBlankNamesWithinLimit() {
    ProductSearchServiceImpl service = newService();
    when(productDocumentRepository.findSuggestions("phone"))
        .thenReturn(
            List.of(
                product("Phone"),
                product("Phone"),
                product(""),
                product(null),
                product("Phone Case")));

    List<String> result = service.getSearchSuggestions("phone", 2);

    assertEquals(List.of("Phone", "Phone Case"), result);
    verify(productDocumentRepository).findSuggestions("phone");
  }

  @Test
  void getSearchSuggestionsReturnsEmptyListForBlankKeywordOrRepositoryFailure() {
    ProductSearchServiceImpl service = newService();

    assertEquals(List.of(), service.getSearchSuggestions("   ", 10));
    verifyNoInteractions(productDocumentRepository);

    when(productDocumentRepository.findSuggestions("phone")).thenThrow(new RuntimeException("es down"));
    assertEquals(List.of(), service.getSearchSuggestions("phone", 10));
  }

  @Test
  void basicSearchUsesActiveProductsWhenKeywordIsBlankAndNormalizesPageSize() {
    ProductSearchServiceImpl service = newService();
    ProductDocument document = product("Default");
    when(productDocumentRepository.findByStatus(any(Integer.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(document)));

    SearchResultDTO<ProductDocument> result = service.basicSearch(" ", -1, 500);

    assertEquals(1, result.getList().size());
    assertEquals(0, result.getPage());
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(productDocumentRepository).findByStatus(any(Integer.class), pageableCaptor.capture());
    assertEquals(0, pageableCaptor.getValue().getPageNumber());
    assertEquals(100, pageableCaptor.getValue().getPageSize());
    verify(productDocumentRepository, never()).searchByKeyword(any(), any());
    verifyNoInteractions(redisTemplate);
  }

  @Test
  void basicSearchUsesKeywordSearchAndRecordsHotKeyword() {
    ProductSearchServiceImpl service = newService();
    ProductDocument document = product("Phone");
    when(productDocumentRepository.searchByKeyword(any(String.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(document)));
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

    SearchResultDTO<ProductDocument> result = service.basicSearch(" Phone ", 1, 20);

    assertEquals(1, result.getList().size());
    verify(productDocumentRepository).searchByKeyword(any(String.class), any(Pageable.class));
    verify(redisTemplate).expire(any(String.class), org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(TimeUnit.DAYS));
    verify(zSetOperations).incrementScore(org.mockito.ArgumentMatchers.eq("search:hot:total"), org.mockito.ArgumentMatchers.eq("phone"), org.mockito.ArgumentMatchers.eq(1.0D));
  }

  @Test
  void searchProductsNormalizesRequestAndDelegatesToCombinedSearch() {
    ProductSearchServiceImpl service = newService();
    when(productDocumentRepository.combinedSearch(
            any(), any(), any(), any(), any(), any(), any(Integer.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(product("Phone"))));

    SearchResultDTO<ProductDocument> result = service.searchProducts(null);

    assertEquals(1, result.getList().size());
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(productDocumentRepository)
        .combinedSearch(
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq(1),
            pageableCaptor.capture());
    assertEquals(0, pageableCaptor.getValue().getPageNumber());
    assertEquals(20, pageableCaptor.getValue().getPageSize());
  }

  private ProductSearchServiceImpl newService() {
    ProductSearchServiceImpl service =
        new ProductSearchServiceImpl(
            productDocumentRepository,
            redisTemplate,
            elasticsearchOptimizedService,
            searchHotDataCacheService,
            searchResultSupport);
    ReflectionTestUtils.setField(service, "hotKeywordDailyTtlDays", 7L);
    return service;
  }

  private ProductDocument product(String productName) {
    ProductDocument document = new ProductDocument();
    document.setProductName(productName);
    return document;
  }
}
