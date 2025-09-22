package com.cloud.stock.config;

import com.cloud.common.config.base.EnhancedRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 库存服务Redis配置
 * 仅使用Redis分布式缓存，不使用本地缓存
 * 库存数据需要保证强一致性和实时性，避免超卖问题
 * 使用Hash类型存储库存数量、冻结数量等高频字段
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class RedisConfig extends EnhancedRedisConfig {

    @Override
    protected String getCacheKeyPrefix() {
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
            default:
                return 300L;  // 默认5分钟
        }
    }

    /**
     * 库存服务启用事务支持
     * 用于保证库存扣减的原子性操作
     */
    @Override
    protected boolean shouldEnableTransactionSupport() {
        return true;
    }
}
