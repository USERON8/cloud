package com.cloud.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.api.product.ProductDubboApi;
import com.cloud.common.domain.dto.product.ProductSearchItemDTO;
import com.cloud.gateway.cache.SearchFallbackCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SearchFallbackControllerTest {

  @Mock private SearchFallbackCache searchFallbackCache;
  @Mock private ProductDubboApi productDubboApi;

  private SearchFallbackController controller;

  @BeforeEach
  void setUp() {
    controller =
        new SearchFallbackController(
            new ObjectMapper(), new SimpleMeterRegistry(), searchFallbackCache);
    ReflectionTestUtils.setField(controller, "productDubboApi", productDubboApi);
    ReflectionTestUtils.setField(controller, "fallbackTimeoutMs", 1000L);
  }

  @Test
  void searchFallbackIgnoresForwardPathAndUsesOriginalSearchRoute() {
    ProductSearchItemDTO item = new ProductSearchItemDTO();
    item.setId(50001L);
    item.setName("Cloud Phone 15");

    when(searchFallbackCache.get(anyString(), anyString(), any())).thenReturn(null);
    when(productDubboApi.searchProducts("phone", 2)).thenReturn(List.of(item));

    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/gateway/fallback/search?keyword=phone&page=0&size=2")
                .build());
    Set<URI> originalUris = new LinkedHashSet<>();
    originalUris.add(URI.create("forward:/gateway/fallback/search"));
    originalUris.add(
        URI.create("http://127.0.0.1/api/search/products?keyword=phone&page=0&size=2"));
    exchange
        .getAttributes()
        .put(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR, originalUris);

    ResponseEntity<String> response = controller.searchFallback(exchange, null).block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"code\":200");
    verify(productDubboApi).searchProducts("phone", 2);
    verify(searchFallbackCache).put(anyString(), anyString(), anyString(), any());
  }
}
