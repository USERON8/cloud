package com.cloud.log.config;

import com.cloud.common.config.RedisConfigFactory;
import com.cloud.common.config.base.BaseRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 日志服务Redis配置
 * 仅使用Redis分布式缓存，不使用本地缓存
 * 日志数据主要用于缓存统计信息和热点数据
 * 使用基础配置，不需要事务支持
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class LogRedisConfig extends BaseRedisConfig {

    /**
     * 日志服务专用的RedisTemplate配置
     * 使用基础配置，不需要事务支持
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化日志服务Redis配置");
        return RedisConfigFactory.createBasicRedisTemplate(redisConnectionFactory);
    }

    @Override
    protected String getServicePrefix() {
        return "log";
    }

    /**
     * 日志服务缓存过期时间配置
     * 日志数据根据类型设置不同的缓存时间
     */
    @Override
    protected long getCacheExpireTime(String type) {
        switch (type) {
            case "logStats":
                return 3600L; // 1小时（日志统计）
            case "errorStats":
                return 1800L; // 30分钟（错误统计）
            case "accessStats":
                return 900L;  // 15分钟（访问统计）
            case "performanceStats":
                return 600L;  // 10分钟（性能统计）
            case "userActivity":
                return 7200L; // 2小时（用户活动）
            case "systemMetrics":
                return 300L;  // 5分钟（系统指标）
            case "alertRule":
                return 86400L; // 24小时（告警规则）
            case "logConfig":
                return 86400L; // 24小时（日志配置）
            default:
                return 3600L; // 默认1小时
        }
    }

    /**
     * 日志服务不需要事务支持（主要用于统计和查询）
     */
    @Override
    protected boolean shouldEnableTransactionSupport() {
        return false;
    }
}
