package com.cloud.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ElasticsearchOptimizedServiceTest {

  @Mock private ElasticsearchClient elasticsearchClient;

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  @Mock private ZSetOperations<String, String> zSetOperations;

  private ElasticsearchOptimizedService service;
  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    Executor directExecutor = Runnable::run;
    meterRegistry = new SimpleMeterRegistry();
    service =
        new ElasticsearchOptimizedService(
            elasticsearchClient, redisTemplate, new ObjectMapper(), meterRegistry, directExecutor);
    ReflectionTestUtils.setField(service, "defaultSearchSize", 20);
    ReflectionTestUtils.setField(service, "defaultKeywordSize", 10);
    ReflectionTestUtils.setField(service, "maxSearchSize", 100);
    ReflectionTestUtils.setField(service, "smartSearchL2TtlSeconds", 120L);
    ReflectionTestUtils.setField(service, "suggestionL2TtlSeconds", 120L);
    ReflectionTestUtils.setField(service, "hotKeywordsL2TtlSeconds", 30L);
    ReflectionTestUtils.setField(service, "recommendationL2TtlSeconds", 60L);
    ReflectionTestUtils.setField(service, "smartSearchL1ExpireAfterWriteMs", 30000L);
    ReflectionTestUtils.setField(service, "suggestionL1ExpireAfterWriteMs", 20000L);
    ReflectionTestUtils.setField(service, "smartSearchL1RefreshAfterWriteMs", 10000L);
    ReflectionTestUtils.setField(service, "suggestionL1RefreshAfterWriteMs", 8000L);
    ReflectionTestUtils.setField(service, "l1MaxEntries", 100);
    ReflectionTestUtils.setField(service, "l1RecordStats", true);
    ReflectionTestUtils.invokeMethod(service, "initL1Caches");
  }

  @Test
  void shouldUseSuggestionL2CacheWithoutCallingElasticsearch() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("search:suggest:phone:5")).thenReturn("[\"phone\",\"phone case\"]");

    List<String> suggestions = service.getSearchSuggestions("phone", 5);

    assertThat(suggestions).containsExactly("phone", "phone case");
    verifyNoInteractions(elasticsearchClient);
  }

  @Test
  void shouldLoadHotKeywordsFromRedisZSetAndWriteRedisCache() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    Set<String> hotKeywords = new LinkedHashSet<>(List.of("iphone", "xiaomi"));
    when(valueOperations.get("search:hot:list:2")).thenReturn(null);
    when(zSetOperations.reverseRange("search:hot:total", 0, 1)).thenReturn(hotKeywords);

    List<String> first = service.getHotSearchKeywords(2);

    assertThat(first).containsExactly("iphone", "xiaomi");
    verify(zSetOperations, times(1)).reverseRange("search:hot:total", 0, 1);
    verify(valueOperations, times(1))
        .set(
            eq("search:hot:list:2"),
            eq("[\"iphone\",\"xiaomi\"]"),
            longThat(ttl -> ttl >= 30L && ttl <= 32L),
            eq(TimeUnit.SECONDS));
  }

  @Test
  void shouldMergeSuggestionsAndHotKeywordsForRecommendations() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("search:recommend:ip:6")).thenReturn(null);
    when(valueOperations.get("search:suggest:ip:6")).thenReturn("[\"iphone\",\"iphone 15\"]");
    when(valueOperations.get("search:hot:list:18")).thenReturn("[\"iphone\",\"ipad\",\"watch\"]");

    List<String> recommendations = service.getKeywordRecommendations("ip", 6);

    assertThat(recommendations).containsExactly("iphone", "iphone 15", "ipad", "watch");
    verifyNoInteractions(elasticsearchClient);
  }

  @Test
  void shouldUseRecommendationRedisCacheWithoutExtraLookup() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("search:recommend:ip:4")).thenReturn("[\"iphone\",\"ipad\"]");

    List<String> recommendations = service.getKeywordRecommendations("ip", 4);

    assertThat(recommendations).containsExactly("iphone", "ipad");
    verifyNoInteractions(elasticsearchClient);
    verifyNoInteractions(zSetOperations);
  }

  @Test
  void shouldExposeCaffeineCacheMetricsForPrometheus() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("search:suggest:phone:5")).thenReturn("[\"phone\"]");

    service.getSearchSuggestions("phone", 5);

    assertThat(meterRegistry.find("cache.gets").tag("cache", "search.suggestions").meter())
        .isNotNull();
    assertThat(meterRegistry.find("cache.evictions").tag("cache", "search.suggestions").meter())
        .isNotNull();
    assertThat(meterRegistry.find("cache.load.duration").tag("cache", "search.suggestions").meter())
        .isNotNull();
  }
}
