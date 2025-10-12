package com.cloud.common.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 线程池优雅关闭管理器
 * 应用关闭时优雅关闭所有线程池
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Component
public class ThreadPoolShutdownManager implements ApplicationListener<ContextClosedEvent> {

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("开始优雅关闭线程池...");

        Map<String, ThreadPoolTaskExecutor> executors =
            applicationContext.getBeansOfType(ThreadPoolTaskExecutor.class);

        if (executors.isEmpty()) {
            log.info("未发现需要关闭的线程池");
            return;
        }

        log.info("发现 {} 个线程池需要关闭", executors.size());

        // 并行关闭所有线程池
        executors.values().parallelStream().forEach(this::shutdownThreadPool);

        log.info("所有线程池优雅关闭完成");
    }

    /**
     * 优雅关闭单个线程池
     */
    private void shutdownThreadPool(ThreadPoolTaskExecutor executor) {
        String threadNamePrefix = executor.getThreadNamePrefix();
        log.info("开始关闭线程池: {}", threadNamePrefix);

        try {
            // 获取底层的ThreadPoolExecutor
            java.util.concurrent.ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();

            // 停止接受新任务
            threadPoolExecutor.shutdown();

            // 等待现有任务完成
            if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("线程池 {} 未能在60秒内完成所有任务，强制关闭", threadNamePrefix);

                // 强制关闭
                threadPoolExecutor.shutdownNow();

                // 再次等待
                if (!threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("线程池 {} 强制关闭失败", threadNamePrefix);
                } else {
                    log.info("线程池 {} 已强制关闭", threadNamePrefix);
                }
            } else {
                log.info("线程池 {} 已优雅关闭", threadNamePrefix);
            }

        } catch (InterruptedException e) {
            log.error("线程池 {} 关闭过程中被中断", threadNamePrefix, e);
            executor.getThreadPoolExecutor().shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}