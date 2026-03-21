package com.cloud.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.ShopSearchRequest;
import com.cloud.search.repository.ShopDocumentRepository;
import com.cloud.search.service.impl.ShopSearchServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  private ShopSearchServiceImpl shopSearchService;

  @BeforeEach
  void setUp() {
    shopSearchService =
        new ShopSearchServiceImpl(
            shopDocumentRepository,
            elasticsearchClient,
            elasticsearchOperations,
            redisTemplate,
            new ObjectMapper());
  }

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
  void searchShopsShouldUseElasticsearchForCombinedFilters() throws Exception {
    ShopSearchRequest request = new ShopSearchRequest();
    request.setKeyword("cloud");
    request.setRecommended(true);

    SearchResponse<Map> response = org.mockito.Mockito.mock(SearchResponse.class);
    HitsMetadata<Map> hits = org.mockito.Mockito.mock(HitsMetadata.class);
    TotalHits totalHits = TotalHits.of(total -> total.value(1L).relation(TotalHitsRelation.Eq));

    when(elasticsearchClient.search(
            any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(Map.class)))
        .thenReturn(response);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(totalHits);
    when(hits.hits())
        .thenReturn(
            List.of(
                Hit.of(
                    hit ->
                        hit.index("shop_index")
                            .id("2")
                            .source(
                                Map.of(
                                    "shopId",
                                    22,
                                    "shopName",
                                    "Combined Shop",
                                    "status",
                                    1,
                                    "recommended",
                                    true)))));

    var result = shopSearchService.searchShops(request);

    ArgumentCaptor<SearchRequest> searchRequestCaptor =
        ArgumentCaptor.forClass(SearchRequest.class);
    verify(elasticsearchClient).search(searchRequestCaptor.capture(), eq(Map.class));
    verifyNoInteractions(shopDocumentRepository);
    assertThat(searchRequestCaptor.getValue().trackTotalHits()).isNotNull();
    assertThat(searchRequestCaptor.getValue().trackTotalHits().enabled()).isTrue();
    assertThat(result.getList())
        .extracting(ShopDocument::getShopName)
        .containsExactly("Combined Shop");
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

  @Test
  @SuppressWarnings("unchecked")
  void searchShopsShouldUseElasticsearchForCountFilters() throws Exception {
    ShopSearchRequest request = new ShopSearchRequest();
    request.setMinProductCount(5);

    SearchResponse<Map> response = org.mockito.Mockito.mock(SearchResponse.class);
    HitsMetadata<Map> hits = org.mockito.Mockito.mock(HitsMetadata.class);
    TotalHits totalHits = TotalHits.of(total -> total.value(1L).relation(TotalHitsRelation.Eq));

    when(elasticsearchClient.search(
            any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(Map.class)))
        .thenReturn(response);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(totalHits);
    when(hits.hits())
        .thenReturn(
            List.of(
                Hit.of(
                    hit ->
                        hit.index("shop_index")
                            .id("1")
                            .source(
                                Map.of(
                                    "shopId", 11,
                                    "shopName", "Counted Shop",
                                    "status", 1,
                                    "productCount", 8)))));

    var result = shopSearchService.searchShops(request);

    verify(elasticsearchClient)
        .search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(Map.class));
    verifyNoInteractions(shopDocumentRepository);
    assertThat(result.getList())
        .extracting(ShopDocument::getShopName)
        .containsExactly("Counted Shop");
    assertThat(result.getTotal()).isEqualTo(1L);
  }

  @Test
  @SuppressWarnings("unchecked")
  void searchShopsShouldReturnAggregationsAndHighlightsFromElasticsearch() throws Exception {
    ShopSearchRequest request = new ShopSearchRequest();
    request.setKeyword("cloud");
    request.setIncludeAggregations(true);
    request.setHighlight(true);

    SearchResponse<Map> response = org.mockito.Mockito.mock(SearchResponse.class);
    HitsMetadata<Map> hits = org.mockito.Mockito.mock(HitsMetadata.class);
    TotalHits totalHits = TotalHits.of(total -> total.value(1L).relation(TotalHitsRelation.Eq));

    when(elasticsearchClient.search(
            any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(Map.class)))
        .thenReturn(response);
    when(response.hits()).thenReturn(hits);
    when(hits.total()).thenReturn(totalHits);
    when(hits.hits())
        .thenReturn(
            List.of(
                Hit.of(
                    hit ->
                        hit.index("shop_index")
                            .id("12")
                            .source(
                                Map.of(
                                    "shopId",
                                    12,
                                    "shopName",
                                    "Cloud Highlight Shop",
                                    "status",
                                    1,
                                    "recommended",
                                    true))
                            .highlight(
                                Map.of(
                                    "shopName",
                                    List.of("<em class='highlight'>Cloud</em> Highlight Shop"))))));
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
                                                    bucket -> bucket.key(1).docCount(1L))))))),
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
                                                    bucket -> bucket.key(1).docCount(1L)))))))));

    var result = shopSearchService.searchShops(request);

    verify(elasticsearchClient)
        .search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), eq(Map.class));
    verifyNoInteractions(shopDocumentRepository);
    assertThat(result.getAggregations())
        .isEqualTo(Map.of("statusCount", Map.of(1, 1L), "recommendCount", Map.of(true, 1L)));
    assertThat(result.getHighlights())
        .containsEntry("12", List.of("<em class='highlight'>Cloud</em> Highlight Shop"));
  }
}
