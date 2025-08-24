package com.cloud.order.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 订单服务异步配置类
 * 继承基础异步配置类，提供订单服务专用的线程池配置
 */
@Slf4j
@Configuration
@EnableAsync
public class OrderAsyncConfig extends BaseAsyncConfig {

    /**
     * 订单查询专用线程池
     * 根据订单服务的特点进行优化配置
     */
    @Bean("orderQueryExecutor")
    public Executor orderQueryExecutor() {
        // 根据订单查询的特点，使用更多核心线程来处理高并发查询
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),           // 最小4个核心线程
                processors * 4,                    // 最大线程数为CPU核心数的4倍
                500,                               // 较大的队列容量应对突发查询
                "order-query-"
        );
        executor.initialize();
        
        log.info("订单查询线程池初始化完成");
        return executor;
    }

    /**
     * 订单操作专用线程池
     * 用于处理订单变更等需要保证顺序性的操作
     */
    @Bean("orderOperationExecutor")
    public Executor orderOperationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,                                 // 核心线程数
                10,                                // 最大线程数
                100,                               // 队列容量
                "order-operation-"
        );
        executor.initialize();
        
        log.info("订单操作线程池初始化完成");
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务
     */
    @Bean("orderCommonAsyncExecutor")
    public Executor orderCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        log.info("订单服务通用异步线程池初始化完成");
        return executor;
    }
}