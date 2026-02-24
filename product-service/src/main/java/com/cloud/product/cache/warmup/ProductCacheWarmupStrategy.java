package com.cloud.product.cache.warmup;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.product.converter.ProductConverter;
import com.cloud.product.mapper.ProductMapper;
import com.cloud.product.module.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductCacheWarmupStrategy implements com.cloud.common.cache.warmup.CacheWarmupStrategy {

    private final ProductMapper productMapper;
    private final ProductConverter productConverter;

    @Override
    public int warmup(CacheManager cacheManager) {
        Cache productCache = cacheManager.getCache("productCache");
        if (productCache == null) {
            log.warn("Skip product cache warmup because productCache is not configured");
            return 0;
        }

        try {
            return warmupLatestEnabledProducts(productCache);
        } catch (Exception e) {
            log.error("Product cache warmup failed", e);
            return 0;
        }
    }

    private int warmupLatestEnabledProducts(Cache cache) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getUpdatedAt)
                .last("LIMIT 100");

        List<Product> products = productMapper.selectList(queryWrapper);
        int count = 0;
        for (Product product : products) {
            if (product == null || product.getId() == null) {
                continue;
            }
            try {
                cache.put(product.getId(), productConverter.toVO(product));
                count++;
            } catch (Exception e) {
                log.warn("Warmup product cache entry failed: productId={}", product.getId(), e);
            }
        }
        log.info("Product cache warmup completed: warmedCount={}", count);
        return count;
    }

    @Override
    public String getStrategyName() {
        return "ProductCacheWarmupStrategy";
    }
}
