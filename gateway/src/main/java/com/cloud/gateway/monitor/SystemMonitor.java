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

/**
 * 系统监控组件
 * 监控网关服务的系统资源使用情况和线程池状态
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
@Slf4j
@Component
public class SystemMonitor {

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

    /**
     * 定时监控系统状态
     * 每10分钟执行一次
     */
    @DistributedLock(
            key = "'gateway:monitor:system'",
            waitTime = 0,
            leaseTime = 540,
            failStrategy = DistributedLock.LockFailStrategy.RETURN_NULL
    )
    @Scheduled(fixedRate = 600000) // 10分钟
    public void monitorSystemStatus() {
        log.info("🔍 网关系统监控报告:");

        // 监控系统资源
        monitorSystemResources();

        // 监控线程池状态
        monitorThreadPools();

        log.info("🔍 网关系统监控报告结束");
    }

    /**
     * 监控系统资源
     */
    private void monitorSystemResources() {
        try {
            // 获取系统信息
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

            // CPU使用率 (使用系统负载平均值作为替代)
            double cpuUsage = osBean.getSystemLoadAverage() > 0 ?
                    Math.min(osBean.getSystemLoadAverage() * 100 / osBean.getAvailableProcessors(), 100.0) : 0.0;

            // 内存使用情况
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = (double) usedMemory / maxMemory * 100;

            // 运行时间
            long uptime = runtimeBean.getUptime();
            long uptimeMinutes = uptime / (1000 * 60);

            log.info("  📊 系统资源 - CPU: {:.1f}%, 内存: {:.1f}% ({}/{}MB), 运行时间: {}分钟",
                    cpuUsage, memoryUsage,
                    usedMemory / (1024 * 1024), maxMemory / (1024 * 1024),
                    uptimeMinutes);

            // 资源告警检查
            checkResourceAlerts(cpuUsage, memoryUsage);

        } catch (Exception e) {
            log.warn("获取系统资源信息失败: {}", e.getMessage());
        }
    }

    /**
     * 监控线程池状态
     */
    private void monitorThreadPools() {
        log.info("  🧵 网关线程池状态:");

        // 监控网关监控线程池
        monitorThreadPool("监控", gatewayMonitorExecutor);

        // 监控网关日志线程池
        monitorThreadPool("日志", gatewayLogExecutor);

        // 监控网关统计线程池
        monitorThreadPool("统计", gatewayStatisticsExecutor);
    }

    /**
     * 监控单个线程池
     */
    private void monitorThreadPool(String poolName, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor threadPool = executor.getThreadPoolExecutor();

        int corePoolSize = threadPool.getCorePoolSize();
        int maximumPoolSize = threadPool.getMaximumPoolSize();
        int activeCount = threadPool.getActiveCount();
        long completedTaskCount = threadPool.getCompletedTaskCount();
        long taskCount = threadPool.getTaskCount();
        int queueSize = threadPool.getQueue().size();
        int queueCapacity = executor.getQueueCapacity();

        // 计算使用率
        double activeRate = (double) activeCount / maximumPoolSize * 100;
        double queueRate = (double) queueSize / queueCapacity * 100;
        double completionRate = taskCount > 0 ? (double) completedTaskCount / taskCount * 100 : 0;

        log.info("    🔹 {} - 活跃: {}/{} ({:.1f}%), 队列: {}/{} ({:.1f}%), 完成率: {:.1f}%",
                poolName, activeCount, maximumPoolSize, activeRate,
                queueSize, queueCapacity, queueRate, completionRate);

        // 线程池告警检查
        checkThreadPoolAlerts(poolName, activeRate, queueRate);
    }

    /**
     * 检查资源告警
     */
    private void checkResourceAlerts(double cpuUsage, double memoryUsage) {
        // CPU使用率告警
        if (cpuUsage > 80) {
            log.warn("⚠️ 网关CPU使用率过高: {:.1f}%", cpuUsage);
        }

        // 内存使用率告警
        if (memoryUsage > 85) {
            log.warn("⚠️ 网关内存使用率过高: {:.1f}%", memoryUsage);
        }

        // 严重告警
        if (cpuUsage > 95 || memoryUsage > 95) {
            log.error("❌ 网关系统资源严重不足! CPU: {:.1f}%, 内存: {:.1f}%", cpuUsage, memoryUsage);
        }
    }

    /**
     * 检查线程池告警
     */
    private void checkThreadPoolAlerts(String poolName, double activeRate, double queueRate) {
        // 活跃线程使用率告警
        if (activeRate > 80) {
            log.warn("⚠️ 网关{} 线程池活跃线程使用率过高: {:.1f}%", poolName, activeRate);
        }

        // 队列使用率告警
        if (queueRate > 70) {
            log.warn("⚠️ 网关{} 线程池队列使用率过高: {:.1f}%", poolName, queueRate);
        }

        // 队列满告警
        if (queueRate >= 100) {
            log.error("❌ 网关{} 线程池队列已满，可能出现任务拒绝!", poolName);
        }
    }

    /**
     * 获取系统状态摘要
     */
    public String getSystemStatusSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("网关系统状态摘要:\n");

        try {
            // 系统资源信息
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            double cpuUsage = osBean.getSystemLoadAverage() > 0 ?
                    Math.min(osBean.getSystemLoadAverage() * 100 / osBean.getAvailableProcessors(), 100.0) : 0.0;
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = (double) usedMemory / maxMemory * 100;

            summary.append(String.format("  系统资源: CPU %.1f%%, 内存 %.1f%%\n", cpuUsage, memoryUsage));

            // 线程池状态
            ThreadPoolExecutor monitorPool = gatewayMonitorExecutor.getThreadPoolExecutor();
            ThreadPoolExecutor logPool = gatewayLogExecutor.getThreadPoolExecutor();
            ThreadPoolExecutor statsPool = gatewayStatisticsExecutor.getThreadPoolExecutor();

            summary.append(String.format("  监控线程池: 活跃 %d/%d, 队列 %d/%d\n",
                    monitorPool.getActiveCount(), monitorPool.getMaximumPoolSize(),
                    monitorPool.getQueue().size(), gatewayMonitorExecutor.getQueueCapacity()));

            summary.append(String.format("  日志线程池: 活跃 %d/%d, 队列 %d/%d\n",
                    logPool.getActiveCount(), logPool.getMaximumPoolSize(),
                    logPool.getQueue().size(), gatewayLogExecutor.getQueueCapacity()));

            summary.append(String.format("  统计线程池: 活跃 %d/%d, 队列 %d/%d\n",
                    statsPool.getActiveCount(), statsPool.getMaximumPoolSize(),
                    statsPool.getQueue().size(), gatewayStatisticsExecutor.getQueueCapacity()));

        } catch (Exception e) {
            summary.append("  获取系统状态失败: ").append(e.getMessage());
        }

        return summary.toString();
    }

    /**
     * 检查是否有告警
     */
    public boolean hasAlerts() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            double cpuUsage = osBean.getSystemLoadAverage() > 0 ?
                    Math.min(osBean.getSystemLoadAverage() * 100 / osBean.getAvailableProcessors(), 100.0) : 0.0;
            double memoryUsage = (double) memoryBean.getHeapMemoryUsage().getUsed() /
                    memoryBean.getHeapMemoryUsage().getMax() * 100;

            // 检查系统资源告警
            if (cpuUsage > 80 || memoryUsage > 85) {
                return true;
            }

            // 检查线程池告警
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
                ThreadPoolTaskExecutor executor = executors[i];

                double activeRate = (double) pool.getActiveCount() / pool.getMaximumPoolSize() * 100;
                double queueRate = (double) pool.getQueue().size() / executor.getQueueCapacity() * 100;

                if (activeRate > 80 || queueRate > 70) {
                    return true;
                }
            }

        } catch (Exception e) {
            log.warn("检查告警状态失败: {}", e.getMessage());
            return false;
        }

        return false;
    }
}
