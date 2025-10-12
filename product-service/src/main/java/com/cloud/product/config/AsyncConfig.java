package com.cloud.product.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 商品服务异步配置
 * 专门为商品服务配置异步线程池
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "product.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    /**
     * 商品业务异步线程池
     * 专门用于商品相关的异步业务处理
     */
    @Bean("productAsyncExecutor")
    public Executor productAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                500,
                "product-async-"
        );
        executor.initialize();

        log.info("✅ [PRODUCT-SERVICE-ASYNC] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 商品日志异步线程池
     * 专门用于商品日志处理
     */
    @Bean("productLogExecutor")
    public Executor productLogExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                2,
                1000,
                "product-log-"
        );
        executor.initialize();

        log.info("✅ [PRODUCT-SERVICE-LOG] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 商品统计异步线程池
     * 专门用于商品统计数据处理
     */
    @Bean("productStatisticsExecutor")
    @ConditionalOnProperty(name = "product.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor productStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                500,
                "product-statistics-"
        );
        executor.initialize();

        log.info("✅ [PRODUCT-SERVICE-STATISTICS] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 商品搜索异步线程池
     * 专门用于商品搜索索引更新
     */
    @Bean("productSearchExecutor")
    @ConditionalOnProperty(name = "product.search.enabled", havingValue = "true", matchIfMissing = true)
    public Executor productSearchExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                2,
                300,
                "product-search-"
        );
        executor.initialize();

        log.info("✅ [PRODUCT-SERVICE-SEARCH] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }
}
