package com.cloud.user.config;

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

    



    @Bean("userQueryExecutor")
    public Executor userQueryExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "userQueryExecutor",
                Math.max(4, processors),
                processors * 4,
                500,
                60,
                "user-query-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("userOperationExecutor")
    public Executor userOperationExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "userOperationExecutor",
                4,
                16,
                500,
                60,
                "user-operation-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("userNotificationExecutor")
    public Executor userNotificationExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "userNotificationExecutor",
                Math.max(4, processors),
                Math.max(8, processors * 2),
                800,
                60,
                "user-notification-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("userStatisticsExecutor")
    public Executor userStatisticsExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "userStatisticsExecutor",
                processors,
                processors + 1,
                100,
                60,
                "user-statistics-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("userCommonAsyncExecutor")
    public Executor userCommonAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "userCommonAsyncExecutor",
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
