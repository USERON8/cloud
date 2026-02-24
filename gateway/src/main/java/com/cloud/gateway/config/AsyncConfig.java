package com.cloud.gateway.config;

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
@ConditionalOnProperty(name = "gateway.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig implements AsyncConfigurer {

    



    @Bean("gatewayRouteExecutor")
    public Executor gatewayRouteExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                Math.max(4, processors),     
                processors * 4,              
                500,                         
                "gateway-route-"
        );
        executor.initialize();

        

        return executor;
    }

    



    @Bean("gatewayMonitorExecutor")
    public Executor gatewayMonitorExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                1,
                3,
                100,
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
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        
        executor.setCorePoolSize(2);

        
        executor.setMaxPoolSize(4);

        
        executor.setQueueCapacity(1000);

        
        executor.setKeepAliveSeconds(120);

        
        executor.setThreadNamePrefix("Gateway-Log-");

        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        
        executor.setWaitForTasksToCompleteOnShutdown(true);

        
        executor.setAwaitTerminationSeconds(60);

        
        executor.initialize();

        

        return executor;
    }

    



    @Bean("gatewayStatisticsExecutor")
    public Executor gatewayStatisticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        
        executor.setCorePoolSize(1);

        
        executor.setMaxPoolSize(3);

        
        executor.setQueueCapacity(2000);

        
        executor.setKeepAliveSeconds(180);

        
        executor.setThreadNamePrefix("Gateway-Statistics-");

        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        
        executor.setWaitForTasksToCompleteOnShutdown(true);

        
        executor.setAwaitTerminationSeconds(60);

        
        executor.initialize();

        

        return executor;
    }

    








    private ThreadPoolTaskExecutor createThreadPoolTaskExecutor(int corePoolSize, int maxPoolSize,
                                                                int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        return executor;
    }

    


    @Override
    public Executor getAsyncExecutor() {
        return gatewayRouteExecutor();
    }
}
