package com.cloud.product.service.cache;

import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.product.module.entity.Category;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryRedisCacheService {

  private static final String PREFIX = "product:category:";
  private static final String KEY_ENTITY_TREE = PREFIX + "tree:entity";
  private static final String KEY_DTO_TREE_PREFIX = PREFIX + "tree:dto:";

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  @Value("${product.cache.category.ttl-seconds:1800}")
  private long ttlSeconds;

  public List<Category> getEntityTree() {
    return get(KEY_ENTITY_TREE, new TypeReference<List<Category>>() {});
  }

  public void putEntityTree(List<Category> categories) {
    put(KEY_ENTITY_TREE, categories);
  }

  public List<CategoryDTO> getDtoTree(Boolean onlyEnabled) {
    return get(dtoTreeKey(onlyEnabled), new TypeReference<List<CategoryDTO>>() {});
  }

  public void putDtoTree(Boolean onlyEnabled, List<CategoryDTO> categories) {
    put(dtoTreeKey(onlyEnabled), categories);
  }

  public void clearAll() {
    try {
      Set<String> keys = stringRedisTemplate.keys(PREFIX + "*");
      if (keys != null && !keys.isEmpty()) {
        stringRedisTemplate.delete(keys);
      }
    } catch (Exception ex) {
      log.warn("Clear category cache failed", ex);
    }
  }

  private <T> T get(String key, TypeReference<T> typeReference) {
    try {
      String json = stringRedisTemplate.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return null;
      }
      stringRedisTemplate.expire(key, ttl());
      return objectMapper.readValue(json, typeReference);
    } catch (Exception ex) {
      log.warn("Read category cache failed: key={}", key, ex);
      return null;
    }
  }

  private void put(String key, Object value) {
    if (value == null) {
      return;
    }
    try {
      stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl());
    } catch (Exception ex) {
      log.warn("Write category cache failed: key={}", key, ex);
    }
  }

  private String dtoTreeKey(Boolean onlyEnabled) {
    return KEY_DTO_TREE_PREFIX + (Boolean.TRUE.equals(onlyEnabled) ? "enabled" : "all");
  }

  private Duration ttl() {
    return Duration.ofSeconds(Math.max(60L, ttlSeconds));
  }
}
