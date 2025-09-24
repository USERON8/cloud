package com.cloud.search.service.impl;

import com.cloud.common.domain.event.ProductSearchEvent;
import com.cloud.search.document.ProductDocument;
import com.cloud.search.repository.ProductDocumentRepository;
import com.cloud.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void saveOrUpdateProduct(ProductSearchEvent event) {
        try {
            log.info("保存或更新商品到ES - 商品ID: {}, 商品名称: {}",
                    event.getProductId(), event.getProductName());

            ProductDocument document = convertToDocument(event);
            productDocumentRepository.save(document);

            log.info("✅ 商品保存到ES成功 - 商品ID: {}", event.getProductId());

        } catch (Exception e) {
            log.error("❌ 保存商品到ES失败 - 商品ID: {}, 错误: {}",
                    event.getProductId(), e.getMessage(), e);
            throw new RuntimeException("保存商品到ES失败", e);
        }
    }

    @Override
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
    public ProductDocument findByProductId(Long productId) {
        return productDocumentRepository.findById(String.valueOf(productId)).orElse(null);
    }

    @Override
    public void batchSaveProducts(List<ProductSearchEvent> events) {
        try {
            log.info("批量保存商品到ES - 数量: {}", events.size());

            List<ProductDocument> documents = events.stream()
                    .map(this::convertToDocument)
                    .toList();

            productDocumentRepository.saveAll(documents);

            log.info("✅ 批量保存商品到ES成功 - 数量: {}", events.size());

        } catch (Exception e) {
            log.error("❌ 批量保存商品到ES失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("批量保存商品到ES失败", e);
        }
    }

    @Override
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
            return elasticsearchOperations.indexOps(ProductDocument.class).exists();
        } catch (Exception e) {
            log.error("检查索引是否存在失败 - 错误: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void createProductIndex() {
        try {
            log.info("创建商品索引");
            elasticsearchOperations.indexOps(ProductDocument.class).create();
            elasticsearchOperations.indexOps(ProductDocument.class).putMapping();
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
            elasticsearchOperations.indexOps(ProductDocument.class).delete();
            log.info("✅ 商品索引删除成功");
        } catch (Exception e) {
            log.error("❌ 删除商品索引失败 - 错误: {}", e.getMessage(), e);
            throw new RuntimeException("删除商品索引失败", e);
        }
    }

    /**
     * 将事件转换为文档
     */
    private ProductDocument convertToDocument(ProductSearchEvent event) {
        return ProductDocument.builder()
                .id(String.valueOf(event.getProductId()))
                .productId(event.getProductId())
                .shopId(event.getShopId())
                .shopName(event.getShopName())
                .productName(event.getProductName())
                .price(event.getPrice())
                .stockQuantity(event.getStockQuantity())
                .categoryId(event.getCategoryId())
                .categoryName(event.getCategoryName())
                .brandId(event.getBrandId())
                .brandName(event.getBrandName())
                .status(event.getStatus())
                .description(event.getDescription())
                .imageUrl(event.getImageUrl())
                .tags(event.getTags())
                .salesCount(event.getSalesCount() != null ? event.getSalesCount() : 0)
                .rating(event.getRating() != null ? event.getRating() : BigDecimal.ZERO)
                .reviewCount(event.getReviewCount() != null ? event.getReviewCount() : 0)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .searchWeight(calculateSearchWeight(event))
                .hotScore(calculateHotScore(event))
                .recommended(false)
                .isNew(isNewProduct(event.getCreatedAt()))
                .isHot(isHotProduct(event.getSalesCount()))
                .build();
    }

    /**
     * 计算搜索权重
     */
    private Double calculateSearchWeight(ProductSearchEvent event) {
        double weight = 1.0;

        // 根据销量增加权重
        if (event.getSalesCount() != null && event.getSalesCount() > 0) {
            weight += Math.log10(event.getSalesCount()) * 0.1;
        }

        // 根据评分增加权重
        if (event.getRating() != null && event.getRating().compareTo(BigDecimal.ZERO) > 0) {
            weight += event.getRating().doubleValue() * 0.2;
        }

        return weight;
    }

    /**
     * 计算热度分数
     */
    private Double calculateHotScore(ProductSearchEvent event) {
        double score = 0.0;

        // 销量权重
        if (event.getSalesCount() != null) {
            score += event.getSalesCount() * 0.3;
        }

        // 评分权重
        if (event.getRating() != null) {
            score += event.getRating().doubleValue() * 20;
        }

        // 评价数量权重
        if (event.getReviewCount() != null) {
            score += event.getReviewCount() * 0.1;
        }

        return score;
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
}
