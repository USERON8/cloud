package com.cloud.product.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

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
public class ProductAsyncConfig {

    /**
     * 商品业务异步线程池
     * 专门用于商品相关的异步业务处理
     */
    @Bean("productAsyncExecutor")
    public Executor productAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：2
        executor.setCorePoolSize(2);

        // 最大线程数：4
        executor.setMaxPoolSize(4);

        // 队列容量：500
        executor.setQueueCapacity(500);

        // 线程空闲时间：60秒
        executor.setKeepAliveSeconds(60);

        // 线程名前缀
        executor.setThreadNamePrefix("Product-Async-");

        // 拒绝策略：调用者运行策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：30秒
        executor.setAwaitTerminationSeconds(30);

        // 初始化线程池
        executor.initialize();

        log.info("✅ 商品异步线程池配置完成 - 核心线程数: 2, 最大线程数: 4, 队列容量: 500");

        return executor;
    }

    /**
     * 商品日志异步线程池
     * 专门用于商品日志处理
     */
    @Bean("productLogExecutor")
    public Executor productLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：1
        executor.setCorePoolSize(1);

        // 最大线程数：2
        executor.setMaxPoolSize(2);

        // 队列容量：1000
        executor.setQueueCapacity(1000);

        // 线程空闲时间：120秒
        executor.setKeepAliveSeconds(120);

        // 线程名前缀
        executor.setThreadNamePrefix("Product-Log-");

        // 拒绝策略：丢弃最老的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：60秒
        executor.setAwaitTerminationSeconds(60);

        // 初始化线程池
        executor.initialize();

        log.info("✅ 商品日志线程池配置完成 - 核心线程数: 1, 最大线程数: 2, 队列容量: 1000");

        return executor;
    }

    /**
     * 商品统计异步线程池
     * 专门用于商品统计数据处理
     */
    @Bean("productStatisticsExecutor")
    @ConditionalOnProperty(name = "product.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor productStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：1
        executor.setCorePoolSize(1);

        // 最大线程数：3
        executor.setMaxPoolSize(3);

        // 队列容量：2000
        executor.setQueueCapacity(2000);

        // 线程空闲时间：180秒
        executor.setKeepAliveSeconds(180);

        // 线程名前缀
        executor.setThreadNamePrefix("Product-Statistics-");

        // 拒绝策略：丢弃最老的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：60秒
        executor.setAwaitTerminationSeconds(60);

        // 初始化线程池
        executor.initialize();

        log.info("✅ 商品统计线程池配置完成 - 核心线程数: 1, 最大线程数: 3, 队列容量: 2000");

        return executor;
    }

    /**
     * 商品搜索异步线程池
     * 专门用于商品搜索索引更新
     */
    @Bean("productSearchExecutor")
    @ConditionalOnProperty(name = "product.search.enabled", havingValue = "true", matchIfMissing = true)
    public Executor productSearchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：1
        executor.setCorePoolSize(1);

        // 最大线程数：2
        executor.setMaxPoolSize(2);

        // 队列容量：300
        executor.setQueueCapacity(300);

        // 线程空闲时间：90秒
        executor.setKeepAliveSeconds(90);

        // 线程名前缀
        executor.setThreadNamePrefix("Product-Search-");

        // 拒绝策略：调用者运行策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：30秒
        executor.setAwaitTerminationSeconds(30);

        // 初始化线程池
        executor.initialize();

        log.info("✅ 商品搜索线程池配置完成 - 核心线程数: 1, 最大线程数: 2, 队列容量: 300");

        return executor;
    }
}
