package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 异步配置属性类
 * 支持线程池配置的外部化
 *
 * @author cloud
 * @date 2025-01-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.async")
public class AsyncProperties {

    /**
     * 是否启用异步功能
     */
    private boolean enabled = true;

    /**
     * 默认线程池配置
     */
    private ThreadPoolConfig defaultExecutor = new ThreadPoolConfig(4, 12, 300, 60);

    /**
     * 消息线程池配置
     */
    private ThreadPoolConfig messageExecutor = new ThreadPoolConfig(3, 8, 100, 60);

    /**
     * 批处理线程池配置
     */
    private ThreadPoolConfig batchExecutor = new ThreadPoolConfig(2, 6, 200, 60);

    /**
     * IO密集型线程池配置
     */
    private ThreadPoolConfig ioExecutor = new ThreadPoolConfig(8, 16, 300, 60);

    /**
     * CPU密集型线程池配置
     */
    private ThreadPoolConfig cpuExecutor = new ThreadPoolConfig(4, 5, 100, 60);

    /**
     * 通用配置
     */
    private CommonConfig common = new CommonConfig();

    @Data
    public static class ThreadPoolConfig {
        /**
         * 核心线程数
         */
        private int corePoolSize;

        /**
         * 最大线程数
         */
        private int maxPoolSize;

        /**
         * 队列容量
         */
        private int queueCapacity;

        /**
         * 空闲线程存活时间（秒）
         */
        private int keepAliveSeconds;

        /**
         * 线程名前缀
         */
        private String threadNamePrefix = "async-";

        /**
         * 是否允许核心线程超时
         */
        private boolean allowCoreThreadTimeOut = false;

        /**
         * 拒绝策略
         * 可选值: CALLER_RUNS, ABORT, DISCARD, DISCARD_OLDEST
         */
        private String rejectedExecutionHandler = "CALLER_RUNS";

        /**
         * 是否等待任务完成后再关闭
         */
        private boolean waitForTasksToCompleteOnShutdown = true;

        /**
         * 等待终止时间（秒）
         */
        private int awaitTerminationSeconds = 60;

        public ThreadPoolConfig() {
        }

        public ThreadPoolConfig(int corePoolSize, int maxPoolSize, int queueCapacity, int keepAliveSeconds) {
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueCapacity = queueCapacity;
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }

    @Data
    public static class CommonConfig {
        /**
         * 是否启用线程池监控
         */
        private boolean monitoringEnabled = false;

        /**
         * 线程池监控间隔（秒）
         */
        private int monitoringIntervalSeconds = 60;

        /**
         * 是否启用线程池预热
         */
        private boolean preStartCoreThreads = false;

        /**
         * 任务装饰器
         */
        private boolean taskDecorator = false;

        /**
         * 是否记录慢任务
         */
        private boolean logSlowTasks = true;

        /**
         * 慢任务阈值（毫秒）
         */
        private long slowTaskThresholdMs = 5000;
    }
}

