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
@ConditionalOnProperty(name = "payment.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    




    @Bean("paymentAsyncExecutor")
    public Executor paymentAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                8,
                400,
                "payment-async-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentCallbackExecutor")
    public Executor paymentCallbackExecutor() {
        ThreadPoolTaskExecutor executor = createIOExecutor("payment-callback-");
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentNotificationExecutor")
    public Executor paymentNotificationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                5,
                300,
                "payment-notification-"
        );
        executor.initialize();

        
        return executor;
    }

    




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

        
        return executor;
    }

    




    @Bean("paymentStatisticsExecutor")
    @ConditionalOnProperty(name = "payment.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor paymentStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createCPUExecutor("payment-stats-");
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentLogExecutor")
    public Executor paymentLogExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                3,
                1000,
                "payment-log-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("paymentRefundExecutor")
    public Executor paymentRefundExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                250,
                "payment-refund-"
        );
        executor.initialize();

        
        return executor;
    }

    



    @Bean("paymentCommonAsyncExecutor")
    public Executor paymentCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        
        return executor;
    }
}

