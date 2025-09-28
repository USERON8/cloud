package com.cloud.log.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 日志服务异步配置类
 * 提供日志服务专用的线程池配置
 * 
 * 日志服务特点：
 * - 高并发的日志写入
 * - 大量的ES批量操作
 * - 日志分析和统计
 * - 实时日志处理
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "log.async.enabled", havingValue = "true", matchIfMissing = true)
public class LogAsyncConfig {

    /**
     * 默认异步线程池
     */
    @Bean("defaultAsyncExecutor")
    public Executor defaultAsyncExecutor() {
        return createThreadPoolTaskExecutor(
                4,
                12,
                200,
                "log-default-"
        );
    }

    /**
     * 创建线程池执行器的通用方法
     */
    protected ThreadPoolTaskExecutor createThreadPoolTaskExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("✅ 线程池初始化完成: {} - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                threadNamePrefix, corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }

    /**
     * 创建写入类型的线程池执行器
     */
    protected ThreadPoolTaskExecutor createWriteExecutor(String threadNamePrefix) {
        return createThreadPoolTaskExecutor(6, 20, 500, threadNamePrefix);
    }

    /**
     * 创建查询类型的线程池执行器
     */
    protected ThreadPoolTaskExecutor createQueryExecutor(String threadNamePrefix) {
        return createThreadPoolTaskExecutor(4, 16, 300, threadNamePrefix);
    }

    /**
     * 创建CPU密集型的线程池执行器
     */
    protected ThreadPoolTaskExecutor createCPUExecutor(String threadNamePrefix) {
        int processorCount = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(processorCount, processorCount * 2, 100, threadNamePrefix);
    }

    /**创建通用异步线程池执行器
     */
    protected ThreadPoolTaskExecutor createCommonAsyncExecutor() {
        return createThreadPoolTaskExecutor(4, 12, 200, "log-common-async-");
    }

    /**
     * 日志写入异步线程池
     * 专门用于日志写入相关的异步处理
     * 高并发写入优化，适合大量日志数据处理
     */
    @Bean("logWriteExecutor")
    public Executor logWriteExecutor() {
        ThreadPoolTaskExecutor executor = createWriteExecutor("log-write-");
        
        log.info("✅ 日志写入线程池初始化完成");
        return executor;
    }

    /**
     * ES批量操作异步线程池
     * 专门用于Elasticsearch批量操作
     * 优化批量索引和批量查询性能
     */
    @Bean("logESBatchExecutor")
    public Executor logESBatchExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                4,
                12,
                1000,
                "log-es-batch-"
        );
        executor.initialize();

        log.info("✅ ES批量操作线程池初始化完成 - 核心线程数: 4, 最大线程数: 12, 队列容量: 1000");
        return executor;
    }

    /**
     * 日志查询异步线程池
     * 专门用于日志查询相关的异步处理
     * 高并发查询优化
     */
    @Bean("logQueryExecutor")
    public Executor logQueryExecutor() {
        ThreadPoolTaskExecutor executor = createQueryExecutor("log-query-");
        executor.initialize();

        log.info("✅ 日志查询线程池初始化完成");
        return executor;
    }

    /**
     * 日志分析异步线程池
     * 专门用于日志分析和统计
     * CPU密集型任务优化
     */
    @Bean("logAnalysisExecutor")
    public Executor logAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = createCPUExecutor("log-analysis-");
        executor.initialize();

        log.info("✅ 日志分析线程池初始化完成");
        return executor;
    }

    /**
     * 日志清理异步线程池
     * 专门用于日志清理和归档任务
     * 低优先级后台任务
     */
    @Bean("logCleanupExecutor")
    public Executor logCleanupExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                2,
                50,
                "log-cleanup-"
        );
        executor.initialize();

        log.info("✅ 日志清理线程池初始化完成 - 核心线程数: 1, 最大线程数: 2, 队列容量: 50");
        return executor;
    }

    /**
     * 实时日志处理异步线程池
     * 专门用于实时日志流处理
     * 低延迟高吞吐量优化
     */
    @Bean("logRealtimeExecutor")
    @ConditionalOnProperty(name = "log.realtime.enabled", havingValue = "true", matchIfMissing = true)
    public Executor logRealtimeExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                6,
                16,
                500,
                "log-realtime-"
        );
        executor.initialize();

        log.info("✅ 实时日志处理线程池初始化完成 - 核心线程数: 6, 最大线程数: 16, 队列容量: 500");
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务
     */
    @Bean("logCommonAsyncExecutor")
    public Executor logCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        log.info("✅ 日志服务通用异步线程池初始化完成");
        return executor;
    }
}
