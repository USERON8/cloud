package com.cloud.user.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Actuator配置类
 */
@Configuration
public class ActuatorConfig {

    /**
     * 自定义健康检查指示器
     * 检查用户服务的健康状态
     */
    @Bean
    public HealthIndicator userServiceHealthIndicator() {
        return () -> {
            // 这里可以添加具体的健康检查逻辑
            // 例如检查数据库连接、缓存连接等
            return Health.up()
                    .withDetail("UserService", "User service is running")
                    .build();
        };
    }
}