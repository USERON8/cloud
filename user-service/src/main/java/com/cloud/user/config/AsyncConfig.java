package com.cloud.user.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 用户服务异步配置类
 * 继承基础异步配置类，提供用户服务专用的线程池配置
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig extends BaseAsyncConfig {

    /**
     * 用户查询专用线程池
     * 根据用户服务的特点进行优化配置，处理用户信息查询等高并发场景
     */
    @Bean("userQueryExecutor")
    public Executor userQueryExecutor() {
        // 根据用户查询的特点，使用更多核心线程来处理高并发查询
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),
                processors * 4,
                500,
                "user-query-"
        );
        executor.initialize();

        log.info("用户查询线程池初始化完成");
        return executor;
    }

    /**
     * 用户操作专用线程池
     * 用于处理用户注册、更新等需要保证数据一致性的操作
     */
    @Bean("userOperationExecutor")
    public Executor userOperationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                10,
                100,
                "user-operation-"
        );
        executor.initialize();

        log.info("用户操作线程池初始化完成");
        return executor;
    }

    /**
     * 用户日志异步线程池
     * 专门用于用户日志处理
     */
    @Bean("userLogExecutor")
    public Executor userLogExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                2,
                800,
                "user-log-"
        );
        executor.initialize();

        log.info("用户日志线程池初始化完成");
        return executor;
    }

    /**
     * 用户通知异步线程池
     * 专门用于用户通知发送
     */
    @Bean("userNotificationExecutor")
    public Executor userNotificationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                200,
                "user-notification-"
        );
        executor.initialize();

        log.info("用户通知线程池初始化完成");
        return executor;
    }

    /**
     * 用户统计异步线程池
     * 专门用于用户行为统计
     */
    @Bean("userStatisticsExecutor")
    public Executor userStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                2,
                1500,
                "user-statistics-"
        );
        executor.initialize();

        log.info("用户统计线程池初始化完成");
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务，如发送邮件、日志记录等
     */
    @Bean("userCommonAsyncExecutor")
    public Executor userCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        log.info("用户服务通用异步线程池初始化完成");
        return executor;
    }
}