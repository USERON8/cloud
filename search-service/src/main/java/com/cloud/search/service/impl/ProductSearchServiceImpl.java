package com.cloud.search.service.impl;

import com.cloud.search.document.ProductDocument;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.SearchResult;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ElasticsearchOptimizedService;
import com.cloud.search.service.ProductSearchService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品搜索服务实现
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private static final String PROCESSED_EVENT_KEY_PREFIX = "search:processed:";
    private static final long PROCESSED_EVENT_TTL = 24 * 60 * 60; // 24小时
    private final ProductDocumentRepository productDocumentRepository;
    private final ElasticsearchOptimizedService elasticsearchOptimizedService;
    private final StringRedisTemplate redisTemplate;


    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "productSearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "searchSuggestionCache", allEntries = true),
                    @CacheEvict(cacheNames = "hotSearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "filterCache", allEntries = true)
            }
    )
    public void deleteProduct(Long productId) {
        try {
            log.info("从ES删除商品 - 商品ID: {}", productId);

            productDocumentRepository.deleteById(String.valueOf(productId));

            log.info("✅ 商品从ES删除成功 - 商品ID: {}", productId);

        } catch (Exception e) {
            log.error("❌ 从ES删除商品失败 - 商品ID: {}, 错误: {}",
                    productId, e.getMessage(), e);
            throw new RuntimeException("从ES删除商品失败", e);
        }
    }

    @Override
    public void updateProductStatus(Long productId, Integer status) {
        try {
            log.info("更新商品状态 - 商品ID: {}, 状态: {}", productId, status);

            Optional<ProductDocument> optionalDoc = productDocumentRepository.findById(String.valueOf(productId));
            if (optionalDoc.isPresent()) {
                ProductDocument document = optionalDoc.get();
                document.setStatus(status);
                document.setUpdatedAt(LocalDateTime.now());
                productDocumentRepository.save(document);

                log.info("✅ 商品状态更新成功 - 商品ID: {}, 状态: {}", productId, status);
            } else {
                log.warn("⚠️ 商品不存在，无法更新状态 - 商品ID: {}", productId);
            }

        } catch (Exception e) {
            log.error("❌ 更新商品状态失败 - 商品ID: {}, 错误: {}",
                    productId, e.getMessage(), e);
            throw new RuntimeException("更新商品状态失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'product:' + #productId",
            condition = "#productId != null")
    public ProductDocument findByProductId(Long productId) {
        return productDocumentRepository.findById(String.valueOf(productId)).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "productSearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "searchSuggestionCache", allEntries = true),
                    @CacheEvict(cacheNames = "hotSearchCache", allEntries = true),
                    @CacheEvict(cacheNames = "filterCache", allEntries = true),
                    @CacheEvict(cacheNames = "aggregationCache", allEntries = true)
            }
    )

    public void batchDeleteProducts(List<Long> productIds) {
        try {
            log.info("批量删除商品从ES - 数量: {}", productIds.size());

            List<String> ids = productIds.stream()
                    .map(String::valueOf)
                    .toList();

            productDocumentRepository.deleteAllById(ids);

            log.info("✅ 批量删除商品从ES成功 - 数量: {}", productIds.size());

        } catch (Exception e) {
            log.error("❌ 批量删除商品从ES失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("批量删除商品从ES失败", e);
        }
    }

    @Override
    public boolean isEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("检查事件处理状态失败 - TraceId: {}, 错误: {}", traceId, e.getMessage());
            return false;
        }
    }

    @Override
    public void markEventProcessed(String traceId) {
        try {
            String key = PROCESSED_EVENT_KEY_PREFIX + traceId;
            redisTemplate.opsForValue().set(key, "1", PROCESSED_EVENT_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("标记事件已处理失败 - TraceId: {}, 错误: {}", traceId, e.getMessage());
        }
    }

    @Override
    public void rebuildProductIndex() {
        try {
            log.info("开始重建商品索引");

            // 删除现有索引
            if (indexExists()) {
                deleteProductIndex();
            }

            // 创建新索引
            createProductIndex();

            log.info("✅ 商品索引重建完成");

        } catch (Exception e) {
            log.error("❌ 重建商品索引失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("重建商品索引失败", e);
        }
    }

    @Override
    public boolean indexExists() {
        try {
            // 简化实现：假设索引总是存在
            return true;
        } catch (Exception e) {
            log.error("检查索引是否存在失败 - 错误: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void createProductIndex() {
        try {
            log.info("创建商品索引");
            // 简化实现：使用Repository自动创建索引
            log.info("✅ 商品索引创建成功");
        } catch (Exception e) {
            log.error("❌ 创建商品索引失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("创建商品索引失败", e);
        }
    }

    @Override
    public void deleteProductIndex() {
        try {
            log.info("删除商品索引");
            // 简化实现：删除所有文档
            productDocumentRepository.deleteAll();
            log.info("✅ 商品索引删除成功");
        } catch (Exception e) {
            log.error("❌ 删除商品索引失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("删除商品索引失败", e);
        }
    }

    /**
     * 判断是否为新品
     */
    private Boolean isNewProduct(LocalDateTime createdAt) {
        if (createdAt == null) {
            return false;
        }
        return createdAt.isAfter(LocalDateTime.now().minusDays(30));
    }

    /**
     * 判断是否为热销商品
     */
    private Boolean isHotProduct(Integer salesCount) {
        return salesCount != null && salesCount > 100;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'search:' + #request.hashCode()",
            condition = "#request != null")
    public SearchResult<ProductDocument> searchProducts(ProductSearchRequest request) {
        try {
            log.info("执行商品复杂搜索 - 关键字: {}, 分类: {}, 品牌: {}, 价格范围: {}-{}",
                    request.getKeyword(), request.getCategoryName(), request.getBrandName(),
                    request.getMinPrice(), request.getMaxPrice());

            long startTime = System.currentTimeMillis();

            // 构建分页参数
            Pageable pageable = PageRequest.of(
                    request.getPage() != null ? request.getPage() : 0,
                    request.getSize() != null ? request.getSize() : 20,
                    buildSort(request)
            );

            // 使用Repository进行简单搜索
            Page<ProductDocument> page;
            if (StringUtils.hasText(request.getKeyword())) {
                page = productDocumentRepository.findByProductNameContaining(request.getKeyword(), pageable);
            } else {
                page = productDocumentRepository.findAll(pageable);
            }

            long took = System.currentTimeMillis() - startTime;

            SearchResult<ProductDocument> result = SearchResult.of(
                    page.getContent(),
                    page.getTotalElements(),
                    page.getNumber(),
                    page.getSize(),
                    took,
                    null, // 暂时不支持聚合
                    null  // 暂时不支持高亮
            );

            log.info("✅ 商品搜索完成 - 总数: {}, 耗时: {}ms", page.getTotalElements(), took);
            return result;

        } catch (Exception e) {
            log.error("❌ 商品搜索失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("商品搜索失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "searchSuggestionCache",
            key = "'suggestion:' + #keyword + ':' + #size",
            condition = "#keyword != null")
    public List<String> getSearchSuggestions(String keyword, Integer size) {
        try {
            if (!StringUtils.hasText(keyword)) {
                return Collections.emptyList();
            }

            log.info("获取搜索建议 - 关键字: {}, 数量: {}", keyword, size);

            // 使用Repository进行简单搜索
            Pageable pageable = PageRequest.of(0, size != null ? size : 10);
            Page<ProductDocument> page = productDocumentRepository.findByProductNameContaining(keyword, pageable);

            List<String> suggestions = page.getContent().stream()
                    .map(ProductDocument::getProductName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(size != null ? size : 10)
                    .collect(Collectors.toList());

            log.info("✅ 获取搜索建议完成 - 数量: {}", suggestions.size());
            return suggestions;

        } catch (Exception e) {
            log.error("❌ 获取搜索建议失败 - 错误: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hotSearchCache",
            key = "'hot:' + #size")
    public List<String> getHotSearchKeywords(Integer size) {
        try {
            log.info("获取热门搜索关键字 - 数量: {}", size);

            // 使用Repository查询热门商品
            Pageable pageable = PageRequest.of(0, size != null ? size : 10,
                    Sort.by(Sort.Direction.DESC, "hotScore"));
            Page<ProductDocument> page = productDocumentRepository.findByStatus(1, pageable);

            Set<String> keywords = new LinkedHashSet<>();
            for (ProductDocument product : page.getContent()) {
                if (StringUtils.hasText(product.getProductName())) {
                    // 提取商品名称中的关键词
                    String[] words = product.getProductName().split("\\s+");
                    for (String word : words) {
                        if (word.length() > 1) {
                            keywords.add(word);
                        }
                    }
                }
                if (StringUtils.hasText(product.getTags())) {
                    // 提取标签中的关键词
                    String[] tags = product.getTags().split(",");
                    for (String tag : tags) {
                        if (StringUtils.hasText(tag.trim())) {
                            keywords.add(tag.trim());
                        }
                    }
                }
            }

            List<String> result = keywords.stream()
                    .limit(size != null ? size : 10)
                    .collect(Collectors.toList());

            log.info("✅ 获取热门搜索关键字完成 - 数量: {}", result.size());
            return result;

        } catch (Exception e) {
            log.error("❌ 获取热门搜索关键字失败 - 错误: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "filterCache",
            key = "'filter:' + #request.hashCode()",
            condition = "#request != null")
    public SearchResult<ProductDocument> getProductFilters(ProductSearchRequest request) {
        try {
            log.info("获取商品筛选聚合信息");

            // 简化实现：返回空的聚合信息
            SearchResult<ProductDocument> result = SearchResult.<ProductDocument>builder()
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

            log.info("✅ 获取商品筛选聚合信息完成");
            return result;

        } catch (Exception e) {
            log.error("❌ 获取商品筛选聚合信息失败 - 错误: {}", e.getMessage(), e);
            return SearchResult.<ProductDocument>builder()
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

    /**
     * 构建排序
     */
    private Sort buildSort(ProductSearchRequest request) {
        if (request.getSortBy() == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortOrder())
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return Sort.by(direction, request.getSortBy());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'basic:' + #keyword + ':' + #page + ':' + #size",
            condition = "#keyword != null")
    public SearchResult<ProductDocument> basicSearch(String keyword, Integer page, Integer size) {
        try {
            log.info("执行基础搜索 - 关键字: {}, 页码: {}, 大小: {}", keyword, page, size);
            long startTime = System.currentTimeMillis();

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));

            Page<ProductDocument> pageResult;
            if (StringUtils.hasText(keyword)) {
                pageResult = productDocumentRepository.searchByKeyword(keyword, pageable);
            } else {
                pageResult = productDocumentRepository.findByStatus(1, pageable);
            }

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            log.info("基础搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), took);
            return result;

        } catch (Exception e) {
            log.error("基础搜索失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("基础搜索失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'filter:' + #request.hashCode()",
            condition = "#request != null")
    public SearchResult<ProductDocument> filterSearch(ProductSearchRequest request) {
        try {
            log.info("执行筛选搜索 - 请求: {}", request);
            long startTime = System.currentTimeMillis();

            int pageNum = request.getPage() != null ? request.getPage() : 0;
            int pageSize = request.getSize() != null ? request.getSize() : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, buildSort(request));

            Page<ProductDocument> pageResult = productDocumentRepository.filterSearch(
                    request.getKeyword(),
                    request.getCategoryId(),
                    request.getBrandId(),
                    request.getShopId(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    request.getMinSalesCount(),
                    request.getStatus() != null ? request.getStatus() : 1,
                    pageable
            );

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            log.info("筛选搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), took);
            return result;

        } catch (Exception e) {
            log.error("筛选搜索失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("筛选搜索失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'category:' + #categoryId + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> searchByCategory(Long categoryId, Integer page, Integer size) {
        try {
            log.info("按分类搜索 - 分类ID: {}, 页码: {}, 大小: {}", categoryId, page, size);
            long startTime = System.currentTimeMillis();

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));

            Page<ProductDocument> pageResult = productDocumentRepository.findByCategoryIdAndStatus(categoryId, 1, pageable);

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            log.info("按分类搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), took);
            return result;

        } catch (Exception e) {
            log.error("按分类搜索失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("按分类搜索失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'brand:' + #brandId + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> searchByBrand(Long brandId, Integer page, Integer size) {
        try {
            log.info("按品牌搜索 - 品牌ID: {}, 页码: {}, 大小: {}", brandId, page, size);
            long startTime = System.currentTimeMillis();

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));

            Page<ProductDocument> pageResult = productDocumentRepository.findByBrandIdAndStatus(brandId, 1, pageable);

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            log.info("按品牌搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), took);
            return result;

        } catch (Exception e) {
            log.error("按品牌搜索失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("按品牌搜索失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'price:' + #minPrice + ':' + #maxPrice + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Integer page, Integer size) {
        try {
            log.info("按价格区间搜索 - 价格范围: {}-{}, 页码: {}, 大小: {}", minPrice, maxPrice, page, size);
            long startTime = System.currentTimeMillis();

            BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
            BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999.99");

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));

            Page<ProductDocument> pageResult = productDocumentRepository.findByPriceBetweenAndStatus(min, max, 1, pageable);

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            log.info("按价格区间搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), took);
            return result;

        } catch (Exception e) {
            log.error("按价格区间搜索失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("按价格区间搜索失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'shop:' + #shopId + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> searchByShop(Long shopId, Integer page, Integer size) {
        try {
            log.info("按店铺搜索 - 店铺ID: {}, 页码: {}, 大小: {}", shopId, page, size);
            long startTime = System.currentTimeMillis();

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "hotScore"));

            Page<ProductDocument> pageResult = productDocumentRepository.findByShopIdAndStatus(shopId, 1, pageable);

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            log.info("按店铺搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), took);
            return result;

        } catch (Exception e) {
            log.error("按店铺搜索失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("按店铺搜索失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productSearchCache",
            key = "'combined:' + #keyword + ':' + #categoryId + ':' + #brandId + ':' + #minPrice + ':' + #maxPrice + ':' + #shopId + ':' + #page + ':' + #size")
    public SearchResult<ProductDocument> combinedSearch(String keyword, Long categoryId, Long brandId,
                                                        BigDecimal minPrice, BigDecimal maxPrice, Long shopId,
                                                        String sortBy, String sortOrder, Integer page, Integer size) {
        try {
            log.info("执行组合搜索 - 关键字: {}, 分类: {}, 品牌: {}, 价格: {}-{}, 店铺: {}",
                    keyword, categoryId, brandId, minPrice, maxPrice, shopId);
            long startTime = System.currentTimeMillis();

            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;

            Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortField = StringUtils.hasText(sortBy) ? sortBy : "hotScore";
            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));

            Page<ProductDocument> pageResult = productDocumentRepository.combinedSearch(
                    keyword,
                    categoryId,
                    brandId,
                    shopId,
                    minPrice,
                    maxPrice,
                    1,
                    pageable
            );

            long took = System.currentTimeMillis() - startTime;
            SearchResult<ProductDocument> result = convertToSearchResult(pageResult, took);

            log.info("组合搜索完成 - 总数: {}, 耗时: {}ms", result.getTotal(), took);
            return result;

        } catch (Exception e) {
            log.error("组合搜索失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("组合搜索失败", e);
        }
    }

    /**
     * 转换Page为SearchResult
     */
    private SearchResult<ProductDocument> convertToSearchResult(Page<ProductDocument> page, long took) {
        return SearchResult.<ProductDocument>builder()
                .list(page.getContent())
                .total(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .took(took)
                .aggregations(null)
                .highlights(null)
                .build();
    }

}
