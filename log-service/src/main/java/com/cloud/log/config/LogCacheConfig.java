package com.cloud.log.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 日志服务缓存配置
 * 日志服务主要用于写入，缓存需求相对较少
 * 主要缓存查询结果和存在性检查
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableCaching
public class LogCacheConfig {

    /**
     * 日志服务专用的缓存管理器
     * 使用简单的内存缓存，主要用于查询优化
     */
    @Bean
    @Primary
    public CacheManager logCacheManager() {
        log.info("初始化日志服务缓存管理器");
        
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // 设置缓存名称
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "userEventCache",           // 用户事件缓存
                "userEventListCache",       // 用户事件列表缓存
                "userEventExistsCache",     // 用户事件存在性缓存
                "orderEventCache",          // 订单事件缓存
                "paymentEventCache",        // 支付事件缓存
                "stockEventCache",          // 库存事件缓存
                "logStatsCache"             // 日志统计缓存
        ));
        
        // 允许空值缓存
        cacheManager.setAllowNullValues(true);
        
        log.info("日志服务缓存管理器初始化完成");
        return cacheManager;
    }
}
