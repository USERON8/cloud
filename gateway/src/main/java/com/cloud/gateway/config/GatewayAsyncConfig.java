package com.cloud.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 网关异步配置
 * 专门为网关服务配置异步线程池和监控功能
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class GatewayAsyncConfig {

    /**
     * 网关监控线程池
     * 专门用于系统监控任务
     */
    @Bean("gatewayMonitorExecutor")
    public Executor gatewayMonitorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：1
        executor.setCorePoolSize(1);

        // 最大线程数：2
        executor.setMaxPoolSize(2);

        // 队列容量：50
        executor.setQueueCapacity(50);

        // 线程空闲时间：300秒
        executor.setKeepAliveSeconds(300);

        // 线程名前缀
        executor.setThreadNamePrefix("Gateway-Monitor-");

        // 拒绝策略：调用者运行策略
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
}
