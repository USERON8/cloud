package com.cloud.common.config;

import com.cloud.common.config.properties.AsyncProperties;
import com.cloud.common.threadpool.ContextAwareTaskDecorator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步处理基础配置类
 * 提供通用的线程池配置模板和工厂方法
 * 各服务可以继承此类并根据业务特点定制线程池
 * 注意：此类仅用于继承，不作为Spring Bean
 *
 * @author cloud
 * @version 2.0
 * @date 2024-01-20
 */
@Slf4j
public class BaseAsyncConfig {

    @Autowired(required = false)
    protected AsyncProperties asyncProperties;

    @Autowired(required = false)
    protected ContextAwareTaskDecorator taskDecorator;

    /**
     * 默认异步任务执行器工厂方法
     * 适用于通用异步任务处理
     *
     * @return Executor
     */
    protected Executor createDefaultAsyncExecutor() {
        ThreadPoolTaskExecutor executor;
        if (asyncProperties != null) {
            AsyncProperties.ThreadPoolConfig config = asyncProperties.getDefaultExecutor();
            executor = createThreadPoolTaskExecutorFromConfig(config, "async-default-");
        } else {
            int processors = Runtime.getRuntime().availableProcessors();
            executor = createThreadPoolTaskExecutor(
                    Math.max(4, processors),
                    processors * 3,
                    300,
                    "async-default-"
            );
        }
        executor.initialize();
        log.info("✅ [DEFAULT-ASYNC] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 消息发送异步任务执行器工厂方法
     * 专门用于消息发送相关的异步任务
     *
     * @return Executor
     */
    protected Executor createAsyncMessageExecutor() {
        ThreadPoolTaskExecutor executor;
        if (asyncProperties != null) {
            AsyncProperties.ThreadPoolConfig config = asyncProperties.getMessageExecutor();
            executor = createThreadPoolTaskExecutorFromConfig(config, "async-message-");
        } else {
            executor = createThreadPoolTaskExecutor(3, 8, 100, "async-message-");
        }
        executor.initialize();
        log.info("✅ [MESSAGE-ASYNC] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 批处理异步任务执行器工厂方法
     * 专门用于批量消息处理相关的异步任务
     *
     * @return Executor
     */
    protected Executor createBatchProcessExecutor() {
        ThreadPoolTaskExecutor executor;
        if (asyncProperties != null) {
            AsyncProperties.ThreadPoolConfig config = asyncProperties.getBatchExecutor();
            executor = createThreadPoolTaskExecutorFromConfig(config, "batch-process-");
        } else {
            executor = createThreadPoolTaskExecutor(2, 6, 200, "batch-process-");
        }
        executor.initialize();
        log.info("✅ [BATCH-ASYNC] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
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

        // 设置任务装饰器（如果可用）
        if (taskDecorator != null && shouldUseTaskDecorator()) {
            executor.setTaskDecorator(taskDecorator);
        }

        return executor;
    }

    /**
     * 从配置对象创建线程池
     */
    protected ThreadPoolTaskExecutor createThreadPoolTaskExecutorFromConfig(
            AsyncProperties.ThreadPoolConfig config, String defaultThreadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 基本配置
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        String prefix = config.getThreadNamePrefix();
        executor.setThreadNamePrefix(prefix != null && !prefix.isEmpty() ? prefix : defaultThreadNamePrefix);

        // 高级配置
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setAllowCoreThreadTimeOut(config.isAllowCoreThreadTimeOut());

        // 拒绝策略
        executor.setRejectedExecutionHandler(getRejectedExecutionHandler(config.getRejectedExecutionHandler()));

        // 优雅关闭配置
        executor.setWaitForTasksToCompleteOnShutdown(config.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(config.getAwaitTerminationSeconds());

        return executor;
    }

    /**
     * 获取拒绝策略
     */
    private RejectedExecutionHandler getRejectedExecutionHandler(String handlerType) {
        return switch (handlerType.toUpperCase()) {
            case "ABORT" -> new ThreadPoolExecutor.AbortPolicy();
            case "DISCARD" -> new ThreadPoolExecutor.DiscardPolicy();
            case "DISCARD_OLDEST" -> new ThreadPoolExecutor.DiscardOldestPolicy();
            default -> new ThreadPoolExecutor.CallerRunsPolicy();
        };
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

    /**
     * 判断是否应该使用任务装饰器
     */
    protected boolean shouldUseTaskDecorator() {
        if (asyncProperties != null && asyncProperties.getCommon() != null) {
            return asyncProperties.getCommon().isTaskDecorator();
        }
        return true; // 默认启用
    }
}
