package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;







@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class RocketMQConfig {

    


    @Bean
    public RocketMQHealthIndicator rocketMQHealthIndicator() {
        return new RocketMQHealthIndicator();
    }

    


    public static class RocketMQHealthIndicator {

        public RocketMQHealthIndicator() {
        }

        public boolean isHealthy() {
            
            return true;
        }
    }
}
