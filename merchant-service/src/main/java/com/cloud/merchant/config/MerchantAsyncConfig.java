package com.cloud.merchant.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 商家服务异步配置类
 * 继承基础异步配置类，提供商家服务专用的线程池配置
 */
@Slf4j
@Configuration
@EnableAsync
public class MerchantAsyncConfig extends BaseAsyncConfig {

    /**
     * 商家查询专用线程池
     * 根据商家服务的特点进行优化配置
     */
    @Bean("merchantQueryExecutor")
    public Executor merchantQueryExecutor() {
        // 根据商家查询的特点，使用更多核心线程来处理高并发查询
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),           // 最小4个核心线程
                processors * 4,                    // 最大线程数为CPU核心数的4倍
                500,                               // 较大的队列容量应对突发查询
                "merchant-query-"
        );
        executor.initialize();
        
        log.info("商家查询线程池初始化完成");
        return executor;
    }

    /**
     * 商家操作专用线程池
     * 用于处理商家变更等需要保证顺序性的操作
     */
    @Bean("merchantOperationExecutor")
    public Executor merchantOperationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,                                 // 核心线程数
                10,                                // 最大线程数
                100,                               // 队列容量
                "merchant-operation-"
        );
        executor.initialize();
        
        log.info("商家操作线程池初始化完成");
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务
     */
    @Bean("merchantCommonAsyncExecutor")
    public Executor merchantCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        log.info("商家服务通用异步线程池初始化完成");
        return executor;
    }
}