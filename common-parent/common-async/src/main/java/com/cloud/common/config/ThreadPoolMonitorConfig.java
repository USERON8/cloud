package com.cloud.common.config;

import com.cloud.common.threadpool.EnhancedThreadPoolMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;








@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.async.common.monitoring-enabled", havingValue = "true", matchIfMissing = true)
public class ThreadPoolMonitorConfig {

    


    @Bean
    public EnhancedThreadPoolMonitor enhancedThreadPoolMonitor() {
        
        return new EnhancedThreadPoolMonitor();
    }
}
