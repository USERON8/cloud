package com.cloud.common.config;

import com.cloud.common.config.properties.AsyncProperties;
import com.cloud.common.threadpool.ContextAwareTaskDecorator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;











@Slf4j
public class BaseAsyncConfig {

    @Autowired(required = false)
    protected AsyncProperties asyncProperties;

    @Autowired(required = false)
    protected ContextAwareTaskDecorator taskDecorator;

    





    protected Executor createDefaultAsyncExecutor() {
        ThreadPoolTaskExecutor executor;
        if (asyncProperties != null) {
            AsyncProperties.ThreadPoolConfig config = asyncProperties.getDefaultExecutor();
            executor = createThreadPoolTaskExecutorFromConfig(config, "async-default-");
        } else {
            int processors = Runtime.getRuntime().availableProcessors();
            executor = createThreadPoolTaskExecutor(
                    Math.max(4, processors),
                    processors * 3,
                    300,
                    "async-default-"
            );
        }
        executor.initialize();
        

        return executor;
    }

    





    protected Executor createAsyncMessageExecutor() {
        ThreadPoolTaskExecutor executor;
        if (asyncProperties != null) {
            AsyncProperties.ThreadPoolConfig config = asyncProperties.getMessageExecutor();
            executor = createThreadPoolTaskExecutorFromConfig(config, "async-message-");
        } else {
            executor = createThreadPoolTaskExecutor(3, 8, 100, "async-message-");
        }
        executor.initialize();
        

        return executor;
    }

    





    protected Executor createBatchProcessExecutor() {
        ThreadPoolTaskExecutor executor;
        if (asyncProperties != null) {
            AsyncProperties.ThreadPoolConfig config = asyncProperties.getBatchExecutor();
            executor = createThreadPoolTaskExecutorFromConfig(config, "batch-process-");
        } else {
            executor = createThreadPoolTaskExecutor(2, 6, 200, "batch-process-");
        }
        executor.initialize();
        

        return executor;
    }

    









    protected ThreadPoolTaskExecutor createThreadPoolTaskExecutor(int corePoolSize, int maxPoolSize,
                                                                  int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(false);

        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        
        if (taskDecorator != null && shouldUseTaskDecorator()) {
            executor.setTaskDecorator(taskDecorator);
        }

        return executor;
    }

    


    protected ThreadPoolTaskExecutor createThreadPoolTaskExecutorFromConfig(
            AsyncProperties.ThreadPoolConfig config, String defaultThreadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        String prefix = config.getThreadNamePrefix();
        executor.setThreadNamePrefix(prefix != null && !prefix.isEmpty() ? prefix : defaultThreadNamePrefix);

        
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setAllowCoreThreadTimeOut(config.isAllowCoreThreadTimeOut());

        
        executor.setRejectedExecutionHandler(getRejectedExecutionHandler(config.getRejectedExecutionHandler()));

        
        executor.setWaitForTasksToCompleteOnShutdown(config.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(config.getAwaitTerminationSeconds());

        return executor;
    }

    


    private RejectedExecutionHandler getRejectedExecutionHandler(String handlerType) {
        return switch (handlerType.toUpperCase()) {
            case "ABORT" -> new ThreadPoolExecutor.AbortPolicy();
            case "DISCARD" -> new ThreadPoolExecutor.DiscardPolicy();
            case "DISCARD_OLDEST" -> new ThreadPoolExecutor.DiscardOldestPolicy();
            default -> new ThreadPoolExecutor.CallerRunsPolicy();
        };
    }

    





    protected ThreadPoolTaskExecutor createCommonAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                Math.max(2, processors / 2),
                processors * 2,
                200,
                "common-async-"
        );
    }

    






    protected ThreadPoolTaskExecutor createQueryExecutor(String threadNamePrefix) {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                Math.max(4, processors),
                processors * 4,
                500,
                threadNamePrefix
        );
    }

    






    protected ThreadPoolTaskExecutor createWriteExecutor(String threadNamePrefix) {
        return createThreadPoolTaskExecutor(
                2,
                8,
                200,
                threadNamePrefix
        );
    }

    






    protected ThreadPoolTaskExecutor createIOExecutor(String threadNamePrefix) {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                processors * 2,
                processors * 4,
                300,
                threadNamePrefix
        );
    }

    






    protected ThreadPoolTaskExecutor createCPUExecutor(String threadNamePrefix) {
        int processors = Runtime.getRuntime().availableProcessors();
        return createThreadPoolTaskExecutor(
                processors,
                processors + 1,
                100,
                threadNamePrefix
        );
    }

    


    protected boolean shouldUseTaskDecorator() {
        if (asyncProperties != null && asyncProperties.getCommon() != null) {
            return asyncProperties.getCommon().isTaskDecorator();
        }
        return true; 
    }
}
