package com.cloud.payment.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;















@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    




    @Bean("paymentAsyncExecutor")
    public Executor paymentAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "paymentAsyncExecutor",
                3,
                8,
                400,
                60,
                "payment-async-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentCallbackExecutor")
    public Executor paymentCallbackExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "paymentCallbackExecutor",
                processors * 2,
                processors * 4,
                300,
                60,
                "payment-callback-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentNotificationExecutor")
    public Executor paymentNotificationExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "paymentNotificationExecutor",
                2,
                5,
                300,
                60,
                "payment-notification-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentReconciliationExecutor")
    @ConditionalOnProperty(name = "payment.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
    public Executor paymentReconciliationExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "paymentReconciliationExecutor",
                2,
                4,
                200,
                60,
                "payment-reconciliation-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentStatisticsExecutor")
    @ConditionalOnProperty(name = "payment.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor paymentStatisticsExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "paymentStatisticsExecutor",
                processors,
                processors + 1,
                100,
                60,
                "payment-stats-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentLogExecutor")
    public Executor paymentLogExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "paymentLogExecutor",
                1,
                3,
                1000,
                60,
                "payment-log-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentRefundExecutor")
    public Executor paymentRefundExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "paymentRefundExecutor",
                2,
                6,
                250,
                60,
                "payment-refund-"
        );
        executor.initialize();

        
        return executor;
    }

    



    @Bean("paymentCommonAsyncExecutor")
    public Executor paymentCommonAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "paymentCommonAsyncExecutor",
                Math.max(2, processors / 2),
                processors * 2,
                200,
                60,
                "common-async-"
        );
        executor.initialize();

        
        return executor;
    }
}

