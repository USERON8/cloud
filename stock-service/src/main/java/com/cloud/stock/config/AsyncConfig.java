package com.cloud.stock.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 库存服务异步配置类
 * 提供库存服务专用的线程池配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    /**
     * 库存查询专用线程池
     * 根据库存服务的特点进行优化配置
     */
    @Bean("stockQueryExecutor")
    public Executor stockQueryExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(8, processors * 2),
                Math.max(16, processors * 6),
                1000,
                "stock-query-"
        );
        executor.initialize();

        log.info("✅ [STOCK-SERVICE-QUERY] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 库存操作专用线程池
     * 用于处理库存变更等需要保证顺序性的操作
     */
    @Bean("stockOperationExecutor")
    public Executor stockOperationExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),
                Math.max(8, processors * 2),
                300,
                "stock-operation-"
        );
        executor.initialize();

        log.info("✅ [STOCK-SERVICE-OPERATION] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务
     */
    @Bean("stockCommonExecutor")
    public Executor stockCommonExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(2, processors / 2),
                processors * 2,
                200,
                "stock-common-async-"
        );
        executor.initialize();

        log.info("✅ [STOCK-SERVICE-COMMON] 线程池初始化完成 - 核心:{}, 最大:{}, 队列:{}, 存活:{}s",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(),
                executor.getKeepAliveSeconds());
        return executor;
    }

}
