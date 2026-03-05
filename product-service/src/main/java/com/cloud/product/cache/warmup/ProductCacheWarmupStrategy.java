package com.cloud.product.cache.warmup;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.product.converter.ProductConverter;
import com.cloud.product.mapper.ProductMapper;
import com.cloud.product.module.entity.Product;
import com.cloud.product.service.support.ProductCacheProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductCacheWarmupStrategy implements com.cloud.common.cache.warmup.CacheWarmupStrategy {

    private final ProductMapper productMapper;
    private final ProductConverter productConverter;
    private final ProductCacheProtectionService productCacheProtectionService;

    @Value("${product.cache.warmup.hot-size:50}")
    private int hotProductsSize;

    @Value("${product.cache.warmup.home-size:30}")
    private int homeProductsSize;

    @Value("${product.cache.warmup.rank-size:50}")
    private int rankProductsSize;

    @Value("${product.cache.warmup.detail-size:100}")
    private int detailProductsSize;

    @Override
    public int warmup(CacheManager cacheManager) {
        Cache productCache = cacheManager.getCache("productCache");
        if (productCache == null) {
            log.warn("Skip product cache warmup because productCache is not configured");
            return 0;
        }

        try {
            int detailCount = warmupLatestEnabledProducts(productCache);
            int hotCount = warmupHotProducts();
            int homeCount = warmupHomeProducts();
            int rankCount = warmupRankingProducts();
            int statsCount = warmupStatsAndSnapshot();
            int total = detailCount + hotCount + homeCount + rankCount + statsCount;
            log.info("Product cache warmup completed: total={}, detail={}, hot={}, home={}, ranking={}, stats={}",
                    total, detailCount, hotCount, homeCount, rankCount, statsCount);
            return total;
        } catch (Exception e) {
            log.error("Product cache warmup failed", e);
            return 0;
        }
    }

    private int warmupLatestEnabledProducts(Cache cache) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getUpdatedAt)
                .last("LIMIT " + Math.max(10, detailProductsSize));

        List<Product> products = productMapper.selectList(queryWrapper);
        int count = 0;
        for (Product product : products) {
            if (product == null || product.getId() == null) {
                continue;
            }
            try {
                var vo = productConverter.toVO(product);
                cache.put(product.getId(), vo);
                productCacheProtectionService.preloadProductDetailCache(product.getId(), vo);
                count++;
            } catch (Exception e) {
                log.warn("Warmup product cache entry failed: productId={}", product.getId(), e);
            }
        }
        return count;
    }

    private int warmupHotProducts() {
        List<com.cloud.common.domain.vo.product.ProductVO> products = queryEnabledProductsByUpdatedAt(
                Math.max(10, hotProductsSize));
        if (products.isEmpty()) {
            return 0;
        }
        productCacheProtectionService.preloadProductListCache("hot:products", products);
        return products.size();
    }

    private int warmupHomeProducts() {
        List<com.cloud.common.domain.vo.product.ProductVO> products = queryEnabledProductsByUpdatedAt(
                Math.max(10, homeProductsSize));
        if (products.isEmpty()) {
            return 0;
        }
        productCacheProtectionService.preloadProductListCache("home:products", products);
        return products.size();
    }

    private int warmupRankingProducts() {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getStock)
                .orderByDesc(Product::getUpdatedAt)
                .last("LIMIT " + Math.max(10, rankProductsSize));
        List<Product> products = productMapper.selectList(queryWrapper);
        if (products == null || products.isEmpty()) {
            return 0;
        }
        List<com.cloud.common.domain.vo.product.ProductVO> ranking = new ArrayList<>(products.size());
        for (Product product : products) {
            if (product == null) {
                continue;
            }
            ranking.add(productConverter.toVO(product));
        }
        productCacheProtectionService.preloadProductListCache("ranking:products", ranking);
        return ranking.size();
    }

    private int warmupStatsAndSnapshot() {
        Long enabledCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
        );
        Long totalCount = productMapper.selectCount(new LambdaQueryWrapper<Product>());
        long safeEnabled = enabledCount == null ? 0L : enabledCount;
        long safeTotal = totalCount == null ? 0L : totalCount;
        productCacheProtectionService.preloadProductStatsCache("enabled:count", safeEnabled);
        productCacheProtectionService.preloadProductStatsCache("total:count", safeTotal);

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("totalCount", safeTotal);
        snapshot.put("enabledCount", safeEnabled);
        snapshot.put("hotProducts", queryEnabledProductsByUpdatedAt(Math.max(10, hotProductsSize)));
        snapshot.put("homeProducts", queryEnabledProductsByUpdatedAt(Math.max(10, homeProductsSize)));
        snapshot.put("rankingProducts", queryRankingProducts(Math.max(10, rankProductsSize)));
        productCacheProtectionService.preloadProductStatsCache("home:snapshot", snapshot);
        return 3;
    }

    private List<com.cloud.common.domain.vo.product.ProductVO> queryEnabledProductsByUpdatedAt(int limit) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getUpdatedAt)
                .last("LIMIT " + limit);
        List<Product> products = productMapper.selectList(queryWrapper);
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        return productConverter.toVOList(products);
    }

    private List<com.cloud.common.domain.vo.product.ProductVO> queryRankingProducts(int limit) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getStock)
                .orderByDesc(Product::getUpdatedAt)
                .last("LIMIT " + limit);
        List<Product> products = productMapper.selectList(queryWrapper);
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        return productConverter.toVOList(products);
    }

    @Override
    public String getStrategyName() {
        return "ProductCacheWarmupStrategy";
    }
}
