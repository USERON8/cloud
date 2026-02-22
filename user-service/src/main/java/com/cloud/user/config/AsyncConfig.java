package com.cloud.user.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    /**
     * 用户查询专用线程池
     * 根据用户服务的特点进行优化配置，处理用户信息查询等高并发场景
     */
    @Bean("userQueryExecutor")
    public Executor userQueryExecutor() {
        ThreadPoolTaskExecutor executor = createQueryExecutor("user-query-");
        executor.initialize();

        log.info("✅ [USER-SERVICE-QUERY] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 用户操作专用线程池
     * 用于处理用户注册、更新等需要保证数据一致性的操作
     */
    @Bean("userOperationExecutor")
    public Executor userOperationExecutor() {
        ThreadPoolTaskExecutor executor = createWriteExecutor("user-operation-");
        executor.initialize();

        log.info("✅ [USER-SERVICE-OPERATION] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 用户日志异步线程池
     * 专门用于用户日志处理
     */
    @Bean("userLogExecutor")
    public Executor userLogExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(2, processors / 2),
                Math.max(4, processors),
                1200,
                "user-log-"
        );
        executor.initialize();

        log.info("✅ [USER-SERVICE-LOG] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 用户通知异步线程池
     * 专门用于用户通知发送
     */
    @Bean("userNotificationExecutor")
    public Executor userNotificationExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),
                Math.max(8, processors * 2),
                800,
                "user-notification-"
        );
        executor.initialize();

        log.info("✅ [USER-SERVICE-NOTIFICATION] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 用户统计异步线程池
     * 专门用于用户行为统计
     */
    @Bean("userStatisticsExecutor")
    public Executor userStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createCPUExecutor("user-statistics-");
        executor.initialize();

        log.info("✅ [USER-SERVICE-STATISTICS] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
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
