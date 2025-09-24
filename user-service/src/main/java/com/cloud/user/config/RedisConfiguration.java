package com.cloud.user.config;

import com.cloud.common.config.RedisConfigFactory;
import com.cloud.common.config.base.BaseRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 用户服务Redis配置
 * 支持多级缓存架构，优化用户相关数据的存储
 * 使用高性能配置，支持用户会话和缓存场景
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class RedisConfiguration extends BaseRedisConfig {

    /**
     * 用户服务专用的RedisTemplate配置
     * 使用高性能配置，支持事务（用于会话管理）
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化用户服务Redis配置");
        return RedisConfigFactory.createHighPerformanceRedisTemplate(redisConnectionFactory);
    }

    @Override
    protected String getServicePrefix() {
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
            case "userSession":
                return 7200L; // 2小时（会话）
            case "userToken":
                return 1800L; // 30分钟（令牌）
            case "userPermission":
                return 3600L; // 1小时（权限）
            default:
                return 1800L; // 默认30分钟
        }
    }

    /**
     * 用户服务需要事务支持（用于会话管理）
     */
    @Override
    protected boolean shouldEnableTransactionSupport() {
        return true;
    }
}
