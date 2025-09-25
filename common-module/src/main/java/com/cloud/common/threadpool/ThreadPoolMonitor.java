package com.cloud.common.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控工具类
 * 提供线程池状态监控、性能统计和健康检查功能
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Component
public class ThreadPoolMonitor {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 获取所有线程池的状态信息
     *
     * @return 线程池状态信息Map
     */
    public Map<String, ThreadPoolInfo> getAllThreadPoolInfo() {
        Map<String, ThreadPoolInfo> threadPoolInfoMap = new HashMap<>();
        
        try {
            Map<String, ThreadPoolTaskExecutor> threadPoolBeans = 
                    applicationContext.getBeansOfType(ThreadPoolTaskExecutor.class);
            
            for (Map.Entry<String, ThreadPoolTaskExecutor> entry : threadPoolBeans.entrySet()) {
                String beanName = entry.getKey();
                ThreadPoolTaskExecutor executor = entry.getValue();
                ThreadPoolInfo info = buildThreadPoolInfo(beanName, executor);
                threadPoolInfoMap.put(beanName, info);
            }
        } catch (Exception e) {
            log.error("获取线程池信息失败", e);
        }
        
        return threadPoolInfoMap;
    }

    /**
     * 获取指定线程池的状态信息
     *
     * @param beanName 线程池Bean名称
     * @return 线程池状态信息
     */
    public ThreadPoolInfo getThreadPoolInfo(String beanName) {
        try {
            ThreadPoolTaskExecutor executor = applicationContext.getBean(beanName, ThreadPoolTaskExecutor.class);
            return buildThreadPoolInfo(beanName, executor);
        } catch (Exception e) {
            log.error("获取线程池{}信息失败", beanName, e);
            return null;
        }
    }

    /**
     * 检查线程池健康状态
     *
     * @return 健康检查结果
     */
    public ThreadPoolHealthStatus checkThreadPoolHealth() {
        Map<String, ThreadPoolInfo> allThreadPools = getAllThreadPoolInfo();
        ThreadPoolHealthStatus healthStatus = new ThreadPoolHealthStatus();
        
        int totalPools = allThreadPools.size();
        int healthyPools = 0;
        int warningPools = 0;
        int criticalPools = 0;
        
        for (Map.Entry<String, ThreadPoolInfo> entry : allThreadPools.entrySet()) {
            ThreadPoolInfo info = entry.getValue();
            String status = evaluateThreadPoolStatus(info);
            
            switch (status) {
                case "HEALTHY":
                    healthyPools++;
                    break;
                case "WARNING":
                    warningPools++;
                    healthStatus.addWarning(entry.getKey(), "线程池使用率较高");
                    break;
                case "CRITICAL":
                    criticalPools++;
                    healthStatus.addCritical(entry.getKey(), "线程池接近满载");
                    break;
            }
        }
        
        healthStatus.setTotalPools(totalPools);
        healthStatus.setHealthyPools(healthyPools);
        healthStatus.setWarningPools(warningPools);
        healthStatus.setCriticalPools(criticalPools);
        
        // 设置整体健康状态
        if (criticalPools > 0) {
            healthStatus.setOverallStatus("CRITICAL");
        } else if (warningPools > 0) {
            healthStatus.setOverallStatus("WARNING");
        } else {
            healthStatus.setOverallStatus("HEALTHY");
        }
        
        return healthStatus;
    }

    /**
     * 构建线程池信息对象
     *
     * @param beanName 线程池Bean名称
     * @param executor 线程池执行器
     * @return 线程池信息
     */
    private ThreadPoolInfo buildThreadPoolInfo(String beanName, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        
        ThreadPoolInfo info = new ThreadPoolInfo();
        info.setBeanName(beanName);
        info.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
        info.setMaximumPoolSize(threadPoolExecutor.getMaximumPoolSize());
        info.setCurrentPoolSize(threadPoolExecutor.getPoolSize());
        info.setActiveThreadCount(threadPoolExecutor.getActiveCount());
        info.setQueueSize(threadPoolExecutor.getQueue().size());
        info.setQueueCapacity(executor.getQueueCapacity());
        info.setCompletedTaskCount(threadPoolExecutor.getCompletedTaskCount());
        info.setTotalTaskCount(threadPoolExecutor.getTaskCount());
        info.setKeepAliveTime(threadPoolExecutor.getKeepAliveTime(java.util.concurrent.TimeUnit.SECONDS));
        info.setRejectedExecutionHandler(threadPoolExecutor.getRejectedExecutionHandler().getClass().getSimpleName());
        
        // 计算使用率
        double poolUsageRate = (double) threadPoolExecutor.getActiveCount() / threadPoolExecutor.getMaximumPoolSize() * 100;
        double queueUsageRate = executor.getQueueCapacity() > 0 ? 
                (double) threadPoolExecutor.getQueue().size() / executor.getQueueCapacity() * 100 : 0;
        
        info.setPoolUsageRate(poolUsageRate);
        info.setQueueUsageRate(queueUsageRate);
        
        // 设置状态
        info.setStatus(evaluateThreadPoolStatus(info));
        
        return info;
    }

    /**
     * 评估线程池状态
     *
     * @param info 线程池信息
     * @return 状态字符串
     */
    private String evaluateThreadPoolStatus(ThreadPoolInfo info) {
        // 线程池使用率超过90%或队列使用率超过90%为危险状态
        if (info.getPoolUsageRate() > 90 || info.getQueueUsageRate() > 90) {
            return "CRITICAL";
        }
        // 线程池使用率超过70%或队列使用率超过70%为警告状态
        else if (info.getPoolUsageRate() > 70 || info.getQueueUsageRate() > 70) {
            return "WARNING";
        }
        // 其他情况为健康状态
        else {
            return "HEALTHY";
        }
    }

    /**
     * 记录线程池状态日志
     */
    public void logThreadPoolStatus() {
        Map<String, ThreadPoolInfo> allThreadPools = getAllThreadPoolInfo();
        
        log.info("========== 线程池状态监控 ==========");
        for (Map.Entry<String, ThreadPoolInfo> entry : allThreadPools.entrySet()) {
            ThreadPoolInfo info = entry.getValue();
            log.info("线程池: {} | 状态: {} | 活跃线程: {}/{} | 队列: {}/{} | 完成任务: {}",
                    info.getBeanName(),
                    info.getStatus(),
                    info.getActiveThreadCount(),
                    info.getMaximumPoolSize(),
                    info.getQueueSize(),
                    info.getQueueCapacity(),
                    info.getCompletedTaskCount());
        }
        log.info("=====================================");
    }
}
