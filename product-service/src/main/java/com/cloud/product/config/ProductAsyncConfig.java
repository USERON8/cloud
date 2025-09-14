package com.cloud.product.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 商品服务异步配置类
 * 继承基础异步配置类，提供商品服务专用的线程池配置
 */
@Slf4j
@Configuration
@EnableAsync
public class ProductAsyncConfig extends BaseAsyncConfig {

    /**
     * 商品查询专用线程池
     * 根据商品服务的特点进行优化配置
     */
    @Bean("productQueryExecutor")
    public Executor productQueryExecutor() {
        // 根据商品查询的特点，使用更多核心线程来处理高并发查询
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),           // 最小4个核心线程
                processors * 4,                    // 最大线程数为CPU核心数的4倍
                500,                               // 较大的队列容量应对突发查询
                "product-query-"
        );
        executor.initialize();

        log.info("商品查询线程池初始化完成");
        return executor;
    }

    /**
     * 商品操作专用线程池
     * 用于处理商品变更等需要保证顺序性的操作
     */
    @Bean("productOperationExecutor")
    public Executor productOperationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,                                 // 核心线程数
                10,                                // 最大线程数
                100,                               // 队列容量
                "product-operation-"
        );
        executor.initialize();

        log.info("商品操作线程池初始化完成");
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务
     */
    @Bean("productCommonAsyncExecutor")
    public Executor productCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        log.info("商品服务通用异步线程池初始化完成");
        return executor;
    }
}