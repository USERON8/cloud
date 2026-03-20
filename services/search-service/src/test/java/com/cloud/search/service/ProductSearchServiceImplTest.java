package com.cloud.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.impl.ProductSearchServiceImpl;
import com.cloud.search.service.support.HotKeywordKeys;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

  @Mock private ZSetOperations<String, String> zSetOperations;

  @InjectMocks private ProductSearchServiceImpl productSearchService;

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
    when(productDocumentRepository.findByIsHotTrue(any()))
        .thenReturn(new PageImpl<>(List.of(new ProductDocument()), PageRequest.of(0, 10), 1));

    productSearchService.getHotProducts(0, 10);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(productDocumentRepository).findByIsHotTrue(pageableCaptor.capture());
    Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("hotScore");
    assertThat(order).isNotNull();
    assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
  }
}
