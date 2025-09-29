package com.cloud.search.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 搜索服务缓存配置
 * 
 * 主要功能：
 * - 启用多级缓存(Caffeine + Redis)
 * - 专用缓存database:7
 * - 搜索结果缓存优化
 * 
 * @author CloudDevAgent
 * @version 1.0
 * @since 2025-09-26
 */
@Slf4j
@Configuration
public class SearchCacheConfig {
    
    // 注意：多级缓存配置通过common-module的CacheConfigFactory自动启用
    // 在application.yml中设置 cache.multi-level=true 即可启用多级缓存
    
    static {
        log.info("🔍 搜索服务缓存配置初始化: 支持多级缓存(Caffeine + Redis database:7)");
    }
}
