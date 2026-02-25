package com.cloud.stock.config;

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

    



    @Bean("stockQueryExecutor")
    public Executor stockQueryExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "stockQueryExecutor",
                Math.max(8, processors * 2),
                Math.max(16, processors * 6),
                1000,
                60,
                "stock-query-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("stockOperationExecutor")
    public Executor stockOperationExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "stockOperationExecutor",
                Math.max(4, processors),
                Math.max(8, processors * 2),
                300,
                60,
                "stock-operation-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("stockCommonExecutor")
    public Executor stockCommonExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "stockCommonExecutor",
                Math.max(2, processors / 2),
                processors * 2,
                200,
                60,
                "stock-common-async-"
        );
        executor.initialize();

        

        return executor;
    }

}
