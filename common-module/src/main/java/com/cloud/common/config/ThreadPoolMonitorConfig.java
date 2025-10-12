package com.cloud.common.config;

import com.cloud.common.threadpool.EnhancedThreadPoolMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 线程池监控配置类
 * 配置线程池监控相关的Bean
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.async.common.monitoring-enabled", havingValue = "true", matchIfMissing = true)
public class ThreadPoolMonitorConfig {

    /**
     * 配置线程池监控
     */
    @Bean
    public EnhancedThreadPoolMonitor enhancedThreadPoolMonitor() {
        log.info("✅ 线程池监控组件初始化完成");
        return new EnhancedThreadPoolMonitor();
    }
}