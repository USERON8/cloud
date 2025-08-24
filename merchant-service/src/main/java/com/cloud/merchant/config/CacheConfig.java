package com.cloud.merchant.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 启用Spring Cache注解支持
 */
@Configuration
@EnableCaching
public class CacheConfig {
}