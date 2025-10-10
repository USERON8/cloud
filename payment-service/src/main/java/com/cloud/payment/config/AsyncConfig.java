package com.cloud.payment.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 支付服务异步配置类
 * 继承基础异步配置类，提供支付服务专用的线程池配置
 * <p>
 * 支付服务特点：
 * - 高安全性要求的支付处理
 * - 支付回调处理
 * - 支付订单状态更新
 * - 支付通知发送
 * - 支付数据统计
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "payment.async.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentAsyncConfig extends BaseAsyncConfig {

    /**
     * 支付业务异步线程池
     * 专门用于支付相关的异步业务处理
     * 如：支付订单创建、支付状态更新等
     */
    @Bean("paymentAsyncExecutor")
    public Executor paymentAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                8,
                400,
                "payment-async-"
        );
        executor.initialize();

        log.info("✅ 支付异步线程池初始化完成 - 核心线程数: 3, 最大线程数: 8, 队列容量: 400");
        return executor;
    }

    /**
     * 支付回调异步线程池
     * 专门用于处理第三方支付平台的回调通知
     * 高并发回调处理优化
     */
    @Bean("paymentCallbackExecutor")
    public Executor paymentCallbackExecutor() {
        ThreadPoolTaskExecutor executor = createIOExecutor("payment-callback-");
        executor.initialize();

        log.info("✅ 支付回调线程池初始化完成");
        return executor;
    }

    /**
     * 支付通知异步线程池
     * 专门用于支付结果通知发送
     * 如：发送用户支付成功通知、订单状态更新通知等
     */
    @Bean("paymentNotificationExecutor")
    public Executor paymentNotificationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                5,
                300,
                "payment-notification-"
        );
        executor.initialize();

        log.info("✅ 支付通知线程池初始化完成 - 核心线程数: 2, 最大线程数: 5, 队列容量: 300");
        return executor;
    }

    /**
     * 支付对账异步线程池
     * 专门用于支付对账任务
     * 定时任务和批量对账处理
     */
    @Bean("paymentReconciliationExecutor")
    @ConditionalOnProperty(name = "payment.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
    public Executor paymentReconciliationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                200,
                "payment-reconciliation-"
        );
        executor.initialize();

        log.info("✅ 支付对账线程池初始化完成 - 核心线程数: 2, 最大线程数: 4, 队列容量: 200");
        return executor;
    }

    /**
     * 支付统计异步线程池
     * 专门用于支付数据统计和分析
     * CPU密集型任务优化
     */
    @Bean("paymentStatisticsExecutor")
    @ConditionalOnProperty(name = "payment.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor paymentStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createCPUExecutor("payment-stats-");
        executor.initialize();

        log.info("✅ 支付统计线程池初始化完成");
        return executor;
    }

    /**
     * 支付日志异步线程池
     * 专门用于支付日志记录
     * 大容量队列支持高并发日志写入
     */
    @Bean("paymentLogExecutor")
    public Executor paymentLogExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                3,
                1000,
                "payment-log-"
        );
        executor.initialize();

        log.info("✅ 支付日志线程池初始化完成 - 核心线程数: 1, 最大线程数: 3, 队列容量: 1000");
        return executor;
    }

    /**
     * 支付退款异步线程池
     * 专门用于退款相关的异步处理
     * 如：退款申请、退款审核、退款通知等
     */
    @Bean("paymentRefundExecutor")
    public Executor paymentRefundExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                250,
                "payment-refund-"
        );
        executor.initialize();

        log.info("✅ 支付退款线程池初始化完成 - 核心线程数: 2, 最大线程数: 6, 队列容量: 250");
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

        log.info("✅ 支付服务通用异步线程池初始化完成");
        return executor;
    }
}

