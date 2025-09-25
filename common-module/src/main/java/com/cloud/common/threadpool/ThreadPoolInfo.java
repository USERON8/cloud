package com.cloud.common.threadpool;

import lombok.Data;

/**
 * 线程池信息数据类
 * 封装线程池的状态和性能指标
 *
 * @author what's up
 * @since 1.0.0
 */
@Data
public class ThreadPoolInfo {
    
    /**
     * 线程池Bean名称
     */
    private String beanName;
    
    /**
     * 核心线程数
     */
    private int corePoolSize;
    
    /**
     * 最大线程数
     */
    private int maximumPoolSize;
    
    /**
     * 当前线程池大小
     */
    private int currentPoolSize;
    
    /**
     * 活跃线程数
     */
    private int activeThreadCount;
    
    /**
     * 队列中等待的任务数
     */
    private int queueSize;
    
    /**
     * 队列容量
     */
    private int queueCapacity;
    
    /**
     * 已完成的任务数
     */
    private long completedTaskCount;
    
    /**
     * 总任务数（包括已完成、正在执行、等待执行的）
     */
    private long totalTaskCount;
    
    /**
     * 线程空闲时间（秒）
     */
    private long keepAliveTime;
    
    /**
     * 拒绝策略类名
     */
    private String rejectedExecutionHandler;
    
    /**
     * 线程池使用率（百分比）
     */
    private double poolUsageRate;
    
    /**
     * 队列使用率（百分比）
     */
    private double queueUsageRate;
    
    /**
     * 线程池状态：HEALTHY, WARNING, CRITICAL
     */
    private String status;
    
    /**
     * 获取等待执行的任务数
     */
    public long getPendingTaskCount() {
        return totalTaskCount - completedTaskCount - activeThreadCount;
    }
    
    /**
     * 获取任务完成率（百分比）
     */
    public double getTaskCompletionRate() {
        if (totalTaskCount == 0) {
            return 100.0;
        }
        return (double) completedTaskCount / totalTaskCount * 100;
    }
    
    /**
     * 判断线程池是否健康
     */
    public boolean isHealthy() {
        return "HEALTHY".equals(status);
    }
    
    /**
     * 判断线程池是否处于警告状态
     */
    public boolean isWarning() {
        return "WARNING".equals(status);
    }
    
    /**
     * 判断线程池是否处于危险状态
     */
    public boolean isCritical() {
        return "CRITICAL".equals(status);
    }
}
