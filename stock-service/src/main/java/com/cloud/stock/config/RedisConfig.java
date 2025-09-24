package com.cloud.stock.config;

import com.cloud.common.config.RedisConfigFactory;
import com.cloud.common.config.base.BaseRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 库存服务Redis配置
 * 仅使用Redis分布式缓存，不使用本地缓存
 * 库存数据需要保证强一致性和实时性，避免超卖问题
 * 使用高性能配置，支持事务（用于库存扣减）
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class RedisConfig extends BaseRedisConfig {

    /**
     * 库存服务专用的RedisTemplate配置
     * 使用高性能配置，支持事务（用于库存扣减操作）
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化库存服务Redis配置");
        return RedisConfigFactory.createHighPerformanceRedisTemplate(redisConnectionFactory);
    }

    @Override
    protected String getServicePrefix() {
        return "stock";
    }

    /**
     * 库存服务缓存过期时间配置
     * 库存数据使用较短的缓存时间，保证实时性
     */
    @Override
    protected long getCacheExpireTime(String type) {
        switch (type) {
            case "stockInfo":
                return 300L;  // 5分钟（库存变化频繁）
            case "stockStatus":
                return 180L;  // 3分钟（状态更新频繁）
            case "stockStats":
                return 120L;  // 2分钟（统计数据实时性要求高）
            case "stockHistory":
                return 1800L; // 30分钟（历史记录相对稳定）
            case "stockAlert":
                return 600L;  // 10分钟（库存预警）
            case "stockLock":
                return 60L;   // 1分钟（库存锁定）
            default:
                return 300L;  // 默认5分钟
        }
    }

    /**
     * 库存服务需要事务支持（用于库存扣减操作）
     */
    @Override
    protected boolean shouldEnableTransactionSupport() {
        return true;
    }
}
