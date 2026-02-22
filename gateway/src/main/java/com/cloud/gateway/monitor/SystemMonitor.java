package com.cloud.gateway.monitor;

import com.cloud.common.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
public class SystemMonitor {

    private static final double RESOURCE_WARNING_THRESHOLD = 80.0;
    private static final double RESOURCE_CRITICAL_THRESHOLD = 95.0;
    private static final double THREAD_POOL_ACTIVE_WARNING_THRESHOLD = 80.0;
    private static final double THREAD_POOL_QUEUE_WARNING_THRESHOLD = 70.0;
    private static final double THREAD_POOL_QUEUE_CRITICAL_THRESHOLD = 95.0;

    private final ThreadPoolTaskExecutor gatewayMonitorExecutor;
    private final ThreadPoolTaskExecutor gatewayLogExecutor;
    private final ThreadPoolTaskExecutor gatewayStatisticsExecutor;

    public SystemMonitor(
            @Qualifier("gatewayMonitorExecutor") Executor gatewayMonitorExecutor,
            @Qualifier("gatewayLogExecutor") Executor gatewayLogExecutor,
            @Qualifier("gatewayStatisticsExecutor") Executor gatewayStatisticsExecutor) {
        this.gatewayMonitorExecutor = (ThreadPoolTaskExecutor) gatewayMonitorExecutor;
        this.gatewayLogExecutor = (ThreadPoolTaskExecutor) gatewayLogExecutor;
        this.gatewayStatisticsExecutor = (ThreadPoolTaskExecutor) gatewayStatisticsExecutor;
    }

    @DistributedLock(
            key = "'gateway:monitor:system'",
            waitTime = 0,
            leaseTime = 540,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    @Scheduled(fixedRate = 600000)
    public void monitorSystemStatus() {
        monitorSystemResources();
        monitorThreadPools();
    }

    private void monitorSystemResources() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

            double cpuUsage = osBean.getSystemLoadAverage() > 0
                    ? Math.min(osBean.getSystemLoadAverage() * 100 / osBean.getAvailableProcessors(), 100.0)
                    : 0.0;

            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0.0;

            long uptimeMinutes = runtimeBean.getUptime() / (1000 * 60);

            log.debug("Gateway system resources - cpu: {:.1f}%, memory: {:.1f}%, uptime: {} min",
                    cpuUsage, memoryUsage, uptimeMinutes);

            checkResourceAlerts(cpuUsage, memoryUsage);
        } catch (Exception e) {
            log.warn("Failed to monitor system resources: {}", e.getMessage());
        }
    }

    private void monitorThreadPools() {
        monitorThreadPool("gatewayMonitorExecutor", gatewayMonitorExecutor);
        monitorThreadPool("gatewayLogExecutor", gatewayLogExecutor);
        monitorThreadPool("gatewayStatisticsExecutor", gatewayStatisticsExecutor);
    }

    private void monitorThreadPool(String poolName, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor threadPool = executor.getThreadPoolExecutor();
        if (threadPool == null) {
            return;
        }

        int corePoolSize = threadPool.getCorePoolSize();
        int maximumPoolSize = threadPool.getMaximumPoolSize();
        int activeCount = threadPool.getActiveCount();
        long completedTaskCount = threadPool.getCompletedTaskCount();
        long taskCount = threadPool.getTaskCount();
        int queueSize = threadPool.getQueue().size();
        int queueCapacity = Math.max(executor.getQueueCapacity(), 1);

        double activeRate = maximumPoolSize > 0 ? (double) activeCount / maximumPoolSize * 100 : 0.0;
        double queueRate = (double) queueSize / queueCapacity * 100;
        double completionRate = taskCount > 0 ? (double) completedTaskCount / taskCount * 100 : 0.0;

        log.debug(
                "Thread pool {} - core: {}, max: {}, active: {}, queue: {}/{}, completed: {}, total: {}, activeRate: {:.1f}%, queueRate: {:.1f}%, completionRate: {:.1f}%",
                poolName, corePoolSize, maximumPoolSize, activeCount, queueSize, queueCapacity,
                completedTaskCount, taskCount, activeRate, queueRate, completionRate
        );

        checkThreadPoolAlerts(poolName, activeRate, queueRate);
    }

    private void checkResourceAlerts(double cpuUsage, double memoryUsage) {
        if (cpuUsage > RESOURCE_WARNING_THRESHOLD) {
            log.warn("Gateway CPU usage is high: {:.1f}%", cpuUsage);
        }

        if (memoryUsage > RESOURCE_WARNING_THRESHOLD) {
            log.warn("Gateway memory usage is high: {:.1f}%", memoryUsage);
        }

        if (cpuUsage > RESOURCE_CRITICAL_THRESHOLD || memoryUsage > RESOURCE_CRITICAL_THRESHOLD) {
            log.error("Gateway resources are critical. CPU: {:.1f}%, memory: {:.1f}%", cpuUsage, memoryUsage);
        }
    }

    private void checkThreadPoolAlerts(String poolName, double activeRate, double queueRate) {
        if (activeRate > THREAD_POOL_ACTIVE_WARNING_THRESHOLD) {
            log.warn("Thread pool {} active usage is high: {:.1f}%", poolName, activeRate);
        }

        if (queueRate > THREAD_POOL_QUEUE_WARNING_THRESHOLD) {
            log.warn("Thread pool {} queue usage is high: {:.1f}%", poolName, queueRate);
        }

        if (queueRate > THREAD_POOL_QUEUE_CRITICAL_THRESHOLD) {
            log.error("Thread pool {} queue is nearly full", poolName);
        }
    }

    public String getSystemStatusSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Gateway system status summary").append('\n');

        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            double cpuUsage = osBean.getSystemLoadAverage() > 0
                    ? Math.min(osBean.getSystemLoadAverage() * 100 / osBean.getAvailableProcessors(), 100.0)
                    : 0.0;
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0.0;

            summary.append(String.format("  System resources: CPU %.1f%%, memory %.1f%%%n", cpuUsage, memoryUsage));

            ThreadPoolExecutor monitorPool = gatewayMonitorExecutor.getThreadPoolExecutor();
            ThreadPoolExecutor logPool = gatewayLogExecutor.getThreadPoolExecutor();
            ThreadPoolExecutor statsPool = gatewayStatisticsExecutor.getThreadPoolExecutor();

            appendPoolSummary(summary, "monitor", monitorPool, gatewayMonitorExecutor);
            appendPoolSummary(summary, "log", logPool, gatewayLogExecutor);
            appendPoolSummary(summary, "statistics", statsPool, gatewayStatisticsExecutor);
        } catch (Exception e) {
            summary.append("  Failed to read system status: ").append(e.getMessage());
        }

        return summary.toString();
    }

    public boolean hasAlerts() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            double cpuUsage = osBean.getSystemLoadAverage() > 0
                    ? Math.min(osBean.getSystemLoadAverage() * 100 / osBean.getAvailableProcessors(), 100.0)
                    : 0.0;
            double memoryUsage = (double) memoryBean.getHeapMemoryUsage().getUsed()
                    / Math.max(memoryBean.getHeapMemoryUsage().getMax(), 1) * 100;

            if (cpuUsage > RESOURCE_WARNING_THRESHOLD || memoryUsage > RESOURCE_WARNING_THRESHOLD) {
                return true;
            }

            ThreadPoolExecutor[] pools = {
                    gatewayMonitorExecutor.getThreadPoolExecutor(),
                    gatewayLogExecutor.getThreadPoolExecutor(),
                    gatewayStatisticsExecutor.getThreadPoolExecutor()
            };
            ThreadPoolTaskExecutor[] executors = {
                    gatewayMonitorExecutor,
                    gatewayLogExecutor,
                    gatewayStatisticsExecutor
            };

            for (int i = 0; i < pools.length; i++) {
                ThreadPoolExecutor pool = pools[i];
                if (pool == null) {
                    continue;
                }
                ThreadPoolTaskExecutor executor = executors[i];
                double activeRate = pool.getMaximumPoolSize() > 0
                        ? (double) pool.getActiveCount() / pool.getMaximumPoolSize() * 100
                        : 0.0;
                double queueRate = (double) pool.getQueue().size() / Math.max(executor.getQueueCapacity(), 1) * 100;

                if (activeRate > THREAD_POOL_ACTIVE_WARNING_THRESHOLD || queueRate > THREAD_POOL_QUEUE_WARNING_THRESHOLD) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to evaluate alert status: {}", e.getMessage());
            return false;
        }

        return false;
    }

    private void appendPoolSummary(
            StringBuilder summary,
            String poolName,
            ThreadPoolExecutor pool,
            ThreadPoolTaskExecutor executor) {
        if (pool == null) {
            summary.append(String.format("  %s pool: unavailable%n", poolName));
            return;
        }

        summary.append(String.format(
                "  %s pool: active %d/%d, queue %d/%d%n",
                poolName,
                pool.getActiveCount(),
                pool.getMaximumPoolSize(),
                pool.getQueue().size(),
                executor.getQueueCapacity()
        ));
    }
}
