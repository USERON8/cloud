package com.cloud.stock.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 库存服务异步配置类
 * 继承基础异步配置类，提供库存服务专用的线程池配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableAsync
public class StockAsyncConfig extends BaseAsyncConfig {

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
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        log.info("库存服务通用异步线程池初始化完成");
        return executor;
    }
}