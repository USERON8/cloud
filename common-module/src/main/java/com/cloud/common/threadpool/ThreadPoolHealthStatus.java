package com.cloud.common.threadpool;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 线程池健康状态类
 * 封装线程池整体健康状况和问题详情
 *
 * @author what's up
 * @since 1.0.0
 */
@Data
public class ThreadPoolHealthStatus {
    
    /**
     * 检查时间
     */
    private LocalDateTime checkTime = LocalDateTime.now();
    
    /**
     * 整体健康状态：HEALTHY, WARNING, CRITICAL
     */
    private String overallStatus = "HEALTHY";
    
    /**
     * 线程池总数
     */
    private int totalPools = 0;
    
    /**
     * 健康的线程池数量
     */
    private int healthyPools = 0;
    
    /**
     * 警告状态的线程池数量
     */
    private int warningPools = 0;
    
    /**
     * 危险状态的线程池数量
     */
    private int criticalPools = 0;
    
    /**
     * 警告信息列表
     */
    private List<HealthIssue> warnings = new ArrayList<>();
    
    /**
     * 危险信息列表
     */
    private List<HealthIssue> criticals = new ArrayList<>();
    
    /**
     * 添加警告信息
     */
    public void addWarning(String threadPoolName, String message) {
        warnings.add(new HealthIssue(threadPoolName, message, "WARNING"));
    }
    
    /**
     * 添加危险信息
     */
    public void addCritical(String threadPoolName, String message) {
        criticals.add(new HealthIssue(threadPoolName, message, "CRITICAL"));
    }
    
    /**
     * 获取健康率（百分比）
     */
    public double getHealthRate() {
        if (totalPools == 0) {
            return 100.0;
        }
        return (double) healthyPools / totalPools * 100;
    }
    
    /**
     * 判断整体是否健康
     */
    public boolean isOverallHealthy() {
        return "HEALTHY".equals(overallStatus);
    }
    
    /**
     * 判断是否有警告
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * 判断是否有危险状况
     */
    public boolean hasCriticals() {
        return !criticals.isEmpty();
    }
    
    /**
     * 获取所有问题数量
     */
    public int getTotalIssues() {
        return warnings.size() + criticals.size();
    }
    
    /**
     * 健康问题内部类
     */
    @Data
    public static class HealthIssue {
        /**
         * 线程池名称
         */
        private String threadPoolName;
        
        /**
         * 问题描述
         */
        private String message;
        
        /**
         * 问题级别：WARNING, CRITICAL
         */
        private String level;
        
        /**
         * 发现时间
         */
        private LocalDateTime discoveredTime = LocalDateTime.now();
        
        public HealthIssue(String threadPoolName, String message, String level) {
            this.threadPoolName = threadPoolName;
            this.message = message;
            this.level = level;
        }
    }
}
