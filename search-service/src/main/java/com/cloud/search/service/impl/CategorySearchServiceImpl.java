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








@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySearchServiceImpl implements CategorySearchService {

    private static final String PROCESSED_EVENT_KEY_PREFIX = "search:category:processed:";
    private static final long PROCESSED_EVENT_TTL = 24 * 60 * 60; 

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
            

            categoryDocumentRepository.deleteById(String.valueOf(categoryId));

            

        } catch (Exception e) {
            log.error("鉂?浠嶦S鍒犻櫎鍒嗙被澶辫触 - 鍒嗙被ID: {}, 閿欒: {}",
                    categoryId, e.getMessage(), e);
            throw new RuntimeException("浠嶦S鍒犻櫎鍒嗙被澶辫触", e);
        }
    }

    @Override
    public void updateCategoryStatus(Long categoryId, Integer status) {
        try {
            

            Optional<CategoryDocument> optionalDoc = categoryDocumentRepository.findById(String.valueOf(categoryId));
            if (optionalDoc.isPresent()) {
                CategoryDocument document = optionalDoc.get();
                document.setStatus(status);
                document.setUpdatedAt(LocalDateTime.now());
                categoryDocumentRepository.save(document);

                
            } else {
                log.warn("鈿狅笍 鍒嗙被涓嶅瓨鍦紝鏃犳硶鏇存柊鐘舵€?- 鍒嗙被ID: {}", categoryId);
            }

        } catch (Exception e) {
            log.error("鉂?鏇存柊鍒嗙被鐘舵€佸け璐?- 鍒嗙被ID: {}, 閿欒: {}",
                    categoryId, e.getMessage(), e);
            throw new RuntimeException("鏇存柊鍒嗙被鐘舵€佸け璐?, e);
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
            

            List<String> ids = categoryIds.stream()
                    .map(String::valueOf)
                    .toList();

            categoryDocumentRepository.deleteAllById(ids);

            

        } catch (Exception e) {
            log.error("鉂?鎵归噺鍒犻櫎鍒嗙被浠嶦S澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鎵归噺鍒犻櫎鍒嗙被浠嶦S澶辫触", e);
        }
    }

    @Override
    public boolean isEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("妫€鏌ュ垎绫讳簨浠跺鐞嗙姸鎬佸け璐?- TraceId: {}, 閿欒: {}", traceId, e.getMessage());
            return false;
        }
    }

    @Override
    public void markEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            redisTemplate.opsForValue().set(key, "1", PROCESSED_EVENT_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("鏍囪鍒嗙被浜嬩欢宸插鐞嗗け璐?- TraceId: {}, 閿欒: {}", traceId, e.getMessage());
        }
    }

    @Override
    public void rebuildCategoryIndex() {
        try {

            
            if (indexExists()) {
                deleteCategoryIndex();
            }

            
            createCategoryIndex();

            

        } catch (Exception e) {
            log.error("鉂?閲嶅缓鍒嗙被绱㈠紩澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("閲嶅缓鍒嗙被绱㈠紩澶辫触", e);
        }
    }

    @Override
    public boolean indexExists() {
        try {
            return elasticsearchOperations.indexOps(CategoryDocument.class).exists();
        } catch (Exception e) {
            log.error("妫€鏌ュ垎绫荤储寮曟槸鍚﹀瓨鍦ㄥけ璐?- 閿欒: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void createCategoryIndex() {
        try {
            
            elasticsearchOperations.indexOps(CategoryDocument.class).create();
            elasticsearchOperations.indexOps(CategoryDocument.class).putMapping();
            
        } catch (Exception e) {
            log.error("鉂?鍒涘缓鍒嗙被绱㈠紩澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鍒涘缓鍒嗙被绱㈠紩澶辫触", e);
        }
    }

    @Override
    public void deleteCategoryIndex() {
        try {
            
            elasticsearchOperations.indexOps(CategoryDocument.class).delete();
            
        } catch (Exception e) {
            log.error("鉂?鍒犻櫎鍒嗙被绱㈠紩澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鍒犻櫎鍒嗙被绱㈠紩澶辫触", e);
        }
    }


}
