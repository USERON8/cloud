package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态异步配置属性类
 * 支持运行时调整线程池参数
 *
 * @author cloud
 * @date 2025-01-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.async")
public class DynamicAsyncProperties {

    /**
     * 是否启用异步功能
     */
    private boolean enabled = true;

    /**
     * 是否启用动态配置
     */
    private boolean dynamicEnabled = false;

    /**
     * 服务特定的线程池配置
     */
    private Map<String, ServiceAsyncConfig> services = new HashMap<>();

    /**
     * 通用配置
     */
    private CommonConfig common = new CommonConfig();

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

    @Data
    public static class ServiceAsyncConfig {
        /**
         * 服务名称
         */
        private String serviceName;

        /**
         * 是否启用异步
         */
        private boolean enabled = true;

        /**
         * 查询线程池配置
         */
        private ThreadPoolConfig query = new ThreadPoolConfig();

        /**
         * 操作线程池配置
         */
        private ThreadPoolConfig operation = new ThreadPoolConfig(2, 8, 200, 60);

        /**
         * 日志线程池配置
         */
        private ThreadPoolConfig log = new ThreadPoolConfig(2, 4, 800, 60);

        /**
         * 统计线程池配置
         */
        private ThreadPoolConfig statistics = new ThreadPoolConfig(2, 4, 500, 60);

        /**
         * 通知线程池配置
         */
        private ThreadPoolConfig notification = new ThreadPoolConfig(2, 6, 300, 60);

        /**
         * 自定义线程池配置
         */
        private Map<String, ThreadPoolConfig> custom = new HashMap<>();
    }

    @Data
    public static class ThreadPoolConfig {
        /**
         * 核心线程数
         */
        private int corePoolSize = 4;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 12;

        /**
         * 队列容量
         */
        private int queueCapacity = 300;

        /**
         * 空闲线程存活时间（秒）
         */
        private int keepAliveSeconds = 60;

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

        /**
         * 线程优先级 (1-10)
         */
        private int threadPriority = 5;

        /**
         * 是否启用线程池预热
         */
        private boolean preStartCoreThreads = false;

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
        private boolean monitoringEnabled = true;

        /**
         * 线程池监控间隔（秒）
         */
        private int monitoringIntervalSeconds = 30;

        /**
         * 是否启用线程池预热
         */
        private boolean preStartCoreThreads = false;

        /**
         * 是否启用任务装饰器
         */
        private boolean taskDecorator = true;

        /**
         * 是否记录慢任务
         */
        private boolean logSlowTasks = true;

        /**
         * 慢任务阈值（毫秒）
         */
        private long slowTaskThresholdMs = 3000;

        /**
         * 是否启用动态调整
         */
        private boolean dynamicAdjustmentEnabled = false;

        /**
         * 动态调整检查间隔（秒）
         */
        private int dynamicAdjustmentIntervalSeconds = 60;

        /**
         * 线程池使用率告警阈值（百分比）
         */
        private double alertThresholdUsageRate = 80.0;

        /**
         * 队列使用率告警阈值（百分比）
         */
        private double alertThresholdQueueRate = 85.0;
    }
}