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
        ThreadPoolTaskExecutor executor = createQueryExecutor("user-query-");
        executor.initialize();

        

        return executor;
    }

    



    @Bean("userOperationExecutor")
    public Executor userOperationExecutor() {
        ThreadPoolTaskExecutor executor = createWriteExecutor("user-operation-");
        executor.initialize();

        

        return executor;
    }

    



    @Bean("userLogExecutor")
    public Executor userLogExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(2, processors / 2),
                Math.max(4, processors),
                1200,
                "user-log-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("userNotificationExecutor")
    public Executor userNotificationExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),
                Math.max(8, processors * 2),
                800,
                "user-notification-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("userStatisticsExecutor")
    public Executor userStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createCPUExecutor("user-statistics-");
        executor.initialize();

        

        return executor;
    }

    



    @Bean("userCommonAsyncExecutor")
    public Executor userCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        
        return executor;
    }
}
