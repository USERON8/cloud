package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;








@Data
@Component
@ConfigurationProperties(prefix = "app.async")
public class DynamicAsyncProperties {

    


    private boolean enabled = true;

    


    private boolean dynamicEnabled = false;

    


    private Map<String, ServiceAsyncConfig> services = new HashMap<>();

    


    private CommonConfig common = new CommonConfig();

    


    private ThreadPoolConfig defaultExecutor = new ThreadPoolConfig(4, 12, 300, 60);

    


    private ThreadPoolConfig messageExecutor = new ThreadPoolConfig(3, 8, 100, 60);

    


    private ThreadPoolConfig batchExecutor = new ThreadPoolConfig(2, 6, 200, 60);

    


    private ThreadPoolConfig ioExecutor = new ThreadPoolConfig(8, 16, 300, 60);

    


    private ThreadPoolConfig cpuExecutor = new ThreadPoolConfig(4, 5, 100, 60);

    @Data
    public static class ServiceAsyncConfig {
        


        private String serviceName;

        


        private boolean enabled = true;

        


        private ThreadPoolConfig query = new ThreadPoolConfig();

        


        private ThreadPoolConfig operation = new ThreadPoolConfig(2, 8, 200, 60);

        


        private ThreadPoolConfig log = new ThreadPoolConfig(2, 4, 800, 60);

        


        private ThreadPoolConfig statistics = new ThreadPoolConfig(2, 4, 500, 60);

        


        private ThreadPoolConfig notification = new ThreadPoolConfig(2, 6, 300, 60);

        


        private Map<String, ThreadPoolConfig> custom = new HashMap<>();
    }

    @Data
    public static class ThreadPoolConfig {
        


        private int corePoolSize = 4;

        


        private int maxPoolSize = 12;

        


        private int queueCapacity = 300;

        


        private int keepAliveSeconds = 60;

        


        private String threadNamePrefix = "async-";

        


        private boolean allowCoreThreadTimeOut = false;

        



        private String rejectedExecutionHandler = "CALLER_RUNS";

        


        private boolean waitForTasksToCompleteOnShutdown = true;

        


        private int awaitTerminationSeconds = 60;

        


        private int threadPriority = 5;

        


        private boolean preStartCoreThreads = false;

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
        


        private boolean monitoringEnabled = true;

        


        private int monitoringIntervalSeconds = 30;

        


        private boolean preStartCoreThreads = false;

        


        private boolean taskDecorator = true;

        


        private boolean logSlowTasks = true;

        


        private long slowTaskThresholdMs = 3000;

        


        private boolean dynamicAdjustmentEnabled = false;

        


        private int dynamicAdjustmentIntervalSeconds = 60;

        


        private double alertThresholdUsageRate = 80.0;

        


        private double alertThresholdQueueRate = 85.0;
    }
}
