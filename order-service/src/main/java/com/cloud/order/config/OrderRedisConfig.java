package com.cloud.order.config;

import com.cloud.common.config.RedisConfigFactory;
import com.cloud.common.config.base.BaseRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 订单服务Redis配置
 * 仅使用Redis分布式缓存，不使用本地缓存
 * 订单数据需要保证强一致性，支持事务操作
 * 使用高性能配置，支持订单状态管理
 *
 * @author what's up
 */
@Slf4j
@Configuration
public class OrderRedisConfig extends BaseRedisConfig {

    /**
     * 订单服务专用的RedisTemplate配置
     * 使用高性能配置，支持事务（用于订单状态管理）
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("初始化订单服务Redis配置");
        return RedisConfigFactory.createHighPerformanceRedisTemplate(redisConnectionFactory);
    }

    @Override
    protected String getServicePrefix() {
        return "order";
    }

    /**
     * 订单服务缓存过期时间配置
     * 订单数据根据状态设置不同的缓存时间
     */
    @Override
    protected long getCacheExpireTime(String type) {
        switch (type) {
            case "orderInfo":
                return 1800L; // 30分钟（订单基础信息）
            case "orderStatus":
                return 600L;  // 10分钟（订单状态）
            case "orderList":
                return 900L;  // 15分钟（订单列表）
            case "orderStats":
                return 300L;  // 5分钟（订单统计）
            case "orderPayment":
                return 1800L; // 30分钟（支付信息）
            case "orderShipping":
                return 3600L; // 1小时（物流信息）
            case "orderHistory":
                return 7200L; // 2小时（订单历史）
            case "orderSession":
                return 1800L; // 30分钟（订单会话）
            default:
                return 1800L; // 默认30分钟
        }
    }

    /**
     * 订单服务需要事务支持（用于订单状态管理）
     */
    @Override
    protected boolean shouldEnableTransactionSupport() {
        return true;
    }
}
