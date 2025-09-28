package com.cloud.stock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 库存服务异步配置类
 * 提供库存服务专用的线程池配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableAsync
public class StockAsyncConfig {

    /**
     * 库存查询专用线程池
     * 根据库存服务的特点进行优化配置
     */
    @Bean("stockQueryExecutor")
    public Executor stockQueryExecutor() {
        // 根据库存查询的特点，使用更多核心线程来处理高并发查询
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),
                processors * 4,
                500,
                "stock-query-"
        );
        executor.initialize();

        log.info("库存查询线程池初始化完成");
        return executor;
    }

    /**
     * 库存操作专用线程池
     * 用于处理库存变更等需要保证顺序性的操作
     */
    @Bean("stockOperationExecutor")
    public Executor stockOperationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                10,
                100,
                "stock-operation-"
        );
        executor.initialize();

        log.info("库存操作线程池初始化完成");
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务
     */
    @Bean("stockCommonAsyncExecutor")
    public Executor stockCommonAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(2, processors / 2),
                processors * 2,
                200,
                "stock-common-async-"
        );
        executor.initialize();

        log.info("库存服务通用异步线程池初始化完成");
        return executor;
    }
    
    /**
     * 创建线程池任务执行器的工厂方法
     * 提供统一的线程池配置模板
     *
     * @param corePoolSize     核心线程数
     * @param maxPoolSize      最大线程数
     * @param queueCapacity    队列容量
     * @param threadNamePrefix 线程名前缀
     * @return ThreadPoolTaskExecutor
     */
    private ThreadPoolTaskExecutor createThreadPoolTaskExecutor(int corePoolSize, int maxPoolSize,
                                                                  int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 基本配置
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        // 高级配置
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(false);

        // 拒绝策略：调用者运行策略（保证任务不丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 优雅关闭配置
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        return executor;
    }
}
