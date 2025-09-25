package com.cloud.log.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 日志服务事务和缓存配置
 * 确保事务管理和缓存正确配置
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class TransactionCacheConfig {

    public TransactionCacheConfig() {
        log.info("日志服务事务和缓存配置初始化完成");
    }
}
