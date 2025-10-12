package com.cloud.search.service.impl;

import com.cloud.search.document.CategoryDocument;
import com.cloud.search.repository.CategoryDocumentRepository;
import com.cloud.search.service.CategorySearchService;
import com.cloud.search.service.ElasticsearchOptimizedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 分类搜索服务实现
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySearchServiceImpl implements CategorySearchService {

    private static final String PROCESSED_EVENT_KEY_PREFIX = "search:category:processed:";
    private static final long PROCESSED_EVENT_TTL = 24 * 60 * 60; // 24小时

    private final CategoryDocumentRepository categoryDocumentRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;
    private final StringRedisTemplate redisTemplate;


    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "categorySearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "categorySuggestionCache", allEntries = true)
            }
    )
    public void deleteCategory(Long categoryId) {
        try {
            log.info("从ES删除分类 - 分类ID: {}", categoryId);

            categoryDocumentRepository.deleteById(String.valueOf(categoryId));

            log.info("✅ 分类从ES删除成功 - 分类ID: {}", categoryId);

        } catch (Exception e) {
            log.error("❌ 从ES删除分类失败 - 分类ID: {}, 错误: {}",
                    categoryId, e.getMessage(), e);
            throw new RuntimeException("从ES删除分类失败", e);
        }
    }

    @Override
    public void updateCategoryStatus(Long categoryId, Integer status) {
        try {
            log.info("更新分类状态 - 分类ID: {}, 状态: {}", categoryId, status);

            Optional<CategoryDocument> optionalDoc = categoryDocumentRepository.findById(String.valueOf(categoryId));
            if (optionalDoc.isPresent()) {
                CategoryDocument document = optionalDoc.get();
                document.setStatus(status);
                document.setUpdatedAt(LocalDateTime.now());
                categoryDocumentRepository.save(document);

                log.info("✅ 分类状态更新成功 - 分类ID: {}, 状态: {}", categoryId, status);
            } else {
                log.warn("⚠️ 分类不存在，无法更新状态 - 分类ID: {}", categoryId);
            }

        } catch (Exception e) {
            log.error("❌ 更新分类状态失败 - 分类ID: {}, 错误: {}",
                    categoryId, e.getMessage(), e);
            throw new RuntimeException("更新分类状态失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categorySearchCache",
            key = "'category:' + #categoryId",
            condition = "#categoryId != null")
    public CategoryDocument findByCategoryId(Long categoryId) {
        return categoryDocumentRepository.findById(String.valueOf(categoryId)).orElse(null);
    }


    @Override
    public void batchDeleteCategories(List<Long> categoryIds) {
        try {
            log.info("批量删除分类从ES - 数量: {}", categoryIds.size());

            List<String> ids = categoryIds.stream()
                    .map(String::valueOf)
                    .toList();

            categoryDocumentRepository.deleteAllById(ids);

            log.info("✅ 批量删除分类从ES成功 - 数量: {}", categoryIds.size());

        } catch (Exception e) {
            log.error("❌ 批量删除分类从ES失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("批量删除分类从ES失败", e);
        }
    }

    @Override
    public boolean isEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("检查分类事件处理状态失败 - TraceId: {}, 错误: {}", traceId, e.getMessage());
            return false;
        }
    }

    @Override
    public void markEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            redisTemplate.opsForValue().set(key, "1", PROCESSED_EVENT_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("标记分类事件已处理失败 - TraceId: {}, 错误: {}", traceId, e.getMessage());
        }
    }

    @Override
    public void rebuildCategoryIndex() {
        try {
            log.info("开始重建分类索引");

            // 删除现有索引
            if (indexExists()) {
                deleteCategoryIndex();
            }

            // 创建新索引
            createCategoryIndex();

            log.info("✅ 分类索引重建完成");

        } catch (Exception e) {
            log.error("❌ 重建分类索引失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("重建分类索引失败", e);
        }
    }

    @Override
    public boolean indexExists() {
        try {
            return elasticsearchOperations.indexOps(CategoryDocument.class).exists();
        } catch (Exception e) {
            log.error("检查分类索引是否存在失败 - 错误: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void createCategoryIndex() {
        try {
            log.info("创建分类索引");
            elasticsearchOperations.indexOps(CategoryDocument.class).create();
            elasticsearchOperations.indexOps(CategoryDocument.class).putMapping();
            log.info("✅ 分类索引创建成功");
        } catch (Exception e) {
            log.error("❌ 创建分类索引失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("创建分类索引失败", e);
        }
    }

    @Override
    public void deleteCategoryIndex() {
        try {
            log.info("删除分类索引");
            elasticsearchOperations.indexOps(CategoryDocument.class).delete();
            log.info("✅ 分类索引删除成功");
        } catch (Exception e) {
            log.error("❌ 删除分类索引失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("删除分类索引失败", e);
        }
    }


}
