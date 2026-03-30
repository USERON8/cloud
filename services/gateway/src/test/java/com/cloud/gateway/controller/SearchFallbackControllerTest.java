package com.cloud.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.gateway.cache.SearchFallbackCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

class SearchFallbackControllerTest {

  private SearchFallbackController controller;

  @BeforeEach
  void setUp() {
    controller =
        new SearchFallbackController(
            WebClient.builder(),
            new ObjectMapper(),
            new SimpleMeterRegistry(),
            Mockito.mock(SearchFallbackCache.class));
  }

  @Test
  void buildCacheKeyShouldIgnoreIrrelevantSearchParams() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("keyword", "  phone  ");
    queryParams.add("page", "3");
    queryParams.add("sortField", "price");
    queryParams.add("unused", "noise");

    MultiValueMap<String, String> normalized = normalizedParams("search", queryParams);

    String cacheKey =
        (String)
            ReflectionTestUtils.invokeMethod(controller, "buildCacheKey", "search", normalized);

    assertThat(cacheKey).isEqualTo("search|keyword=phone");
  }

  @Test
  void buildCacheKeyShouldCanonicalizeSuggestionsParams() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("keyword", "  iphone  ");
    queryParams.add("size", "999");
    queryParams.add("page", "2");

    MultiValueMap<String, String> normalized = normalizedParams("suggestions", queryParams);

    String cacheKey =
        (String)
            ReflectionTestUtils.invokeMethod(
                controller, "buildCacheKey", "suggestions", normalized);

    assertThat(cacheKey).isEqualTo("suggestions|keyword=iphone|size=50");
  }

  @Test
  void buildCacheKeyShouldKeepEmptySearchKeywordAsRouteOnly() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("keyword", "   ");
    queryParams.add("page", "9");

    MultiValueMap<String, String> normalized = normalizedParams("search", queryParams);

    String cacheKey =
        (String)
            ReflectionTestUtils.invokeMethod(controller, "buildCacheKey", "search", normalized);

    assertThat(cacheKey).isEqualTo("search");
  }

  @SuppressWarnings("unchecked")
  private MultiValueMap<String, String> normalizedParams(
      String routeType, MultiValueMap<String, String> queryParams) {
    return (MultiValueMap<String, String>)
        ReflectionTestUtils.invokeMethod(
            controller, "normalizeCacheQueryParams", routeType, queryParams);
  }
}
