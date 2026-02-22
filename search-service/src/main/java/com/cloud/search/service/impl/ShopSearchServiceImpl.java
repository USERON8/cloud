package com.cloud.search.service.impl;


import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.dto.ShopSearchRequest;
import com.cloud.search.repository.ShopDocumentRepository;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.cloud.search.service.ShopSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;








@Slf4j
@Service
@RequiredArgsConstructor
public class ShopSearchServiceImpl implements ShopSearchService {

    private static final String PROCESSED_EVENT_KEY_PREFIX = "search:shop:processed:";
    private static final long PROCESSED_EVENT_TTL = 24 * 60 * 60; 

    private final ShopDocumentRepository shopDocumentRepository;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;
    private final StringRedisTemplate redisTemplate;


    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "shopSearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "shopSuggestionCache", allEntries = true),
                    @CacheEvict(cacheNames = "hotShopCache", allEntries = true),
                    @CacheEvict(cacheNames = "shopFilterCache", allEntries = true)
            }
    )
    public void deleteShop(Long shopId) {
        try {
            

            shopDocumentRepository.deleteById(String.valueOf(shopId));

            

        } catch (Exception e) {
            log.error("鉂?浠嶦S鍒犻櫎搴楅摵澶辫触 - 搴楅摵ID: {}, 閿欒: {}",
                    shopId, e.getMessage(), e);
            throw new RuntimeException("浠嶦S鍒犻櫎搴楅摵澶辫触", e);
        }
    }

    @Override
    public void updateShopStatus(Long shopId, Integer status) {
        try {
            

            Optional<ShopDocument> optionalDoc = shopDocumentRepository.findById(String.valueOf(shopId));
            if (optionalDoc.isPresent()) {
                ShopDocument document = optionalDoc.get();
                document.setStatus(status);
                document.setUpdatedAt(LocalDateTime.now());
                shopDocumentRepository.save(document);

                
            } else {
                log.warn("鈿狅笍 搴楅摵涓嶅瓨鍦紝鏃犳硶鏇存柊鐘舵€?- 搴楅摵ID: {}", shopId);
            }

        } catch (Exception e) {
            log.error("鉂?鏇存柊搴楅摵鐘舵€佸け璐?- 搴楅摵ID: {}, 閿欒: {}",
                    shopId, e.getMessage(), e);
            throw new RuntimeException("鏇存柊搴楅摵鐘舵€佸け璐?, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopSearchCache",
            key = "'shop:' + #shopId",
            condition = "#shopId != null")
    public ShopDocument findByShopId(Long shopId) {
        return shopDocumentRepository.findById(String.valueOf(shopId)).orElse(null);
    }


    @Override
    public void batchDeleteShops(List<Long> shopIds) {
        try {
            

            List<String> ids = shopIds.stream()
                    .map(String::valueOf)
                    .toList();

            shopDocumentRepository.deleteAllById(ids);

            

        } catch (Exception e) {
            log.error("鉂?鎵归噺鍒犻櫎搴楅摵浠嶦S澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鎵归噺鍒犻櫎搴楅摵浠嶦S澶辫触", e);
        }
    }

    @Override
    public boolean isEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("妫€鏌ュ簵閾轰簨浠跺鐞嗙姸鎬佸け璐?- TraceId: {}, 閿欒: {}", traceId, e.getMessage());
            return false;
        }
    }

    @Override
    public void markEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            redisTemplate.opsForValue().set(key, "1", PROCESSED_EVENT_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("鏍囪搴楅摵浜嬩欢宸插鐞嗗け璐?- TraceId: {}, 閿欒: {}", traceId, e.getMessage());
        }
    }

    @Override
    public void rebuildShopIndex() {
        try {

            
            if (indexExists()) {
                deleteShopIndex();
            }

            
            createShopIndex();

            

        } catch (Exception e) {
            log.error("鉂?閲嶅缓搴楅摵绱㈠紩澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("閲嶅缓搴楅摵绱㈠紩澶辫触", e);
        }
    }

    @Override
    public boolean indexExists() {
        try {
            
            return true;
        } catch (Exception e) {
            log.error("妫€鏌ュ簵閾虹储寮曟槸鍚﹀瓨鍦ㄥけ璐?- 閿欒: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void createShopIndex() {
        try {
            
            
            
        } catch (Exception e) {
            log.error("鉂?鍒涘缓搴楅摵绱㈠紩澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鍒涘缓搴楅摵绱㈠紩澶辫触", e);
        }
    }

    


    public void deleteShopIndex() {
        try {
            
            
            shopDocumentRepository.deleteAll();
            
        } catch (Exception e) {
            log.error("鉂?鍒犻櫎搴楅摵绱㈠紩澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("鍒犻櫎搴楅摵绱㈠紩澶辫触", e);
        }
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopSearchCache",
            key = "'search:' + #request.hashCode()",
            condition = "#request != null")
    public SearchResult<ShopDocument> searchShops(ShopSearchRequest request) {
        try {
            


            long startTime = System.currentTimeMillis();

            
            Pageable pageable = PageRequest.of(
                    request.getPage() != null ? request.getPage() : 0,
                    request.getSize() != null ? request.getSize() : 20,
                    buildShopSort(request)
            );

            
            Page<ShopDocument> page;
            if (StringUtils.hasText(request.getKeyword())) {
                page = shopDocumentRepository.findByShopNameContaining(request.getKeyword(), pageable);
            } else {
                page = shopDocumentRepository.findAll(pageable);
            }

            long took = System.currentTimeMillis() - startTime;

            SearchResult<ShopDocument> result = SearchResult.of(
                    page.getContent(),
                    page.getTotalElements(),
                    page.getNumber(),
                    page.getSize(),
                    took,
                    null, 
                    null  
            );

            
            return result;

        } catch (Exception e) {
            log.error("鉂?搴楅摵鎼滅储澶辫触 - 閿欒: {}", e.getMessage(), e);
            throw new RuntimeException("搴楅摵鎼滅储澶辫触", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopSuggestionCache",
            key = "'suggestion:' + #keyword + ':' + #size",
            condition = "#keyword != null")
    public List<String> getSearchSuggestions(String keyword, Integer size) {
        try {
            if (!StringUtils.hasText(keyword)) {
                return Collections.emptyList();
            }

            

            
            Pageable pageable = PageRequest.of(0, size != null ? size : 10);
            Page<ShopDocument> page = shopDocumentRepository.findByShopNameContaining(keyword, pageable);

            List<String> suggestions = page.getContent().stream()
                    .map(ShopDocument::getShopName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(size != null ? size : 10)
                    .collect(Collectors.toList());

            
            return suggestions;

        } catch (Exception e) {
            log.error("鉂?鑾峰彇搴楅摵鎼滅储寤鸿澶辫触 - 閿欒: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hotShopCache",
            key = "'hot:' + #size")
    public List<ShopDocument> getHotShops(Integer size) {
        try {
            

            
            Pageable pageable = PageRequest.of(0, size != null ? size : 10,
                    Sort.by(Sort.Direction.DESC, "hotScore"));
            Page<ShopDocument> page = shopDocumentRepository.findByStatus(1, pageable);

            List<ShopDocument> hotShops = page.getContent();

            
            return hotShops;

        } catch (Exception e) {
            log.error("鉂?鑾峰彇鐑棬搴楅摵澶辫触 - 閿欒: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "shopFilterCache",
            key = "'filter:' + #request.hashCode()",
            condition = "#request != null")
    public SearchResult<ShopDocument> getShopFilters(ShopSearchRequest request) {
        try {
            

            return result;

        } catch (Exception e) {
            log.error("鉂?鑾峰彇搴楅摵绛涢€夎仛鍚堜俊鎭け璐?- 閿欒: {}", e.getMessage(), e);
            return SearchResult.<ShopDocument>builder()
                    .list(Collections.emptyList())
                    .total(0L)
                    .page(0)
                    .size(0)
                    .totalPages(0)
                    .hasNext(false)
                    .hasPrevious(false)
                    .took(0L)
                    .aggregations(Collections.emptyMap())
                    .build();
        }
    }

    


    private Sort buildShopSort(ShopSearchRequest request) {
        if (request.getSortBy() == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortOrder())
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return Sort.by(direction, request.getSortBy());
    }

}
