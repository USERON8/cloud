package com.cloud.order.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 订单服务异步配置类
 * 继承基础异步配置类，提供订单服务专用的线程池配置
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "order.async.enabled", havingValue = "true", matchIfMissing = true)
public class OrderAsyncConfig extends BaseAsyncConfig {

    /**
     * 订单业务异步线程池
     * 专门用于订单相关的异步业务处理
     */
    @Bean("orderAsyncExecutor")
    public Executor orderAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                6,
                400,
                "order-async-"
        );
        executor.initialize();

        log.info("订单异步线程池初始化完成");
        return executor;
    }

    /**
     * 订单日志异步线程池
     * 专门用于订单日志处理
     */
    @Bean("orderLogExecutor")
    public Executor orderLogExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                3,
                1000,
                "order-log-"
        );
        executor.initialize();

        log.info("订单日志线程池初始化完成");
        return executor;
    }

    /**
     * 订单通知异步线程池
     * 专门用于订单通知发送
     */
    @Bean("orderNotificationExecutor")
    public Executor orderNotificationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                300,
                "order-notification-"
        );
        executor.initialize();

        log.info("订单通知线程池初始化完成");
        return executor;
    }

    /**
     * 订单统计异步线程池
     * 专门用于订单统计数据处理
     */
    @Bean("orderStatisticsExecutor")
    @ConditionalOnProperty(name = "order.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor orderStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                2,
                1200,
                "order-statistics-"
        );
        executor.initialize();

        log.info("订单统计线程池初始化完成");
        return executor;
    }

    /**
     * 订单支付异步线程池
     * 专门用于订单支付相关的异步处理
     */
    @Bean("orderPaymentExecutor")
    public Executor orderPaymentExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                5,
                200,
                "order-payment-"
        );
        executor.initialize();

        log.info("订单支付线程池初始化完成");
        return executor;
    }
}
