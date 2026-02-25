package com.cloud.gateway.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig implements AsyncConfigurer {

    @Bean("gatewayRouteExecutor")
    public Executor gatewayRouteExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "gatewayRouteExecutor",
                Math.max(4, processors),
                processors * 4,
                500,
                60,
                "gateway-route-"
        );
        executor.initialize();
        return executor;
    }

    @Bean("gatewayMonitorExecutor")
    public Executor gatewayMonitorExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "gatewayMonitorExecutor",
                1,
                3,
                100,
                60,
                "gateway-monitor-"
        );
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean("gatewayLogExecutor")
    public Executor gatewayLogExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "gatewayLogExecutor",
                2,
                4,
                1000,
                120,
                "gateway-log-",
                e -> e.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
        );
        executor.initialize();
        return executor;
    }

    @Bean("gatewayStatisticsExecutor")
    public Executor gatewayStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "gatewayStatisticsExecutor",
                1,
                3,
                2000,
                180,
                "gateway-statistics-",
                e -> e.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
        );
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return gatewayRouteExecutor();
    }
}
