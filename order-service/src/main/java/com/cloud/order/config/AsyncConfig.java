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
@ConditionalOnProperty(name = "order.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    



    @Bean("orderAsyncExecutor")
    public Executor orderAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                4,
                8,
                400,
                "order-async-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("orderLogExecutor")
    public Executor orderLogExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                3,
                1000,
                "order-log-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("orderNotificationExecutor")
    public Executor orderNotificationExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                300,
                "order-notification-"
        );
        executor.initialize();

        

        return executor;
    }

    



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

        

        return executor;
    }

    



    @Bean("orderPaymentExecutor")
    public Executor orderPaymentExecutor() {
        ThreadPoolTaskExecutor executor = createWriteExecutor("order-payment-");
        executor.initialize();

        

        return executor;
    }

}
