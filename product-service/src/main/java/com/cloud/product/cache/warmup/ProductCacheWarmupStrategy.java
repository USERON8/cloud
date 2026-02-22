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















@Component
@Slf4j
@RequiredArgsConstructor
public class ProductCacheWarmupStrategy implements com.cloud.common.cache.warmup.CacheWarmupStrategy {

    private final ProductMapper productMapper;

    @Override
    public int warmup(CacheManager cacheManager) {
        int warmedUpCount = 0;

        try {
            

            return warmedUpCount;

        } catch (Exception e) {
            log.error("鍟嗗搧缂撳瓨棰勭儹澶辫触", e);
            return warmedUpCount;
        }
    }

    


    private int warmupHotProducts(Cache cache) {
        try {
            LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Product::getStatus, 1)  
                    .eq(Product::getIsHot, 1)  
                    .orderByDesc(Product::getSalesCount)  
                    .last("LIMIT 50");

            List<Product> hotProducts = productMapper.selectList(queryWrapper);
            

            int count = 0;
            for (Product product : hotProducts) {
                try {
                    cache.put(product.getId(), product);
                    count++;
                } catch (Exception e) {
                    log.warn("棰勭儹鐑攢鍟嗗搧 {} 澶辫触: {}", product.getId(), e.getMessage());
                }
            }

            

            return count;

        } catch (Exception e) {
            log.error("棰勭儹鎺ㄨ崘鍟嗗搧澶辫触", e);
            return 0;
        }
    }

    @Override
    public String getStrategyName() {
        return "ProductCacheWarmupStrategy";
    }
}
