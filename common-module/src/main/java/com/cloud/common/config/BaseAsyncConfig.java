package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 基础异步线程池配置类
 * 提供基础线程池配置，供各服务继承和扩展
 */
@Slf4j
public abstract class BaseAsyncConfig {

    /**
     * 创建基础线程池
     *
     * @param corePoolSize    核心线程数
     * @param maxPoolSize     最大线程数
     * @param queueCapacity   队列容量
     * @param threadNamePrefix 线程名前缀
     * @return 线程池任务执行器
     */
    protected ThreadPoolTaskExecutor createThreadPoolTaskExecutor(
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            String threadNamePrefix) {
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        log.info("线程池配置完成: 核心线程数={}, 最大线程数={}, 队列容量={}, 线程名前缀={}",
                corePoolSize, maxPoolSize, queueCapacity, threadNamePrefix);
        
        return executor;
    }

    /**
     * 创建库存查询专用线程池（默认配置）
     *
     * @return 线程池任务执行器
     */
    protected ThreadPoolTaskExecutor createStockQueryExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                processors,
                processors * 2,
                200,
                "stock-query-"
        );
    }

    /**
     * 创建通用异步线程池（默认配置）
     *
     * @return 线程池任务执行器
     */
    protected ThreadPoolTaskExecutor createCommonAsyncExecutor() {
        return createThreadPoolTaskExecutor(
                4,
                8,
                100,
                "common-async-"
        );
    }
}