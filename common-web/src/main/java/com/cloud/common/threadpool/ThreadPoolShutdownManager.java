package com.cloud.common.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ThreadPoolShutdownManager implements ApplicationListener<ContextClosedEvent> {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        Map<String, ThreadPoolTaskExecutor> executors =
                applicationContext.getBeansOfType(ThreadPoolTaskExecutor.class);

        if (executors.isEmpty()) {
            return;
        }

        executors.values().parallelStream().forEach(this::shutdownThreadPool);
    }

    private void shutdownThreadPool(ThreadPoolTaskExecutor executor) {
        String threadNamePrefix = executor.getThreadNamePrefix();

        try {
            ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
            threadPoolExecutor.shutdown();

            if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Thread pool {} did not terminate in 60 seconds, forcing shutdown", threadNamePrefix);
                threadPoolExecutor.shutdownNow();

                if (!threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("Thread pool {} failed to terminate after force shutdown", threadNamePrefix);
                }
            }
        } catch (InterruptedException e) {
            log.error("Thread pool {} shutdown interrupted", threadNamePrefix, e);
            executor.getThreadPoolExecutor().shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Thread pool {} shutdown failed", threadNamePrefix, e);
        }
    }
}
