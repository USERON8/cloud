package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;








@Data
@Component
@ConfigurationProperties(prefix = "app.async")
public class AsyncProperties {

    


    private boolean enabled = true;

    


    private ThreadPoolConfig defaultExecutor = new ThreadPoolConfig(4, 12, 300, 60);

    


    private ThreadPoolConfig messageExecutor = new ThreadPoolConfig(3, 8, 100, 60);

    


    private ThreadPoolConfig batchExecutor = new ThreadPoolConfig(2, 6, 200, 60);

    


    private ThreadPoolConfig ioExecutor = new ThreadPoolConfig(8, 16, 300, 60);

    


    private ThreadPoolConfig cpuExecutor = new ThreadPoolConfig(4, 5, 100, 60);

    /**
     * Executor overrides keyed by Spring bean name, for example:
     * app.async.executors.searchQueryExecutor.core-pool-size=8
     */
    private Map<String, ThreadPoolConfig> executors = new HashMap<>();

    


    private CommonConfig common = new CommonConfig();

    @Data
    public static class ThreadPoolConfig {
        


        private int corePoolSize;

        


        private int maxPoolSize;

        


        private int queueCapacity;

        


        private int keepAliveSeconds;

        


        private String threadNamePrefix = "async-";

        


        private boolean allowCoreThreadTimeOut = false;

        



        private String rejectedExecutionHandler = "CALLER_RUNS";

        


        private boolean waitForTasksToCompleteOnShutdown = true;

        


        private int awaitTerminationSeconds = 60;

        public ThreadPoolConfig() {
        }

        public ThreadPoolConfig(int corePoolSize, int maxPoolSize, int queueCapacity, int keepAliveSeconds) {
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueCapacity = queueCapacity;
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }

    @Data
    public static class CommonConfig {
        


        private boolean monitoringEnabled = false;

        


        private int monitoringIntervalSeconds = 60;

        


        private boolean preStartCoreThreads = false;

        


        private boolean taskDecorator = false;

        


        private boolean logSlowTasks = true;

        


        private long slowTaskThresholdMs = 5000;
    }
}

