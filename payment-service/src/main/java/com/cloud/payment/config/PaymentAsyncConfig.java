package com.cloud.payment.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 支付服务异步配置类
 * 继承基础异步配置类，提供支付服务专用的线程池配置
 *
 * @author what's up
 */
@Slf4j
@Configuration
@EnableAsync
public class PaymentAsyncConfig extends BaseAsyncConfig {

    /**
     * 支付查询专用线程池
     * 根据支付查询的特点进行优化配置
     */
    @Bean("paymentQueryExecutor")
    public Executor paymentQueryExecutor() {
        // 支付查询通常需要快速响应，使用较多的核心线程
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),
                processors * 3,
                300,
                "payment-query-"
        );
        executor.initialize();

        log.info("支付查询线程池初始化完成");
        return executor;
    }

    /**
     * 支付操作专用线程池
     * 用于处理支付创建、更新等核心操作
     */
    @Bean("paymentOperationExecutor")
    public Executor paymentOperationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                12,
                150,
                "payment-operation-"
        );
        executor.initialize();

        log.info("支付操作线程池初始化完成");
        return executor;
    }

    /**
     * 支付回调专用线程池
     * 用于处理第三方支付平台的异步回调
     */
    @Bean("paymentCallbackExecutor")
    public Executor paymentCallbackExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                8,
                100,
                "payment-callback-"
        );
        executor.initialize();

        log.info("支付回调线程池初始化完成");
        return executor;
    }

    /**
     * 支付通知专用线程池
     * 用于发送支付成功/失败通知
     */
    @Bean("paymentNotificationExecutor")
    public Executor paymentNotificationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                80,
                "payment-notification-"
        );
        executor.initialize();

        log.info("支付通知线程池初始化完成");
        return executor;
    }

    /**
     * 通用异步线程池
     * 用于处理其他异步任务
     */
    @Bean("paymentCommonAsyncExecutor")
    public Executor paymentCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        log.info("支付服务通用异步线程池初始化完成");
        return executor;
    }
}
