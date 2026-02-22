package com.cloud.common.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;








@Slf4j
@Component
public class ThreadPoolMonitor {

    @Autowired
    private ApplicationContext applicationContext;

    




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
            log.error("Failed to collect thread pool information", e);
        }

        return threadPoolInfoMap;
    }

    





    public ThreadPoolInfo getThreadPoolInfo(String beanName) {
        try {
            ThreadPoolTaskExecutor executor = applicationContext.getBean(beanName, ThreadPoolTaskExecutor.class);
            return buildThreadPoolInfo(beanName, executor);
        } catch (Exception e) {
            log.error("Failed to get thread pool information: {}", beanName, e);
            return null;
        }
    }

    




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
                    healthStatus.addWarning(entry.getKey(), "Thread pool usage is high");
                    break;
                case "CRITICAL":
                    criticalPools++;
                    healthStatus.addCritical(entry.getKey(), "Thread pool is near saturation");
                    break;
            }
        }

        healthStatus.setTotalPools(totalPools);
        healthStatus.setHealthyPools(healthyPools);
        healthStatus.setWarningPools(warningPools);
        healthStatus.setCriticalPools(criticalPools);

        
        if (criticalPools > 0) {
            healthStatus.setOverallStatus("CRITICAL");
        } else if (warningPools > 0) {
            healthStatus.setOverallStatus("WARNING");
        } else {
            healthStatus.setOverallStatus("HEALTHY");
        }

        return healthStatus;
    }

    






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

        
        double poolUsageRate = (double) threadPoolExecutor.getActiveCount() / threadPoolExecutor.getMaximumPoolSize() * 100;
        double queueUsageRate = executor.getQueueCapacity() > 0 ?
                (double) threadPoolExecutor.getQueue().size() / executor.getQueueCapacity() * 100 : 0;

        info.setPoolUsageRate(poolUsageRate);
        info.setQueueUsageRate(queueUsageRate);

        
        info.setStatus(evaluateThreadPoolStatus(info));

        
        info.setTimestamp(System.currentTimeMillis());

        return info;
    }

    





    private String evaluateThreadPoolStatus(ThreadPoolInfo info) {
        
        if (info.getPoolUsageRate() > 90 || info.getQueueUsageRate() > 90) {
            return "CRITICAL";
        }
        
        else if (info.getPoolUsageRate() > 70 || info.getQueueUsageRate() > 70) {
            return "WARNING";
        }
        
        else {
            return "HEALTHY";
        }
    }

    


    public void logThreadPoolStatus() {
        Map<String, ThreadPoolInfo> allThreadPools = getAllThreadPoolInfo();

        
        for (Map.Entry<String, ThreadPoolInfo> entry : allThreadPools.entrySet()) {
            ThreadPoolInfo info = entry.getValue();
            

        }
        
    }
}
