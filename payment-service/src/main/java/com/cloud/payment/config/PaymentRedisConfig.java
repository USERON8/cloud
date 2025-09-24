package com.cloud.payment.config;

import com.cloud.common.config.RedisConfigFactory;
import com.cloud.common.config.base.BaseRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 支付服务Redis配置
 * 仅使用Redis分布式缓存，不使用本地缓存
 * 支付数据需要保证强一致性和安全性，支持事务操作
 * 使用高性能配置，支持支付状态管理
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class PaymentRedisConfig extends BaseRedisConfig {

    /**
     * 支付服务专用的RedisTemplate配置
     * 使用高性能配置，支持事务（用于支付状态管理）
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化支付服务Redis配置");
        return RedisConfigFactory.createHighPerformanceRedisTemplate(redisConnectionFactory);
    }

    @Override
    protected String getServicePrefix() {
        return "payment";
    }

    /**
     * 支付服务缓存过期时间配置
     * 支付数据根据敏感性设置不同的缓存时间
     */
    @Override
    protected long getCacheExpireTime(String type) {
        switch (type) {
            case "paymentInfo":
                return 1800L; // 30分钟（支付基础信息）
            case "paymentStatus":
                return 300L;  // 5分钟（支付状态）
            case "paymentMethod":
                return 3600L; // 1小时（支付方式）
            case "paymentChannel":
                return 7200L; // 2小时（支付渠道）
            case "paymentStats":
                return 600L;  // 10分钟（支付统计）
            case "paymentHistory":
                return 3600L; // 1小时（支付历史）
            case "paymentToken":
                return 900L;  // 15分钟（支付令牌）
            case "paymentSession":
                return 1800L; // 30分钟（支付会话）
            case "paymentCallback":
                return 300L;  // 5分钟（支付回调）
            default:
                return 1800L; // 默认30分钟
        }
    }

    /**
     * 支付服务需要事务支持（用于支付状态管理）
     */
    @Override
    protected boolean shouldEnableTransactionSupport() {
        return true;
    }
}
