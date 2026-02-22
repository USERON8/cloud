package com.cloud.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;









@Data
@Component
@ConfigurationProperties(prefix = "cloud.distributed-lock")
public class DistributedLockProperties {

    


    private boolean enabled = true;

    


    private long defaultWaitTime = 3;

    


    private long defaultLeaseTime = 10;

    


    private TimeUnit defaultTimeUnit = TimeUnit.SECONDS;

    


    private String keyPrefix = "distributed-lock";

    


    private boolean monitorEnabled = true;

    


    private long monitorInterval = 60;

    


    private boolean logEnabled = true;

    


    private String logLevel = "DEBUG";

    


    private boolean performanceStatsEnabled = false;

    


    private int performanceStatsRetentionHours = 24;

    


    private RedissonProperties redisson = new RedissonProperties();

    


    @Data
    public static class RedissonProperties {

        


        private long lockWatchdogTimeout = 30000;

        


        private int threads = 16;

        


        private int nettyThreads = 32;

        


        private int connectTimeout = 10000;

        


        private int timeout = 3000;

        


        private int retryAttempts = 3;

        


        private int retryInterval = 1500;

        


        private int connectionMinimumIdleSize = 10;

        


        private int connectionPoolSize = 64;

        


        private int idleConnectionTimeout = 10000;
    }
}
