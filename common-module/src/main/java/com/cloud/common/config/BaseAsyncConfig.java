package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步处理基础配置类
 * 提供通用的线程池配置模板和工厂方法
 * 各服务可以继承此类并根据业务特点定制线程池
 *
 * @author cloud
 * @date 2024-01-20
 * @version 2.0
 */
@Slf4j
@Configuration
@EnableAsync
public class BaseAsyncConfig implements AsyncConfigurer {

    /**
     * 获取默认异步执行器
     * 实现AsyncConfigurer接口，提供全局默认异步执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        return defaultAsyncExecutor();
    }

    /**
     * 默认异步任务执行器
     * 适用于通用异步任务处理
     *
     * @return Executor
     */
    @Bean("defaultAsyncExecutor")
    public Executor defaultAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),
                processors * 3,
                300,
                "async-default-"
        );
        executor.initialize();
        log.info("✅ 默认异步线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: 300",
                Math.max(4, processors), processors * 3);
        return executor;
    }



    /**
     * 消息发送异步任务执行器
     * 专门用于消息发送相关的异步任务
     *
     * @return Executor
     */
    @Bean("asyncMessageExecutor")
    public Executor asyncMessageExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                8,
                100,
                "async-message-"
        );
        executor.initialize();
        log.info("✅ 消息异步线程池初始化完成 - 核心线程数: 3, 最大线程数: 8, 队列容量: 100");
        return executor;
    }

    /**
     * 批处理异步任务执行器
     * 专门用于批量消息处理相关的异步任务
     *
     * @return Executor
     */
    @Bean("batchProcessExecutor")
    public Executor batchProcessExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                200,
                "batch-process-"
        );
        executor.initialize();
        log.info("✅ 批处理异步线程池初始化完成 - 核心线程数: 2, 最大线程数: 6, 队列容量: 200");
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
    protected ThreadPoolTaskExecutor createThreadPoolTaskExecutor(int corePoolSize, int maxPoolSize,
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

    /**
     * 创建通用异步执行器
     * 适用于各服务的通用异步任务
     *
     * @return ThreadPoolTaskExecutor
     */
    protected ThreadPoolTaskExecutor createCommonAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                Math.max(2, processors / 2),
                processors * 2,
                200,
                "common-async-"
        );
    }

    /**
     * 创建高并发查询专用线程池
     * 适用于查询密集型业务
     *
     * @param threadNamePrefix 线程名前缀
     * @return ThreadPoolTaskExecutor
     */
    protected ThreadPoolTaskExecutor createQueryExecutor(String threadNamePrefix) {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                Math.max(4, processors),
                processors * 4,
                500,
                threadNamePrefix
        );
    }

    /**
     * 创建写操作专用线程池
     * 适用于写操作密集型业务，控制并发度保证数据一致性
     *
     * @param threadNamePrefix 线程名前缀
     * @return ThreadPoolTaskExecutor
     */
    protected ThreadPoolTaskExecutor createWriteExecutor(String threadNamePrefix) {
        return createThreadPoolTaskExecutor(
                2,
                8,
                200,
                threadNamePrefix
        );
    }

    /**
     * 创建IO密集型线程池
     * 适用于文件上传、网络请求等IO密集型任务
     *
     * @param threadNamePrefix 线程名前缀
     * @return ThreadPoolTaskExecutor
     */
    protected ThreadPoolTaskExecutor createIOExecutor(String threadNamePrefix) {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                processors * 2,
                processors * 4,
                300,
                threadNamePrefix
        );
    }

    /**
     * 创建CPU密集型线程池
     * 适用于计算密集型任务
     *
     * @param threadNamePrefix 线程名前缀
     * @return ThreadPoolTaskExecutor
     */
    protected ThreadPoolTaskExecutor createCPUExecutor(String threadNamePrefix) {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                processors,
                processors + 1,
                100,
                threadNamePrefix
        );
    }
}
