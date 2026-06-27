package com.cloud.search.service.impl;

import com.cloud.search.document.CategoryDocument;
import com.cloud.search.repository.CategoryDocumentRepository;
import com.cloud.search.service.CategorySearchService;
import com.cloud.search.service.support.SearchProcessedEventSupport;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySearchServiceImpl implements CategorySearchService {

  private static final String PROCESSED_EVENT_BUCKET_PREFIX = "search:category:processed:bucket:";

  private final CategoryDocumentRepository categoryDocumentRepository;
  private final ElasticsearchOperations elasticsearchOperations;
  private final StringRedisTemplate redisTemplate;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteCategory(Long categoryId) {
    try {
      categoryDocumentRepository.deleteById(String.valueOf(categoryId));
    } catch (Exception e) {
      throw new RuntimeException("Delete category from index failed", e);
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateCategoryStatus(Long categoryId, Integer status) {
    try {
      CategoryDocument document = findByCategoryId(categoryId);
      if (document == null) {
        return;
      }
      document.setStatus(status);
      categoryDocumentRepository.save(document);
    } catch (Exception e) {
      throw new RuntimeException("Update category status in index failed", e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryDocument findByCategoryId(Long categoryId) {
    return categoryDocumentRepository.findById(String.valueOf(categoryId)).orElse(null);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void batchDeleteCategories(List<Long> categoryIds) {
    try {
      List<String> ids =
          categoryIds == null
              ? Collections.emptyList()
              : categoryIds.stream().map(String::valueOf).toList();
      if (!ids.isEmpty()) {
        categoryDocumentRepository.deleteAllById(ids);
      }
    } catch (Exception e) {
      throw new RuntimeException("Batch delete categories from index failed", e);
    }
  }

  @Override
  public boolean isEventProcessed(String traceId) {
    return SearchProcessedEventSupport.isProcessed(
        redisTemplate, PROCESSED_EVENT_BUCKET_PREFIX, traceId, log);
  }

  @Override
  public void markEventProcessed(String traceId) {
    SearchProcessedEventSupport.markProcessed(
        redisTemplate, PROCESSED_EVENT_BUCKET_PREFIX, traceId, log);
  }

  @Override
  public void rebuildCategoryIndex() {
    if (indexExists()) {
      deleteCategoryIndex();
    }
    createCategoryIndex();
  }

  @Override
  public boolean indexExists() {
    try {
      return elasticsearchOperations.indexOps(CategoryDocument.class).exists();
    } catch (Exception e) {
      log.error("Check category index existence failed", e);
      return false;
    }
  }

  @Override
  public void createCategoryIndex() {
    try {
      elasticsearchOperations.indexOps(CategoryDocument.class).create();
      elasticsearchOperations.indexOps(CategoryDocument.class).putMapping();
    } catch (Exception e) {
      throw new RuntimeException("Create category index failed", e);
    }
  }

  @Override
  public void deleteCategoryIndex() {
    try {
      elasticsearchOperations.indexOps(CategoryDocument.class).delete();
    } catch (Exception e) {
      throw new RuntimeException("Delete category index failed", e);
    }
  }
}
