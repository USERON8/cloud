package com.cloud.product.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * 商品服务缓存配置
 * 
 * 使用标准Spring Cache接口，支持条件化多级缓存策略。
 * 通过cache.multi-level配置项自动选择：
 * - true: 启用Caffeine+Redis双级缓存
 * - false: 使用标准Redis缓存
 * 
 * 缓存特点：
 * - 商品基础信息相对稳定，适合本地缓存
 * - 商品库存信息更新频繁，依赖Redis同步
 * - 商品分类信息变化少，可长时间缓存
 * - 商品搜索结果需要跨节点共享
 * - 支持L1(Caffeine) + L2(Redis)多级缓存
 * 
 * @author CloudDevAgent
 * @version 2.0
 * @since 2025-09-26
 */
@Configuration
@Slf4j
public class ProductCacheConfig {

    @Autowired
    private CacheManager cacheManager;

    /**
     * 应用启动完成后记录缓存配置信息
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logCacheConfiguration() {
        String cacheType = cacheManager.getClass().getSimpleName();
        log.info("🚀 商品服务缓存配置完成: type={}, names={}", 
                cacheType, cacheManager.getCacheNames());
        
        if ("MultiLevelCacheManager".equals(cacheType)) {
            log.info("🔥 启用多级缓存 - L1:Caffeine + L2:Redis，支持跨节点一致性");
        } else {
            log.info("🔧 使用标准Redis缓存 - 单级分布式缓存");
        }
    }
}
