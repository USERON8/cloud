package com.cloud.common.config.actuator;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring Boot Actuator配置类
 * <p>
 * 提供统一的健康检查、指标监控配置
 * 适用于Cloud微服务平台的所有服务
 * <p>
 * 主要功能：
 * - 健康检查端点配置
 * - 自定义健康指示器
 * - 服务指标监控配置
 * - 统一的服务信息展示
 *
 * @author CloudDevAgent
 * @version 1.0
 * @since 2025-09-27
 */
@Configuration
public class ActuatorConfig {

    /**
     * 自定义健康指示器
     * 检查服务基本运行状态
     *
     * @param environment 环境配置
     * @return 健康指示器
     */
    @Bean
    public HealthIndicator cloudServiceHealthIndicator(Environment environment) {
        return new HealthIndicator() {
            @Override
            public Health health() {
                try {
                    // 获取服务基本信息
                    String serviceName = environment.getProperty("spring.application.name", "unknown-service");
                    String profile = String.join(",", environment.getActiveProfiles());
                    String serverPort = environment.getProperty("server.port", "unknown");

                    // 简单的健康检查逻辑
                    boolean isHealthy = checkServiceHealth();

                    Health.Builder builder = isHealthy ? Health.up() : Health.down();

                    return builder
                            .withDetail("service", serviceName)
                            .withDetail("profile", profile)
                            .withDetail("port", serverPort)
                            .withDetail("status", isHealthy ? "运行正常" : "运行异常")
                            .withDetail("timestamp", System.currentTimeMillis())
                            .build();

                } catch (Exception e) {
                    return Health.down()
                            .withDetail("error", e.getMessage())
                            .withDetail("timestamp", System.currentTimeMillis())
                            .build();
                }
            }

            /**
             * 检查服务健康状态
             * 可扩展为具体的健康检查逻辑
             *
             * @return 健康状态
             */
            private boolean checkServiceHealth() {
                // 基础健康检查：JVM内存使用率
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory;

                // 如果内存使用率超过90%，认为服务不健康
                return memoryUsage < 0.9;
            }
        };
    }

    /**
     * 自定义指标注册器
     * 为服务添加统一的标签信息
     *
     * @param environment 环境配置
     * @return 指标注册器自定义配置
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
        return registry -> {
            String serviceName = environment.getProperty("spring.application.name", "unknown-service");
            String profile = String.join(",", environment.getActiveProfiles());

            registry.config().commonTags(
                    "service", serviceName,
                    "profile", profile,
                    "platform", "cloud-microservices"
            );
        };
    }
}
