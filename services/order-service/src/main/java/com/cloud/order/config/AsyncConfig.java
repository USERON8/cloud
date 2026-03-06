package com.cloud.order.config;

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

    



    @Bean("orderAsyncExecutor")
    public Executor orderAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "orderAsyncExecutor",
                4,
                8,
                400,
                60,
                "order-async-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("orderLogExecutor")
    public Executor orderLogExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "orderLogExecutor",
                1,
                3,
                1000,
                60,
                "order-log-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("orderNotificationExecutor")
    public Executor orderNotificationExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "orderNotificationExecutor",
                2,
                4,
                300,
                60,
                "order-notification-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("orderStatisticsExecutor")
    @ConditionalOnProperty(name = "order.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor orderStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "orderStatisticsExecutor",
                1,
                2,
                1200,
                60,
                "order-statistics-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("orderPaymentExecutor")
    public Executor orderPaymentExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "orderPaymentExecutor",
                4,
                16,
                500,
                60,
                "order-payment-"
        );
        executor.initialize();

        

        return executor;
    }

}
