package com.cloud.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 网关异步配置
 * 专门为网关服务配置异步线程池和监控功能
 *
 * 网关服务特点：
 * - 高并发的请求路由
 * - 实时监控和统计
 * - 限流和熔断处理
 * - 日志收集和分析
 *
 * @author what's up
 * @date 2025-01-15
 * @since 2.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(name = "gateway.async.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayAsyncConfig implements AsyncConfigurer {

    /**
     * 网关路由异步线程池
     * 专门用于路由处理相关的异步任务
     */
    @Bean("gatewayRouteExecutor")
    public Executor gatewayRouteExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),     // 核心线程数
                processors * 4,              // 最大线程数
                500,                         // 队列容量
                "gateway-route-"
        );
        executor.initialize();

        log.info("✅ 网关路由线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: 500",
                Math.max(4, processors), processors * 4);
        return executor;
    }

    /**
     * 网关监控线程池
     * 专门用于系统监控任务
     */
    @Bean("gatewayMonitorExecutor")
    public Executor gatewayMonitorExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                3,
                100,
                "gateway-monitor-"
        );
        executor.initialize();

        log.info("✅ 网关监控线程池初始化完成 - 核心线程数: 1, 最大线程数: 3, 队列容量: 100");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：30秒
        executor.setAwaitTerminationSeconds(30);

        // 初始化线程池
        executor.initialize();

        log.info("✅ 网关监控线程池配置完成 - 核心线程数: 1, 最大线程数: 2, 队列容量: 50");

        return executor;
    }

    /**
     * 网关日志线程池
     * 专门用于网关日志处理
     */
    @Bean("gatewayLogExecutor")
    public Executor gatewayLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：2
        executor.setCorePoolSize(2);

        // 最大线程数：4
        executor.setMaxPoolSize(4);

        // 队列容量：1000
        executor.setQueueCapacity(1000);

        // 线程空闲时间：120秒
        executor.setKeepAliveSeconds(120);

        // 线程名前缀
        executor.setThreadNamePrefix("Gateway-Log-");

        // 拒绝策略：丢弃最老的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：60秒
        executor.setAwaitTerminationSeconds(60);

        // 初始化线程池
        executor.initialize();

        log.info("✅ 网关日志线程池配置完成 - 核心线程数: 2, 最大线程数: 4, 队列容量: 1000");

        return executor;
    }

    /**
     * 网关统计线程池
     * 专门用于网关访问统计
     */
    @Bean("gatewayStatisticsExecutor")
    public Executor gatewayStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：1
        executor.setCorePoolSize(1);

        // 最大线程数：3
        executor.setMaxPoolSize(3);

        // 队列容量：2000
        executor.setQueueCapacity(2000);

        // 线程空闲时间：180秒
        executor.setKeepAliveSeconds(180);

        // 线程名前缀
        executor.setThreadNamePrefix("Gateway-Statistics-");

        // 拒绝策略：丢弃最老的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：60秒
        executor.setAwaitTerminationSeconds(60);

        // 初始化线程池
        executor.initialize();

        log.info("✅ 网关统计线程池配置完成 - 核心线程数: 1, 最大线程数: 3, 队列容量: 2000");

        return executor;
    }

    /**
     * 创建线程池任务执行器
     *
     * @param corePoolSize    核心线程数
     * @param maxPoolSize     最大线程数
     * @param queueCapacity   队列容量
     * @param threadNamePrefix 线程名前缀
     * @return ThreadPoolTaskExecutor
     */
    private ThreadPoolTaskExecutor createThreadPoolTaskExecutor(int corePoolSize, int maxPoolSize,
                                                               int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        return executor;
    }

    /**
     * 获取默认异步执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        return gatewayRouteExecutor();
    }
}
