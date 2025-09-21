package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ基础配置类
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class RocketMQConfig {

    /**
     * RocketMQ配置检查
     */
    @Bean
    public RocketMQHealthIndicator rocketMQHealthIndicator() {
        return new RocketMQHealthIndicator();
    }

    /**
     * RocketMQ健康检查指示器
     */
    public static class RocketMQHealthIndicator {

        public RocketMQHealthIndicator() {
            log.info("✅ RocketMQ配置已加载");
        }

        public boolean isHealthy() {
            // 简单的健康检查，可以扩展为实际的连接测试
            return true;
        }
    }
}
