package com.cloud.product.cache.warmup;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.product.mapper.ProductMapper;
import com.cloud.product.module.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商品缓存预热策略
 * <p>
 * 预热内容:
 * 1. 热销商品 (is_hot = 1, 按销量倒序, 取前50条)
 * 2. 推荐商品 (is_recommended = 1, 按排序权重倒序, 取前30条)
 * 3. 上架状态的商品 (status = 1)
 * <p>
 * 预热目标缓存:
 * - productInfo: 商品基本信息缓存(45分钟TTL)
 *
 * @author CloudDevAgent
 * @since 2025-10-12
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductCacheWarmupStrategy implements com.cloud.common.cache.warmup.CacheWarmupStrategy {

    private final ProductMapper productMapper;

    @Override
    public int warmup(CacheManager cacheManager) {
        int warmedUpCount = 0;

        try {
            log.info("开始执行商品缓存预热策略");

            // 1. 获取缓存实例
            Cache productInfoCache = cacheManager.getCache("productInfo");
            if (productInfoCache == null) {
                log.warn("未找到 productInfo 缓存实例,跳过预热");
                return 0;
            }

            // 2. 预热热销商品 (前50条)
            warmedUpCount += warmupHotProducts(productInfoCache);

            // 3. 预热推荐商品 (前30条)
            warmedUpCount += warmupRecommendedProducts(productInfoCache);

            log.info("商品缓存预热完成: 成功预热 {} 个商品", warmedUpCount);
            return warmedUpCount;

        } catch (Exception e) {
            log.error("商品缓存预热失败", e);
            return warmedUpCount;
        }
    }

    /**
     * 预热热销商品
     */
    private int warmupHotProducts(Cache cache) {
        try {
            LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Product::getStatus, 1)  // 上架状态
                    .eq(Product::getIsHot, 1)  // 热销商品
                    .orderByDesc(Product::getSalesCount)  // 按销量倒序
                    .last("LIMIT 50");

            List<Product> hotProducts = productMapper.selectList(queryWrapper);
            log.info("查询到 {} 个热销商品,开始预热...", hotProducts.size());

            int count = 0;
            for (Product product : hotProducts) {
                try {
                    cache.put(product.getId(), product);
                    count++;
                } catch (Exception e) {
                    log.warn("预热热销商品 {} 失败: {}", product.getId(), e.getMessage());
                }
            }

            log.info("热销商品预热完成: {} 个", count);
            return count;

        } catch (Exception e) {
            log.error("预热热销商品失败", e);
            return 0;
        }
    }

    /**
     * 预热推荐商品
     */
    private int warmupRecommendedProducts(Cache cache) {
        try {
            LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Product::getStatus, 1)  // 上架状态
                    .eq(Product::getIsRecommended, 1)  // 推荐商品
                    .orderByDesc(Product::getSortOrder)  // 按排序权重倒序
                    .last("LIMIT 30");

            List<Product> recommendedProducts = productMapper.selectList(queryWrapper);
            log.info("查询到 {} 个推荐商品,开始预热...", recommendedProducts.size());

            int count = 0;
            for (Product product : recommendedProducts) {
                try {
                    // 避免重复预热(热销商品可能也是推荐商品)
                    if (cache.get(product.getId()) == null) {
                        cache.put(product.getId(), product);
                        count++;
                    }
                } catch (Exception e) {
                    log.warn("预热推荐商品 {} 失败: {}", product.getId(), e.getMessage());
                }
            }

            log.info("推荐商品预热完成: {} 个", count);
            return count;

        } catch (Exception e) {
            log.error("预热推荐商品失败", e);
            return 0;
        }
    }

    @Override
    public String getStrategyName() {
        return "ProductCacheWarmupStrategy";
    }
}
