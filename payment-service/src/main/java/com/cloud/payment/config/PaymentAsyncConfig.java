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
 * 
 * 支付服务特点：
 * - 高安全性的支付处理
 * - 第三方支付接口调用
 * - 支付状态同步
 * - 支付数据统计分析
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
     * 支付处理异步线程池
     * 专门用于支付相关的异步业务处理
     * 保证支付处理的高可靠性
     */
    @Bean("paymentProcessExecutor")
    public Executor paymentProcessExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                8,
                200,
                "payment-process-"
        );
        executor.initialize();

        log.info("✅ 支付处理线程池初始化完成 - 核心线程数: 3, 最大线程数: 8, 队列容量: 200");
        return executor;
    }

    /**
     * 第三方支付接口异步线程池
     * 专门用于调用第三方支付接口
     * IO密集型任务优化，处理网络请求
     */
    @Bean("paymentThirdPartyExecutor")
    public Executor paymentThirdPartyExecutor() {
        ThreadPoolTaskExecutor executor = createIOExecutor("payment-3rd-");
        executor.initialize();

        log.info("✅ 第三方支付接口线程池初始化完成");
        return executor;
    }

    /**
     * 支付状态同步异步线程池
     * 专门用于支付状态同步和回调处理
     * 高并发状态更新优化
     */
    @Bean("paymentSyncExecutor")
    public Executor paymentSyncExecutor() {
        ThreadPoolTaskExecutor executor = createQueryExecutor("payment-sync-");
        executor.initialize();

        log.info("✅ 支付状态同步线程池初始化完成");
        return executor;
    }

    /**
     * 支付通知异步线程池
     * 专门用于支付通知和消息发送
     * 保证通知的及时性和可靠性
     */
    @Bean("paymentNotifyExecutor")
    public Executor paymentNotifyExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                300,
                "payment-notify-"
        );
        executor.initialize();

        log.info("✅ 支付通知线程池初始化完成 - 核心线程数: 2, 最大线程数: 6, 队列容量: 300");
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
     * 支付对账异步线程池
     * 专门用于支付对账和清算任务
     * 批量处理优化
     */
    @Bean("paymentReconciliationExecutor")
    @ConditionalOnProperty(name = "payment.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
    public Executor paymentReconciliationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                3,
                100,
                "payment-recon-"
        );
        executor.initialize();

        log.info("✅ 支付对账线程池初始化完成 - 核心线程数: 1, 最大线程数: 3, 队列容量: 100");
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
