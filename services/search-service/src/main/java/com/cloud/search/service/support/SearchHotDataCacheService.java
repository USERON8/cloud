package com.cloud.search.service.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHotDataCacheService {

  private static final String HOT_KEYWORD_LIST_KEY_PREFIX = "search:hot:list:";
  private static final String SELL_RANK_ID_LIST_KEY = "search:sell-rank:today:ids";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Value("${search.cache.hot-keywords.ttl-seconds:60}")
  private long hotKeywordCacheTtlSeconds;

  @Value("${search.cache.today-hot-product-ids.ttl-seconds:120}")
  private long todayHotProductIdsCacheTtlSeconds;

  public List<String> getHotKeywords(int limit, Supplier<List<String>> loader) {
    String key = HOT_KEYWORD_LIST_KEY_PREFIX + Math.max(1, limit);
    return getOrLoadList(key, Math.max(10L, hotKeywordCacheTtlSeconds), loader);
  }

  public List<String> getTodayHotProductIds(Supplier<List<String>> loader) {
    return getOrLoadList(
        SELL_RANK_ID_LIST_KEY, Math.max(10L, todayHotProductIdsCacheTtlSeconds), loader);
  }

  public void evictHotKeywords(int limit) {
    redisTemplate.delete(HOT_KEYWORD_LIST_KEY_PREFIX + Math.max(1, limit));
  }

  public void evictTodayHotProductIds() {
    redisTemplate.delete(SELL_RANK_ID_LIST_KEY);
  }

  private List<String> getOrLoadList(String key, long ttlSeconds, Supplier<List<String>> loader) {
    try {
      String cached = redisTemplate.opsForValue().get(key);
      if (cached != null && !cached.isBlank()) {
        return objectMapper.readValue(cached, new TypeReference<List<String>>() {});
      }
    } catch (Exception ex) {
      log.warn("Read search hot data cache failed: key={}", key, ex);
    }

    List<String> loaded = loader.get();
    List<String> normalized =
        loaded == null ? List.of() : loaded.stream().filter(Objects::nonNull).toList();
    try {
      redisTemplate
          .opsForValue()
          .set(key, objectMapper.writeValueAsString(normalized), ttlSeconds, TimeUnit.SECONDS);
    } catch (Exception ex) {
      log.warn("Write search hot data cache failed: key={}", key, ex);
    }
    return normalized;
  }
}
