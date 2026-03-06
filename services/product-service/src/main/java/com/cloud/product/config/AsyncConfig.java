package com.cloud.product.config;

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

    



    @Bean("productAsyncExecutor")
    public Executor productAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "productAsyncExecutor",
                2,
                4,
                500,
                60,
                "product-async-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("productLogExecutor")
    public Executor productLogExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "productLogExecutor",
                1,
                2,
                1000,
                60,
                "product-log-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("productStatisticsExecutor")
    @ConditionalOnProperty(name = "product.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor productStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "productStatisticsExecutor",
                2,
                4,
                500,
                60,
                "product-statistics-"
        );
        executor.initialize();

        

        return executor;
    }

    



}
