package com.cloud.product.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 事务和缓存配置类
 * 确保商品服务的事务管理和多级缓存正确配置
 * 
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@EnableCaching(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class TransactionCacheConfig {

    /**
     * 配置初始化日志
     */
    public TransactionCacheConfig() {
        log.info("初始化商品服务事务和缓存配置");
        log.info("- 事务管理: 已启用 (支持回滚)");
        log.info("- 多级缓存: 已启用 (L1: Caffeine + L2: Redis)");
        log.info("- AOP代理: 已启用 (支持自调用)");
    }
}
