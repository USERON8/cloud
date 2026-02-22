package com.cloud.search.service.impl;

import com.cloud.search.document.CategoryDocument;
import com.cloud.search.repository.CategoryDocumentRepository;
import com.cloud.search.service.CategorySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySearchServiceImpl implements CategorySearchService {

    private static final String PROCESSED_EVENT_KEY_PREFIX = "search:category:processed:";
    private static final long PROCESSED_EVENT_TTL_SECONDS = 24 * 60 * 60;

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
            List<String> ids = categoryIds == null ? Collections.emptyList() : categoryIds.stream().map(String::valueOf).toList();
            if (!ids.isEmpty()) {
                categoryDocumentRepository.deleteAllById(ids);
            }
        } catch (Exception e) {
            throw new RuntimeException("Batch delete categories from index failed", e);
        }
    }

    @Override
    public boolean isEventProcessed(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PROCESSED_EVENT_KEY_PREFIX + traceId));
        } catch (Exception e) {
            log.warn("Check category processed event failed: traceId={}", traceId, e);
            return false;
        }
    }

    @Override
    public void markEventProcessed(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    PROCESSED_EVENT_KEY_PREFIX + traceId,
                    "1",
                    PROCESSED_EVENT_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("Mark category event processed failed: traceId={}", traceId, e);
        }
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
