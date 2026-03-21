package com.cloud.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.ShopSearchRequest;
import com.cloud.search.repository.ShopDocumentRepository;
import com.cloud.search.service.impl.ShopSearchServiceImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class ShopSearchServiceImplTest {

  @Mock private ShopDocumentRepository shopDocumentRepository;

  @Mock private ElasticsearchClient elasticsearchClient;

  @Mock private ElasticsearchOperations elasticsearchOperations;

  @Mock private StringRedisTemplate redisTemplate;

  @InjectMocks private ShopSearchServiceImpl shopSearchService;

  @Test
  void searchShopsShouldDefaultToActiveStatusForKeyword() {
    ShopSearchRequest request = new ShopSearchRequest();
    request.setKeyword("cloud");
    ShopDocument document = new ShopDocument();
    document.setShopName("Cloud Store");

    when(shopDocumentRepository.searchByKeywordAndStatus(eq("cloud"), eq(1), any()))
        .thenReturn(new PageImpl<>(List.of(document), PageRequest.of(0, 20), 1));

    var result = shopSearchService.searchShops(request);

    assertThat(result.getList()).hasSize(1);
    verify(shopDocumentRepository).searchByKeywordAndStatus(eq("cloud"), eq(1), any());
  }

  @Test
  void getSearchSuggestionsShouldUseActiveStatus() {
    ShopDocument active = new ShopDocument();
    active.setShopName("Cloud Shop");

    when(shopDocumentRepository.findByShopNameContainingAndStatus(eq("cloud"), eq(1), any()))
        .thenReturn(new PageImpl<>(List.of(active), PageRequest.of(0, 10), 1));

    List<String> result = shopSearchService.getSearchSuggestions("cloud", 10);

    assertThat(result).containsExactly("Cloud Shop");
    verify(shopDocumentRepository).findByShopNameContainingAndStatus(eq("cloud"), eq(1), any());
  }

  @Test
  void searchShopsShouldUseActiveFallbackWhenNoFilterProvided() {
    ShopDocument active = new ShopDocument();
    active.setShopName("Active Shop");

    when(shopDocumentRepository.findByStatus(eq(1), any()))
        .thenReturn(new PageImpl<>(List.of(active), PageRequest.of(0, 20), 1));

    var result = shopSearchService.searchShops(new ShopSearchRequest());

    assertThat(result.getList())
        .extracting(ShopDocument::getShopName)
        .containsExactly("Active Shop");
    verify(shopDocumentRepository).findByStatus(eq(1), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getShopFiltersShouldUseElasticsearchAggregations() throws Exception {
    ShopSearchRequest request = new ShopSearchRequest();
    request.setKeyword("cloud");

    ShopDocument active = new ShopDocument();
    active.setShopName("Active Shop");

    when(shopDocumentRepository.searchByKeywordAndStatus(eq("cloud"), eq(1), any()))
        .thenReturn(new PageImpl<>(List.of(active), PageRequest.of(0, 20), 5));

    SearchResponse<Map> response = org.mockito.Mockito.mock(SearchResponse.class);
    when(elasticsearchClient.search(
            any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(Map.class)))
        .thenReturn(response);
    when(response.aggregations())
        .thenReturn(
            Map.of(
                "statusCount",
                Aggregate.of(
                    a ->
                        a.lterms(
                            t ->
                                t.buckets(
                                    b ->
                                        b.array(
                                            List.of(
                                                LongTermsBucket.of(
                                                    bucket -> bucket.key(1).docCount(5L))))))),
                "recommendCount",
                Aggregate.of(
                    a ->
                        a.lterms(
                            t ->
                                t.buckets(
                                    b ->
                                        b.array(
                                            List.of(
                                                LongTermsBucket.of(
                                                    bucket -> bucket.key(1).docCount(3L)),
                                                LongTermsBucket.of(
                                                    bucket -> bucket.key(0).docCount(2L)))))))));

    var result = shopSearchService.getShopFilters(request);

    assertThat(result.getAggregations())
        .isEqualTo(
            Map.of(
                "statusCount", Map.of(1, 5L),
                "recommendCount", Map.of(true, 3L, false, 2L)));
  }
}
