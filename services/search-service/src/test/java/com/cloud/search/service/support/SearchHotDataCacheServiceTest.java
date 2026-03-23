package com.cloud.search.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SearchHotDataCacheServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  private SearchHotDataCacheService searchHotDataCacheService;

  @BeforeEach
  void setUp() {
    searchHotDataCacheService = new SearchHotDataCacheService(redisTemplate, new ObjectMapper());
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    ReflectionTestUtils.setField(searchHotDataCacheService, "hotKeywordCacheTtlSeconds", 60L);
    ReflectionTestUtils.setField(
        searchHotDataCacheService, "todayHotProductIdsCacheTtlSeconds", 120L);
  }

  @Test
  void getHotKeywords_shouldReadCacheFirst() {
    when(valueOperations.get("search:hot:list:3")).thenReturn("[\"iphone\",\"ipad\"]");

    List<String> result = searchHotDataCacheService.getHotKeywords(3, List::<String>of);

    assertThat(result).containsExactly("iphone", "ipad");
  }

  @Test
  void getHotKeywords_shouldWriteLoadedValueToRedis() {
    List<String> result =
        searchHotDataCacheService.getHotKeywords(2, () -> List.of("iphone", "xiaomi"));

    assertThat(result).containsExactly("iphone", "xiaomi");
    verify(valueOperations)
        .set("search:hot:list:2", "[\"iphone\",\"xiaomi\"]", 60L, TimeUnit.SECONDS);
  }

  @Test
  void getTodayHotProductIds_shouldWriteLoadedValueToRedis() {
    List<String> result =
        searchHotDataCacheService.getTodayHotProductIds(() -> List.of("1001", "1002"));

    assertThat(result).containsExactly("1001", "1002");
    verify(valueOperations, times(1))
        .set("search:sell-rank:today:ids", "[\"1001\",\"1002\"]", 120L, TimeUnit.SECONDS);
  }
}
