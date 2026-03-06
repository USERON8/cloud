package com.cloud.search.service.impl;

import com.cloud.search.document.ShopDocument;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.dto.ShopSearchRequest;
import com.cloud.search.repository.ShopDocumentRepository;
import com.cloud.search.service.ShopSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopSearchServiceImpl implements ShopSearchService {

    private static final String PROCESSED_EVENT_KEY_PREFIX = "search:shop:processed:";
    private static final long PROCESSED_EVENT_TTL_SECONDS = 24 * 60 * 60;

    private final ShopDocumentRepository shopDocumentRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShop(Long shopId) {
        try {
            shopDocumentRepository.deleteById(String.valueOf(shopId));
        } catch (Exception e) {
            throw new RuntimeException("Delete shop from index failed", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShopStatus(Long shopId, Integer status) {
        try {
            ShopDocument document = findByShopId(shopId);
            if (document == null) {
                return;
            }
            document.setStatus(status);
            shopDocumentRepository.save(document);
        } catch (Exception e) {
            throw new RuntimeException("Update shop status in index failed", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShopDocument findByShopId(Long shopId) {
        return shopDocumentRepository.findById(String.valueOf(shopId)).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteShops(List<Long> shopIds) {
        try {
            List<String> ids = shopIds == null ? Collections.emptyList() : shopIds.stream().map(String::valueOf).toList();
            if (!ids.isEmpty()) {
                shopDocumentRepository.deleteAllById(ids);
            }
        } catch (Exception e) {
            throw new RuntimeException("Batch delete shops from index failed", e);
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
            log.warn("Check shop processed event failed: traceId={}", traceId, e);
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
            log.warn("Mark shop event processed failed: traceId={}", traceId, e);
        }
    }

    @Override
    public void rebuildShopIndex() {
        if (indexExists()) {
            deleteShopIndex();
        }
        createShopIndex();
    }

    @Override
    public boolean indexExists() {
        try {
            return elasticsearchOperations.indexOps(ShopDocument.class).exists();
        } catch (Exception e) {
            log.error("Check shop index existence failed", e);
            return false;
        }
    }

    @Override
    public void createShopIndex() {
        try {
            elasticsearchOperations.indexOps(ShopDocument.class).create();
            elasticsearchOperations.indexOps(ShopDocument.class).putMapping();
        } catch (Exception e) {
            throw new RuntimeException("Create shop index failed", e);
        }
    }

    @Override
    public void deleteShopIndex() {
        try {
            elasticsearchOperations.indexOps(ShopDocument.class).delete();
        } catch (Exception e) {
            throw new RuntimeException("Delete shop index failed", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ShopDocument> searchShops(ShopSearchRequest request) {
        ShopSearchRequest safeRequest = request == null ? new ShopSearchRequest() : request;
        long start = System.currentTimeMillis();

        Pageable pageable = PageRequest.of(
                normalizePage(safeRequest.getPage()),
                normalizeSize(safeRequest.getSize()),
                buildSort(safeRequest.getSortBy(), safeRequest.getSortOrder())
        );

        Page<ShopDocument> page = selectPage(safeRequest, pageable);
        long took = System.currentTimeMillis() - start;

        return SearchResult.of(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), took);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String keyword, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        int limit = size == null || size <= 0 ? 10 : Math.min(size, 50);
        try {
            Page<ShopDocument> page = shopDocumentRepository.findByShopNameContaining(keyword, PageRequest.of(0, limit));
            return page.getContent().stream()
                    .map(ShopDocument::getShopName)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Get shop suggestions failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDocument> getHotShops(Integer size) {
        int limit = size == null || size <= 0 ? 10 : Math.min(size, 100);
        try {
            Page<ShopDocument> page = shopDocumentRepository.findByStatus(
                    1,
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "hotScore"))
            );
            return page.getContent();
        } catch (Exception e) {
            log.error("Get hot shops failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult<ShopDocument> getShopFilters(ShopSearchRequest request) {
        SearchResult<ShopDocument> base = searchShops(request);
        List<ShopDocument> list = base.getList() == null ? Collections.emptyList() : base.getList();

        Map<String, Object> aggregations = new LinkedHashMap<>();
        aggregations.put("statusCount", list.stream().collect(Collectors.groupingBy(ShopDocument::getStatus, Collectors.counting())));
        aggregations.put("recommendCount", list.stream().collect(Collectors.groupingBy(ShopDocument::getRecommended, Collectors.counting())));
        base.setAggregations(aggregations);
        return base;
    }

    private Page<ShopDocument> selectPage(ShopSearchRequest request, Pageable pageable) {
        Integer status = request.getStatus();
        if (StringUtils.hasText(request.getKeyword()) && status != null) {
            return shopDocumentRepository.searchByKeywordAndStatus(request.getKeyword(), status, pageable);
        }
        if (StringUtils.hasText(request.getKeyword())) {
            return shopDocumentRepository.findByShopNameContaining(request.getKeyword(), pageable);
        }
        if (request.getMerchantId() != null && status != null) {
            return shopDocumentRepository.findByMerchantIdAndStatus(request.getMerchantId(), status, pageable);
        }
        if (request.getMerchantId() != null) {
            return shopDocumentRepository.findByMerchantId(request.getMerchantId(), pageable);
        }
        if (request.getRecommended() != null) {
            return shopDocumentRepository.findByRecommended(request.getRecommended(), pageable);
        }
        if (StringUtils.hasText(request.getAddressKeyword())) {
            return shopDocumentRepository.findByAddressContaining(request.getAddressKeyword(), pageable);
        }
        if (request.getMinRating() != null) {
            return shopDocumentRepository.advancedSearch(
                    StringUtils.hasText(request.getKeyword()) ? request.getKeyword() : "",
                    request.getMinRating(),
                    status != null ? status : 1,
                    pageable
            );
        }
        if (status != null) {
            return shopDocumentRepository.findByStatus(status, pageable);
        }
        return shopDocumentRepository.findAll(pageable);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private Sort buildSort(String sortBy, String sortOrder) {
        String field = StringUtils.hasText(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}
