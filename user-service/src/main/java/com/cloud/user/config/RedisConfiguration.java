package com.cloud.user.config;

import com.cloud.common.config.base.EnhancedRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 用户服务Redis配置
 * 支持多级缓存架构，优化用户相关数据的存储
 * 使用Hash类型存储用户高频访问字段
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class RedisConfiguration extends EnhancedRedisConfig {

    @Override
    protected String getCacheKeyPrefix() {
        return "user";
    }

    /**
     * 用户服务缓存过期时间配置
     */
    @Override
    protected long getCacheExpireTime(String type) {
        switch (type) {
            case "userInfo":
                return 3600L; // 1小时
            case "userProfile":
                return 1800L; // 30分钟
            case "userAddress":
                return 2700L; // 45分钟
            case "userStats":
                return 900L;  // 15分钟
            default:
                return 1800L; // 默认30分钟
        }
    }
}
