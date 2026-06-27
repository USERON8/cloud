package com.cloud.product.service.cache;

import com.cloud.common.cache.AbstractJsonRedisCacheSupport;
import com.cloud.common.domain.dto.product.CategoryDTO;
import com.cloud.common.util.TransactionCommitSupport;
import com.cloud.product.module.entity.Category;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CategoryRedisCacheService extends AbstractJsonRedisCacheSupport {

  private static final String PREFIX = "product:category:";
  private static final String KEY_ENTITY_TREE = PREFIX + "tree:entity";
  private static final String KEY_DTO_TREE_PREFIX = PREFIX + "tree:dto:";

  private final TaskScheduler taskScheduler;

  public CategoryRedisCacheService(
      StringRedisTemplate stringRedisTemplate,
      ObjectMapper objectMapper,
      TaskScheduler taskScheduler) {
    super(stringRedisTemplate, objectMapper);
    this.taskScheduler = taskScheduler;
  }

  @Value("${product.cache.category.ttl-seconds:1800}")
  private long ttlSeconds;

  @Value("${product.cache.category.delayed-double-delete-ms:500}")
  private long delayedDoubleDeleteMs;

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
    clearAllNow();
  }

  public void clearAllAfterCommit() {
    TransactionCommitSupport.runAfterCommitRepeated(
        this::clearAllNow, taskScheduler, delayedDoubleDeleteMs);
  }

  private void clearAllNow() {
    clearAllByPrefix(PREFIX, log, "category");
  }

  private <T> T get(String key, TypeReference<T> typeReference) {
    return getValue(key, typeReference, ttl(ttlSeconds), log, "category");
  }

  private void put(String key, Object value) {
    putValue(key, value, ttl(ttlSeconds), log, "category");
  }

  private String dtoTreeKey(Boolean onlyEnabled) {
    return KEY_DTO_TREE_PREFIX + (Boolean.TRUE.equals(onlyEnabled) ? "enabled" : "all");
  }
}
