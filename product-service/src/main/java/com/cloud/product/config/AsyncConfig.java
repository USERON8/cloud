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
@ConditionalOnProperty(name = "product.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    



    @Bean("productAsyncExecutor")
    public Executor productAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                500,
                "product-async-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("productLogExecutor")
    public Executor productLogExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                2,
                1000,
                "product-log-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("productStatisticsExecutor")
    @ConditionalOnProperty(name = "product.statistics.enabled", havingValue = "true", matchIfMissing = true)
    public Executor productStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                4,
                500,
                "product-statistics-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("productSearchExecutor")
    @ConditionalOnProperty(name = "product.search.enabled", havingValue = "true", matchIfMissing = true)
    public Executor productSearchExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                2,
                300,
                "product-search-"
        );
        executor.initialize();

        

        return executor;
    }
}
