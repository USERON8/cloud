package com.cloud.stock.config;

import com.cloud.common.config.BaseRedisConfig;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 启用Spring Cache注解支持
 */
@Configuration
@EnableCaching
public class RedisConfig extends BaseRedisConfig {
}