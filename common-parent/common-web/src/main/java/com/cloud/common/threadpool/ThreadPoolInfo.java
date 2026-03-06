package com.cloud.common.threadpool;

import lombok.Data;








@Data
public class ThreadPoolInfo {

    


    private String beanName;

    


    private int corePoolSize;

    


    private int maximumPoolSize;

    


    private int currentPoolSize;

    


    private int activeThreadCount;

    


    private int queueSize;

    


    private int queueCapacity;

    


    private long completedTaskCount;

    


    private long totalTaskCount;

    


    private long keepAliveTime;

    


    private String rejectedExecutionHandler;

    


    private double poolUsageRate;

    


    private double queueUsageRate;

    


    private String status;

    


    private long timestamp;

    


    public long getPendingTaskCount() {
        return totalTaskCount - completedTaskCount - activeThreadCount;
    }

    


    public double getTaskCompletionRate() {
        if (totalTaskCount == 0) {
            return 100.0;
        }
        return (double) completedTaskCount / totalTaskCount * 100;
    }

    


    public boolean isHealthy() {
        return "HEALTHY".equals(status);
    }

    


    public boolean isWarning() {
        return "WARNING".equals(status);
    }

    


    public boolean isCritical() {
        return "CRITICAL".equals(status);
    }
}
